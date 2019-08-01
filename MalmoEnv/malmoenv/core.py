# ------------------------------------------------------------------------------------------------
# Copyright (c) 2018 Microsoft Corporation
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

from lxml import etree
import struct
import socket
import time
import random
import numpy as np
from malmoenv import comms
from malmoenv.commands import CommandParser
import uuid
import gym.spaces
from malmoenv.comms import retry
from malmoenv.version import malmo_version


class StringActionSpace(gym.spaces.Discrete):
    """Malmo actions as their strings."""
    def __init__(self):
        gym.spaces.Discrete.__init__(self, 1)

    def __getitem__(self, action):
        return action


class ActionSpace(gym.spaces.Discrete):
    """Malmo actions as gym action space"""
    def __init__(self, actions):
        self.actions = actions
        gym.spaces.Discrete.__init__(self, len(self.actions))

    def sample(self):
        return random.randint(1, len(self.actions)) - 1

    def __getitem__(self, action):
        return self.actions[action]

    def __len__(self):
        return len(self.actions)


class VisualObservationSpace(gym.spaces.Box):
    """Space for visual observations: width x height x depth as a flat array.
    Where depth is 3 or 4 if encoding scene depth.
    """
    def __init__(self, width, height, depth):
        gym.spaces.Box.__init__(self,
                                low=np.iinfo(np.uint8).min, high=np.iinfo(np.uint8).max,
                                shape=(height, width, depth), dtype=np.uint8)


class EnvException(Exception):
    def __init__(self, message):
        super(EnvException, self).__init__(message)


class MissionInitException(Exception):
    def __init__(self, message):
        super(MissionInitException, self).__init__(message)


MAX_WAIT = 60 * 3


