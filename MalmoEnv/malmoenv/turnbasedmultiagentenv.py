# ------------------------------------------------------------------------------------------------
# Copyright (c) 2020 Microsoft Corporation
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
# associated documentation files (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge, publish, distribute,
# sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or
# substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
# DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------------------------------------
import time
from threading import Thread
from lxml import etree
from ray.rllib.env.multi_agent_env import MultiAgentEnv
import malmoenv
from malmoenv.core import EnvException

STEP_DELAY_TIME = 0.15

def _validate_config(xml, agent_configs):
    """
    Verify that the supplied agent config is compatible with the mission XML.
    """
    assert len(agent_configs) >= 2
    xml = etree.fromstring(xml)
    xml_agent_count = len(xml.findall("{http://ProjectMalmo.microsoft.com}AgentSection"))
    assert len(agent_configs) == xml_agent_count


def _parse_address(address):
    """
    Take addresses of various forms and convert them to a tuple of the form (HOST, PORT).
    """

    if isinstance(address, int):
        # Only a port number provided
        return ("127.0.0.1", address)

    if isinstance(address, str):
        parts = address.split(":")
        if len(parts) == 1:
            # Port number as a string
            return ("127.0.0.1", int(parts[0]))
        if len(parts) == 2:
            # String in the form "HOST:PORT"
            return (parts[0], int(parts[1]))

    if len(address) == 2 and isinstance(address[0], str) and isinstance(address[1], int):
        # An already parsed address
        return address

    raise EnvException(f"{address} is not a valid address")

def _await_results(results):
    """
    Receives a dictionary of result tasks and repopulates it with the final results after the tasks
    complete.
    """
    for agent_id, task in results.items():
        results[agent_id] = task.wait()

def _default_env_factory(agent_id, xml, role, host_address, host_port, command_address, command_port):
    """
    Default environment factory that fills out just enough settings to connect multiple game
    instances into a single game session.
    agent_id - The agent we're constructing the environment connection for.
    xml - The mission XML.
    role - The agent's role number. 0 == host agent.
    host_address, host_port - Connection details for the game session host.
    command_address, command_port - Connection details for the game instance the agent is controlling.
    """
    env = malmoenv.make()
    env.init(xml, host_port,
        server=host_address,
        server2=command_address,
        port2=command_port,
        role=role,
        exp_uid="default_experiment_id"
    )
    return env

def _default_all_done_checker(env, obs, rewards, dones, infos):
    """
    Returns True if any agent is reported as done.
    """
    for done in dones.values():
        if done:
            return True
    return False

# Wraps a MalmoEnv instance and provides async reset and step operations
# Reset operations need to be executed async as none of the connected environments will complete
# their reset operations until all environments have at least issued a reset request.
class _ConnectionContext:
    def __init__(self, id, address, env):
        """
        Wrapper around a connection to a game instance.
        id - The agent id that is in control of the game instance.
        address - (server, port) tuple for the command connection.
        env - The MalmoEnv instance that is connected to the game instance.
        """
        self.id = id
        self.address = address
        self.env = env
        self.last_observation = None

        # Async task status tracking
        self._task_thread = None
        self._task_result = None

    def wait(self):
        """
        Wait for the current async task to complete and return the result.
        """
        assert self._task_thread is not None
        self._task_thread.join()
        self._task_thread = None

        # We want to re-trow the exception if the task raised an error
        if isinstance(self._task_result, Exception):
            raise self._task_result

        return self._task_result

    def reset(self):
        """
        Issue a reset request and return the async task immediately.
        """
        assert self._task_thread is None
        self._task_thread = Thread(target=self._reset_task, name=f"Agent '{self.id}' reset")
        self._task_thread.start()
        return self

    def _reset_task(self):
        try:
            self._task_result = self.last_observation = self.env.reset()
        except Exception as e:
            self._task_result = e

    def step(self, action):
        """
        Issue a step request and return the async task immediately.
        """
        self.last_observation, r, d, i = self.env.step(action)
        return self.last_observation, r, d, i

    def close(self):
        """
        Shut down the Minecraft instance.
        """
        self.env.close()

# Config for a single agent that will be present within the environment
class AgentConfig:
    def __init__(self, id, address):
        """
        Configuration details for an agent acting within the environment.
        id - The agent's id as used by RLlib.
        address - The address for the game instance for the agent to connect to.
        """
        self.id = id
        self.address = _parse_address(address)

