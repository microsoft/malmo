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
import gym
import ray
from ray.rllib.env.atari_wrappers import FrameStack
from ray.tune import register_env, run_experiments
from pathlib import Path
import malmoenv
from malmoenv.turnbasedmultiagentenv import AgentConfig, TurnBasedRllibMultiAgentEnv, SyncRllibMultiAgentEnv

SINGLE_AGENT_ENV = "malmo_single_agent"
MULTI_AGENT_ENV = "malmo_multi_agent"
MISSION_XML = "missions/rllib_multiagent.xml"
COMMAND_PORT = 8999
NUM_ENVIRONMENT_INSTANCES = 2
FRAME_STACK = 1

xml = Path(MISSION_XML).read_text()

# An environment wrapper to shape rewards and determine episode terminality independently of Malmo
class TrackingEnv(gym.Wrapper):
    def __init__(self, env):
        super().__init__(env)
        self._actions = [
            self._forward,
            self._back,
            self._turn_right,
            self._turn_left,
            self._idle
        ]
        self._step_count = 0

    def _reset_state(self):
        self._facing = (1, 0)
        self._position = (0, 0)
        self._visited = {}
        self._update_visited()
        self._step_count = 0

    def _forward(self):
        self._position = (
            self._position[0] + self._facing[0],
            self._position[1] + self._facing[1]
        )

    def _back(self):
        self._position = (
            self._position[0] - self._facing[0],
            self._position[1] - self._facing[1]
        )

    def _turn_left(self):
        self._facing = (self._facing[1], -self._facing[0])

    def _turn_right(self):
        self._facing = (-self._facing[1], self._facing[0])

    def _idle(self):
        pass

    def _encode_state(self):
        return self._position

    def _update_visited(self):
        state = self._encode_state()
        value = self._visited.get(state, 0)
        self._visited[state] = value + 1
        return value

    def reset(self):
        self._reset_state()
        return super().reset()

    def step(self, action):
        o, r, d, i = super().step(action)
        self._actions[action]()
        revisit_count = self._update_visited()
        if revisit_count == 0:
            r += 0.02
        if action == 4:
            r += -0.5

        self._step_count += 1
        if self._step_count == 50:
            d = True
        elif r < -0.9:
            d = True

        return o, r, d, i


def env_factory(agent_id, xml, role, host_address, host_port, command_address, command_port):
    env = malmoenv.make()
    env.init(xml, host_port,
        server=host_address,
        server2=command_address,
        port2=command_port,
        role=role,
        exp_uid="multiagent",
        reshape=True
    )
    if FRAME_STACK > 1:
        env = FrameStack(env, FRAME_STACK)
    env = TrackingEnv(env)
    return env

def create_single_agent_env(config):
    port = COMMAND_PORT + config.worker_index
    return env_factory("agent0", xml, 0, "127.0.0.1", port, "127.0.0.1", port)

def create_multi_agent_env(config):
    port = COMMAND_PORT + (config.worker_index * 2)
    agent_config = [
        AgentConfig(id=f"agent1", address=port-1),
        AgentConfig(id=f"agent2", address=port),
    ]
    env = TurnBasedRllibMultiAgentEnv(xml, agent_config,
        env_factory=env_factory,
    )
    env = SyncRllibMultiAgentEnv(env, idle_action=4)
    return env


register_env(SINGLE_AGENT_ENV, create_single_agent_env)
register_env(MULTI_AGENT_ENV, create_multi_agent_env)

run_experiments({
    "malmo": {
        "run": "IMPALA",
        "env": MULTI_AGENT_ENV,
        "config": {
            "model": {
                "dim": 42
            },
            "num_workers": NUM_ENVIRONMENT_INSTANCES,
            "num_gpus": 0,
            "rollout_fragment_length": 50,
            "train_batch_size": 1024,
            "replay_buffer_num_slots": 4000,
            "replay_proportion": 10,
            "learner_queue_timeout": 900,
            "num_sgd_iter": 2,
            "num_data_loader_buffers": 1,

            "exploration_config": {
                "type": "EpsilonGreedy",
                "initial_epsilon": 1.0,
                "final_epsilon": 0.02,
                "epsilon_timesteps": 7000
            },

            "multiagent": {
                "policies": { "shared_policy": (
                    None,
                    gym.spaces.Box(0, 255, shape=(84, 84, 3 * FRAME_STACK)),
                    gym.spaces.Discrete(5),
                    {}
                )},
                "policy_mapping_fn": (lambda agent_id: "shared_policy")
            }
        }
    }
})