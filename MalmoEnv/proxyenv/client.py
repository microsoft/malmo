# ------------------------------------------------------------------------------------------------
# Copyright (c) 2019 Microsoft Corporation
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
import socket

import cloudpickle
import pickle

import proxyenv.comms as comms

TIMEOUT = 300
PORT = 50010
HOST = 'localhost'
from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter


class ProxyEnv:
    """A proxy environment for openai gym"""
    def __init__(self, host, port, config):
        """create a proxy env"""
        self.host = host
        self.port = port

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(TIMEOUT)
        sock.connect((self.host, self.port))
        self.client_sock = sock
 
        msg = cloudpickle.dumps(config)
        comms.send_message(self.client_sock, msg)

        reply = comms.recv_message(self.client_sock)
        r = pickle.loads(reply)
        action_space, observation_space = r

        print("created env with action space {} observation space {}"
              .format(repr(action_space), repr(observation_space)))

        self.action_space = action_space
        self.observation_space = observation_space
        self.metadata = {'render.modes': []}
        self.reward_range = (-float('inf'), float('inf'))

    def step(self, action):
        """proxy step"""
        msg = cloudpickle.dumps(('s', action))
        comms.send_message(self.client_sock, msg)
        reply = comms.recv_message(self.client_sock)
        step_response = pickle.loads(reply)
        return step_response

    def reset(self):
        """proxy reset"""
        msg = cloudpickle.dumps(('r', None))
        comms.send_message(self.client_sock, msg)
        reply = comms.recv_message(self.client_sock)
        reset_response = pickle.loads(reply)
        return reset_response

    def render(self, mode='human'):
        """proxy render"""
        msg = cloudpickle.dumps(('re', mode))
        comms.send_message(self.client_sock, msg)
        reply = comms.recv_message(self.client_sock)
        render_response = pickle.loads(reply)
        return render_response

    def seed(self, seed=None):
        """proxy seed"""
        msg = cloudpickle.dumps(('se', seed))
        comms.send_message(self.client_sock, msg)
        reply = comms.recv_message(self.client_sock)
        seed_response = pickle.loads(reply)
        return seed_response

    def close(self):
        """close"""
        self.client_sock.close() 


def main():
    arg_parser = ArgumentParser(description='Run a poxy environment',
                                formatter_class=ArgumentDefaultsHelpFormatter)
    arg_parser.add_argument('--host', type=str, default=HOST, help='Optional host to connect to.')
    arg_parser.add_argument('--port', type=int, default=PORT, help='Optional port to connect to.')
    args = arg_parser.parse_args()

    env = ProxyEnv(args.host, args.port, {'a': 'xyz'})

    env.seed(100.0)
    for i in range(10):
        print("-- reset --")
        env.reset()

        done = False
        while not done:
            action = env.action_space.sample()

            obs, reward, done, info = env.step(action)
            print("obs " + repr(obs))
            print("reward " + repr(reward))
            print("done " + repr(done))
            print("info " + repr(info))

            env.render()

    env.close()


if __name__ == "__main__":
    main()
