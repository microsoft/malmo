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

class ActionSpace:
    # TODO create commands from mission xml.
    def __init__(self, actions):
        self.actions = actions

    def sample(self):
        return random.randint(1, len(self.actions)) - 1

    def get(self, action):
        return self.actions[action]


class Env:

    def __init__(self, actions=None):
        if actions is None:
            actions = ["move 1", "move -1", "turn 1", "turn -1"]
        self.action_space = ActionSpace(actions)
        self.xml = None

        self.integratedServerPort = 0
        self.expId = None
        self.role = 0
        self.agentCount = 0
        self.resets = 0
        self.ns = "{http://ProjectMalmo.microsoft.com}"
        self.clientsocket = None
        self.server = "localhost"  # The game server
        self.port = 9000  # The game port
        self.server2 = self.server  # optional server for agent
        self.port2 = self.port + self.role  # optional port for agent
        self.turnKey = ""

    def reset(self):
        self.resets += 1
        if self.role != 0:
            self._find_server()
        if not self.clientsocket:
            self.clientsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            print("connect " + self.server2 + ":" + str(self.port2))
            self.clientsocket.connect((self.server2, self.port2))
        self._init_mission()

    def init(self, xml, port, server=None, server2=None, port2=None, episode=0):
        """"Initialize a Malmo environment.
        XML is a <MissionInit> containing the <Mission>"""
        self.xml = etree.fromstring(xml)
        self.role = int(self.xml.find(self.ns + "ClientRole").text)
        print("role " + str(self.role))
        self.expId = self.xml.find(self.ns + "ExperimentUID").text
        print("envId " + str(self.expId))
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
        self.agentCount = len(self.xml.findall(self.ns + "Mission/" + self.ns + "AgentSection"))
        e = self.xml.find(self.ns + "MinecraftServerConnection")
        if e is not None:
            e.attrib['port'] = "0"
        self.resets = episode
        self.turnKey = ""
        # print(xml)

    def step(self, action):
        obs = None
        done = False
        while not done and (obs is None or len(obs) == 0):
            comms.send_message(self.clientsocket, ("<Step>" + self.action_space.get(action) + "</Step>").encode())
            comms.send_message(self.clientsocket, self.turnKey.encode())
            obs = comms.recv_message(self.clientsocket)
            reply = comms.recv_message(self.clientsocket)
            reward, done = struct.unpack('!dI', reply)
            self.turnKey = comms.recv_message(self.clientsocket).decode('utf-8')
            info = None
            if obs is None or len(obs) == 0:
                time.sleep(0.1)
            obs = np.frombuffer(obs, dtype=np.uint8)

        return obs, reward, done, info

    def reinit(self):
        """Use carefully to reset the episode count to 0."""
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.server, self.port))

        comms.send_message(sock, ("<Init>" + self._get_token() + "</Init>").encode())
        reply = comms.recv_message(sock)
        ok, = struct.unpack('!I', reply)
        return ok != 0

    def close(self):
        if self.clientsocket:
            self.clientsocket.close()

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
        e = self.xml.find(self.ns + "MinecraftServerConnection")
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
            self.turnkey = comms.recv_message(self.clientsocket).decode('utf-8')
            if ok != 1:
                time.sleep(1)

    def _get_token(self):
        return self.expId + ":" + str(self.role) + ":" + str(self.resets)


def make():
    return Env()
