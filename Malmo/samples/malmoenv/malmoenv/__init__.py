# ------------------------------------------------------------------------------------------------
# Copyright (c) 2016 Microsoft Corporation
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


class ActionSpace:
    """Malmo commands as gym action space"""
    def __init__(self, actions):
        self.actions = actions

    def sample(self):
        return random.randint(1, len(self.actions)) - 1

    def get(self, action):
        return self.actions[action]

    def __len__(self):
        return len(self.actions)


class Env:
    """Malmo "Env" open ai gym compatible API"""
    def __init__(self):
        self.action_space = None
        self.xml = None
        self.integratedServerPort = 0
        self.expUId = None
        self.role = 0
        self.agentCount = 0
        self.resets = 0
        self.ns = '{http://ProjectMalmo.microsoft.com}'
        self.clientsocket = None
        self.server = 'localhost'  # The game server
        self.port = 9000  # The game port
        self.server2 = self.server  # optional server for agent
        self.port2 = self.port + self.role  # optional port for agent
        self.turn_key = ""
        self.expUId = ""

    def init(self, xml, port,
             server=None, server2=None, port2=None,
             role=0, exp_uid=None, episode=0,
             action_filter=None):
        """"Initialize a Malmo environment.
            xml - the mission xml.
            port - the MalmoEnv service's port.
            server - the MalmoEnv service address. Default is localhost.
            server2 - the MalmoEnv service address for given role if not 0.
            port2 - the MalmoEnv service port for given role if not 0.
            role - the agent role (0..N-1) for missions with N agents. Defaults to 0.
            exp_uid - the experiment's unique identifier. Generated if not given
            episode - the "reset" start count for experiment re-starts. Defaults to 0.
        """
        if action_filter is None:
            action_filter = {"move", "turn", "use", "attack"}
        self.xml = etree.fromstring(xml)
        self.role = role
        if exp_uid is None:
            self.expUId = str(uuid.uuid4())
        else:
            self.expUId = exp_uid
        print("role " + str(self.role))

        command_parser = CommandParser(action_filter)
        commands = command_parser.get_commands_from_xml(self.xml, self.role)
        actions = command_parser.get_actions(commands)
        print(actions)
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
        self.agentCount = len(self.xml.findall(self.ns + 'AgentSection'))
        # print("agent count " + str(self.agentCount))

        self.resets = episode
        self.turn_key = ""

        e = etree.fromstring("""<MissionInit xmlns="http://ProjectMalmo.microsoft.com" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    SchemaVersion="" PlatformVersion="0.36.0">
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
        self.xml.find(self.ns + 'ExperimentUID').text = self.expUId
        if self.role != 0 and self.agentCount > 1:
            e = etree.Element(self.ns + 'MinecraftServerConnection',
                              attrib={'address': self.server,
                                      'port': str(0)
                                      })
            self.xml.insert(2, e)
        # print(etree.tostring(self.xml))

    def reset(self):
        """gym api reset"""
        self.resets += 1
        if self.role != 0:
            self._find_server()
        if not self.clientsocket:
            self.clientsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            print("connect " + self.server2 + ":" + str(self.port2))
            self.clientsocket.connect((self.server2, self.port2))
        self._init_mission()

    def step(self, action):
        """gym api step"""
        obs = None
        done = False
        reward = None
        info = None
        turn = True
        while not done and ((obs is None or len(obs) == 0) or turn):
            comms.send_message(self.clientsocket, ("<Step>" + self.action_space.get(action) + "</Step>").encode())
            comms.send_message(self.clientsocket, self.turn_key.encode())
            obs = comms.recv_message(self.clientsocket)
            reply = comms.recv_message(self.clientsocket)
            reward, done = struct.unpack('!dI', reply)
            info = comms.recv_message(self.clientsocket).decode('utf-8')

            turn_key = comms.recv_message(self.clientsocket).decode('utf-8')
            if turn_key != "":
                turn = self.turn_key == turn_key
            else:
                turn = False
            self.turn_key = turn_key

            if (obs is None or len(obs) == 0) or turn:
                time.sleep(0.1)
            obs = np.frombuffer(obs, dtype=np.uint8)

        return obs, reward, done, info

    def close(self):
        """gym api close"""
        # Purge last token from head node.
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.server, self.port))

        comms.send_message(sock, ("<Close>" + self._get_token() + "</Close>").encode())
        reply = comms.recv_message(sock)
        ok, = struct.unpack('!I', reply)

        sock.close()
        if self.clientsocket:
            self.clientsocket.close()

    def reinit(self):
        """Use carefully to reset the episode count to 0."""
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.server, self.port))

        comms.send_message(sock, ("<Init>" + self._get_token() + "</Init>").encode())
        reply = comms.recv_message(sock)
        ok, = struct.unpack('!I', reply)
        return ok != 0

    def exit(self):
        """Use carefully to cause the service to exit (and hopefully restart).
            Likely to throw communication errors so use carefully.
        """
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.server, self.port))

        comms.send_message(sock, ("<Exit>" + self._get_token() + "</Exit>").encode())
        reply = comms.recv_message(sock)
        ok, = struct.unpack('!I', reply)
        return ok != 0

    def _find_server(self):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.server, self.port))

        port = 0
        while port == 0:
            comms.send_message(sock, ("<Find>" + self._get_token() + "</Find>").encode())
            reply = comms.recv_message(sock)
            port, = struct.unpack('!I', reply)
            if port == 0:
                time.sleep(1)
        sock.close()
        print("found port " + str(port))
        self.integratedServerPort = port
        e = self.xml.find(self.ns + 'MinecraftServerConnection')
        if e is not None:
            e.attrib['port'] = str(self.integratedServerPort)

    def _init_mission(self):
        ok = 0
        while ok != 1:
            xml = etree.tostring(self.xml)
            token = (self._get_token() + ":" + str(self.agentCount)).encode()
            # print(xml.decode())
            comms.send_message(self.clientsocket, xml)
            comms.send_message(self.clientsocket, token)

            reply = comms.recv_message(self.clientsocket)
            ok, = struct.unpack('!I', reply)
            self.turn_key = comms.recv_message(self.clientsocket).decode('utf-8')
            if ok != 1:
                time.sleep(1)

    def _get_token(self):
        return self.expUId + ":" + str(self.role) + ":" + str(self.resets)


def make():
    return Env()