# RLlib compatible multi-agent environment.
# This wraps multiple instances of MalmoEnv environments that are connected to their own Minecraft
# instances.
# The first agent defined in the agent_configs is treated as the primary Minecraft instance that
# will act as the game server.
class TurnBasedRllibMultiAgentEnv(MultiAgentEnv):
    def __init__(self, xml, agent_configs, env_factory=None, all_done_checker=None):
        """
        An RLlib compatible multi-agent environment.
        NOTE: Will not work with turn based actions as all agent act together.
        xml - The mission XML
        agent_configs - A list of AgentConfigs to decribe the agents within the environment.
        env_factory - Function to allow custom construction of the MalmoEnv instances.
                      This can be used to override the default inti parameter for the environment.
        all_done_checker - Function to check if the "__all__" key should be set in the step done
                           dictionary. The default check returns True if any agent reports that
                           they're done.
        """
        _validate_config(xml, agent_configs)

        self._all_done_checker = all_done_checker or _default_all_done_checker
        env_factory = env_factory or _default_env_factory

        # The first agent is treated as the game session host
        host_address = agent_configs[0].address
        self._id = host_address
        self._connections = {}
        self._reset_request_time = 0
        self._step = 0

        role = 0
        for agent_config in agent_configs:
            env = env_factory(
                agent_id=agent_config.id,
                xml=xml,
                role=role,
                host_address=host_address[0],
                host_port=host_address[1],
                command_address=agent_config.address[0],
                command_port=agent_config.address[1]
            )
            context = _ConnectionContext(
                agent_config.id,
                agent_config.address,
                env
            )
            self._connections[agent_config.id] = context
            role += 1


    def get_observation_space(self, agent_id):
        return self._connections[agent_id].env.observation_space

    def get_action_space(self, agent_id):
        return self._connections[agent_id].env.action_space

    def reset(self):
        print(f"Resetting {self._id}...")
        self._step = 0
        obs = {}
        request_time = time.perf_counter()
        for agent_id, connection in self._connections.items():
            obs[agent_id] = connection.reset()

        # All reset operations must be issued asynchronously as none of the Minecraft instances
        # will complete their reset requests until all agents have issued a reset request
        _await_results(obs)
        self._reset_request_time = time.perf_counter() - request_time
        print(f"Reset {self._id} complete")

        return obs

    def step(self, actions):
#        print(f"Step {self._step} for agent {self._id} - Actions: {actions}...")
        self._step += 1
        results = {}
        request_time = time.perf_counter()
        done = False

        for agent_id, action in actions.items():
            if not done:
                time.sleep(STEP_DELAY_TIME)
                o, r, done, i = self._connections[agent_id].step(action)
            else:
                o = self._connections[agent_id].last_observation
                r = 0.0
                i = {}

            assert self._connections[agent_id].env.observation_space.contains(o), f"Shape={o.shape}"
            results[agent_id] = (o, r, done, i)

        request_time = time.perf_counter() - request_time

        # We need to repack the individual step results into dictionaries per data type to conform
        # with RLlib's requirements
        obs = {
            agent_id: result[0]
            for agent_id, result in results.items()
        }
        rewards = {
            agent_id: result[1]
            for agent_id, result in results.items()
        }
        dones = {
            agent_id: result[2]
            for agent_id, result in results.items()
        }
        infos = {
            agent_id: result[3]
            for agent_id, result in results.items()
        }

        # Pass the results to the done checker to set the required __all__ value
        dones["__all__"] = self._all_done_checker(self, obs, rewards, dones, infos)
#        infos["step_request_time"] = request_time
#        infos["reset_request_time"] = self._reset_request_time

#        print(f"Step of {self._id} complete - {dones}")

        return obs, rewards, dones, infos

    def close(self):
        for connection in self._connections.values():
            try:
                connection.close()
            except Exception as e:
                message = getattr(e, "message", e)
                print(f"Error closing environment: {message}")


class SyncRllibMultiAgentEnv(MultiAgentEnv):
    def __init__(self, env, idle_action):
        self.env = env
        self.idle_action = idle_action

    def reset(self):
        return self.env.reset()

    def step(self, actions):
        o, r, d, i = self.env.step(actions)
        for done in d.values():
            if done:
                return o, r, d, i

        return self.env.step({
            key: self.idle_action
            for key in actions
        })

    def close(self):
        return self.env.close()