class Env:
    """Malmo "Env" open ai gym compatible environment API"""
    def __init__(self, reshape=False):
        self.action_space = None
        self.observation_space = None
        self.xml = None
        self.integratedServerPort = 0
        self.role = 0
        self.agent_count = 0
        self.resets = 0
        self.ns = '{http://ProjectMalmo.microsoft.com}'
        self.client_socket = None
        self.server = 'localhost'  # The mission server
        self.port = 9000  # The mission server port
        self.server2 = self.server  # optional server for agent (role <> 0)
        self.port2 = self.port + self.role  # optional server port for agent
        self.resync_period = 0
        self.turn_key = ""
        self.exp_uid = ""
        self.done = True
        self.step_options = None
        self.width = 0
        self.height = 0
        self.depth = 0
        self.reshape = reshape

    def init(self, xml, port, server=None,
             server2=None, port2=None,
             role=0, exp_uid=None, episode=0,
             action_filter=None, resync=0, step_options=0, action_space=None):
        """"Initialize a Malmo environment.
            xml - the mission xml.
            port - the MalmoEnv service's port.
            server - the MalmoEnv service address. Default is localhost.
            server2 - the MalmoEnv service address for given role if not 0.
            port2 - the MalmoEnv service port for given role if not 0.
            role - the agent role (0..N-1) for missions with N agents. Defaults to 0.
            exp_uid - the experiment's unique identifier. Generated if not given.
            episode - the "reset" start count for experiment re-starts. Defaults to 0.
            action_filter - an optional list of valid actions to filter by. Defaults to simple commands.
            step_options - encodes withTurnKey and withInfo in step messages. Defaults to info included,
            turn if required.
        """
        if action_filter is None:
            action_filter = {"move", "turn", "use", "attack"}

        if not xml.startswith('<Mission'):
            i = xml.index("<Mission")
            if i == -1:
                raise EnvException("Mission xml must contain <Mission> tag.")
            xml = xml[i:]

        self.xml = etree.fromstring(xml)
        self.role = role
        if exp_uid is None:
            self.exp_uid = str(uuid.uuid4())
        else:
            self.exp_uid = exp_uid

        command_parser = CommandParser(action_filter)
        commands = command_parser.get_commands_from_xml(self.xml, self.role)
        actions = command_parser.get_actions(commands)
        # print("role " + str(self.role) + " actions " + str(actions)

        if action_space:
            self.action_space = action_space
        else:
            self.action_space = ActionSpace(actions)

        self.port = port
        if server is not None:
            self.server = server
        if server2 is not None:
            self.server2 = server2
        else:
            self.server2 = self.server
        if port2 is not None:
            self.port2 = port2
        else:
            self.port2 = self.port + self.role

        self.agent_count = len(self.xml.findall(self.ns + 'AgentSection'))
        turn_based = self.xml.find('.//' + self.ns + 'TurnBasedCommands') is not None
        if turn_based:
            self.turn_key = 'AKWozEre'
        else:
            self.turn_key = ""
        if step_options is None:
            self.step_options = 0 if not turn_based else 2
        else:
            self.step_options = step_options
        self.done = True
        # print("agent count " + str(self.agent_count) + " turn based  " + turn_based)
        self.resync_period = resync
        self.resets = episode

        e = etree.fromstring("""<MissionInit xmlns="http://ProjectMalmo.microsoft.com" 
                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                                SchemaVersion="" PlatformVersion=""" + '\"' + malmo_version + '\"' +
                             """>
                                <ExperimentUID></ExperimentUID>
                                <ClientRole>0</ClientRole>
                                <ClientAgentConnection>
                                    <ClientIPAddress>127.0.0.1</ClientIPAddress>
                                    <ClientMissionControlPort>0</ClientMissionControlPort>
                                    <ClientCommandsPort>0</ClientCommandsPort>
                                    <AgentIPAddress>127.0.0.1</AgentIPAddress>
                                    <AgentMissionControlPort>0</AgentMissionControlPort>
                                    <AgentVideoPort>0</AgentVideoPort>
                                    <AgentDepthPort>0</AgentDepthPort>
                                    <AgentLuminancePort>0</AgentLuminancePort>
                                    <AgentObservationsPort>0</AgentObservationsPort>
                                    <AgentRewardsPort>0</AgentRewardsPort>
                                    <AgentColourMapPort>0</AgentColourMapPort>
                                    </ClientAgentConnection>
                                </MissionInit>""")
        e.insert(0, self.xml)
        self.xml = e
        self.xml.find(self.ns + 'ClientRole').text = str(self.role)
        self.xml.find(self.ns + 'ExperimentUID').text = self.exp_uid
        if self.role != 0 and self.agent_count > 1:
            e = etree.Element(self.ns + 'MinecraftServerConnection',
                              attrib={'address': self.server,
                                      'port': str(0)
                                      })
            self.xml.insert(2, e)

        video_producers = self.xml.findall('.//' + self.ns + 'VideoProducer')
        assert len(video_producers) == self.agent_count
        video_producer = video_producers[self.role]
        self.width = int(video_producer.find(self.ns + 'Width').text)
        self.height = int(video_producer.find(self.ns + 'Height').text)
        want_depth = video_producer.attrib["want_depth"]
        self.depth = 4 if want_depth is not None and (want_depth == "true" or want_depth == "1") else 3
        # print(str(self.width) + "x" + str(self.height) + "x" + str(self.depth))
        self.observation_space = VisualObservationSpace(self.width, self.height, self.depth)
        # print(etree.tostring(self.xml))

    @staticmethod
    def _hello(sock):
        comms.send_message(sock, ("<MalmoEnv" + malmo_version + "/>").encode())

    def reset(self):
        """gym api reset"""

        if self.resync_period > 0 and (self.resets + 1) % self.resync_period == 0:
            self.exit_resync()

        while not self.done:
            self.done = self._quit_episode()
            if not self.done:
                time.sleep(0.1)

        return self._start_up()

    @retry
    def _start_up(self):
        self.resets += 1
        if self.role != 0:
            self._find_server()
        if not self.client_socket:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            # print("connect " + self.server2 + ":" + str(self.port2))
            sock.connect((self.server2, self.port2))
            self._hello(sock)
            self.client_socket = sock  # Now retries will use connected socket.
        self._init_mission()
        self.done = False
        return self._peek_obs()

    def _peek_obs(self):
        obs = None
        start_time = time.time()
        while not self.done and (obs is None or len(obs) == 0):
            peek_message = "<Peek/>"
            comms.send_message(self.client_socket, peek_message.encode())
            obs = comms.recv_message(self.client_socket)
            reply = comms.recv_message(self.client_socket)
            done, = struct.unpack('!b', reply)
            self.done = done == 1
            if obs is None or len(obs) == 0:
                if time.time() - start_time > MAX_WAIT:
                    self.client_socket.close()
                    self.client_socket = None
                    raise MissionInitException('too long waiting for first observation')
                time.sleep(0.1)

            obs = np.frombuffer(obs, dtype=np.uint8)

        if obs is None or len(obs) == 0 or obs.size == 0:
            if self.reshape:
                obs = np.zeros((self.height, self.width, self.depth), dtype=np.uint8)
            else: 
                obs = np.zeros(self.height * self.width * self.depth, dtype=np.uint8)
        elif self.reshape:
            obs = obs.reshape((self.height, self.width, self.depth)).astype(np.uint8)

        return obs

    def _quit_episode(self):
        comms.send_message(self.client_socket, "<Quit/>".encode())
        reply = comms.recv_message(self.client_socket)
        ok, = struct.unpack('!I', reply)
        return ok != 0

    def render(self):
        """gym api render"""
        pass

    def seed(self):
        pass

    def step(self, action):
        """gym api step"""
        obs = None
        reward = None
        info = None
        turn = True
        withturnkey = self.step_options < 2
        withinfo = self.step_options == 0 or self.step_options == 2

        while not self.done and \
                ((obs is None or len(obs) == 0) or
                 (withinfo and info is None) or turn):
            step_message = "<Step" + str(self.step_options) + ">" + \
                           self.action_space[action] + \
                           "</Step" + str(self.step_options) + " >"
            comms.send_message(self.client_socket, step_message.encode())
            if withturnkey:
                comms.send_message(self.client_socket, self.turn_key.encode())
            obs = comms.recv_message(self.client_socket)

            reply = comms.recv_message(self.client_socket)
            reward, done, sent = struct.unpack('!dbb', reply)
            self.done = done == 1
            if withinfo:
                info = comms.recv_message(self.client_socket).decode('utf-8')

            turn_key = comms.recv_message(self.client_socket).decode('utf-8') if withturnkey else ""
            # print("[" + str(self.role) + "] TK " + turn_key + " self.TK " + str(self.turn_key))
            if turn_key != "":
                if sent != 0:
                    turn = False
                # Done turns if: turn = self.turn_key == turn_key
                self.turn_key = turn_key
            else:
                turn = sent == 0

            if (obs is None or len(obs) == 0) or turn:
                time.sleep(0.1)
            obs = np.frombuffer(obs, dtype=np.uint8)

        if self.reshape:
            if obs.size == 0:
                obs = np.zeros((self.height, self.width, self.depth), dtype=np.uint8)
            else:
                obs = obs.reshape((self.height, self.width, self.depth)).astype(np.uint8)

        return obs, reward, self.done, info

    def close(self):
        """gym api close"""
        try:
            # Purge last token from head node with <Close> message.
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.connect((self.server, self.port))
            self._hello(sock)

            comms.send_message(sock, ("<Close>" + self._get_token() + "</Close>").encode())
            reply = comms.recv_message(sock)
            ok, = struct.unpack('!I', reply)
            assert ok
            sock.close()
        except Exception as e:
            self._log_error(e)
        if self.client_socket:
            self.client_socket.close()
            self.client_socket = None

    def reinit(self):
        """Use carefully to reset the episode count to 0."""
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.server, self.port))
        self._hello(sock)

        comms.send_message(sock, ("<Init>" + self._get_token() + "</Init>").encode())
        reply = comms.recv_message(sock)
        sock.close()
        ok, = struct.unpack('!I', reply)
        return ok != 0

    def status(self, head):
        """Get status from server.
        head - Ping the the head node if True.
        """
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        if head:
            sock.connect((self.server, self.port))
        else:
            sock.connect((self.server2, self.port2))
        self._hello(sock)

        comms.send_message(sock, "<Status/>".encode())
        status = comms.recv_message(sock).decode('utf-8')
        sock.close()
        return status

    def exit(self):
        """Use carefully to cause the Minecraft service to exit (and hopefully restart).
        Likely to throw communication errors so wrap in exception handler.
        """
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.server2, self.port2))
        self._hello(sock)

        comms.send_message(sock, ("<Exit>" + self._get_token() + "</Exit>").encode())
        reply = comms.recv_message(sock)
        sock.close()
        ok, = struct.unpack('!I', reply)
        return ok != 0

    def resync(self):
        """make sure we can ping the head and assigned node.
        Possibly after an env.exit()"""
        success = 0
        for head in [True, False]:
            for _ in range(30):
                try:
                    self.status(head)
                    success += 1
                    break
                except Exception as e:
                    self._log_error(e)
                    time.sleep(10)

        if success != 2:
            raise EnvException("Failed to contact service" + (" head" if success == 0 else ""))

    def exit_resync(self):
        """Exit the current Minecraft and wait for new one to replace it."""
        print("********** exit & resync **********")
        try:
            if self.client_socket:
                self.client_socket.close()
                self.client_socket = None
            try:
                self.exit()
            except Exception as e:
                self._log_error(e)
            print("Pause for exit(s) ...")
            time.sleep(60)
        except (socket.error, ConnectionError):
            pass
        self.resync()

    def _log_error(self, exn):
        pass  # Keeping pylint happy

    def _find_server(self):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.server, self.port))
        self._hello(sock)

        start_time = time.time()
        port = 0
        while port == 0:
            comms.send_message(sock, ("<Find>" + self._get_token() + "</Find>").encode())
            reply = comms.recv_message(sock)
            port, = struct.unpack('!I', reply)
            if port == 0:
                if time.time() - start_time > MAX_WAIT:
                    if self.client_socket:
                        self.client_socket.close()
                        self.client_socket = None
                    raise MissionInitException('too long finding mission to join')
                time.sleep(1)
        sock.close()
        # print("Found mission integrated server port " + str(port))
        self.integratedServerPort = port
        e = self.xml.find(self.ns + 'MinecraftServerConnection')
        if e is not None:
            e.attrib['port'] = str(self.integratedServerPort)

    def _init_mission(self):
        ok = 0
        while ok != 1:
            xml = etree.tostring(self.xml)
            token = (self._get_token() + ":" + str(self.agent_count)).encode()
            # print(xml.decode())
            comms.send_message(self.client_socket, xml)
            comms.send_message(self.client_socket, token)

            reply = comms.recv_message(self.client_socket)
            ok, = struct.unpack('!I', reply)
            self.turn_key = comms.recv_message(self.client_socket).decode('utf-8')
            if ok != 1:
                time.sleep(1)

    def _get_token(self):
        return self.exp_uid + ":" + str(self.role) + ":" + str(self.resets)


def make():
    return Env()
