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
from gym import spaces

import cloudpickle
import pickle

import socket
import threading

import proxyenv.comms as comms
import traceback
from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter

import numpy as np

BIND_IP = '0.0.0.0'
BIND_PORT = 50010


class ServerEnv(gym.Env):
    """Dummy server env for testing """
    def __init__(self, config):
        self.config = config

        self.action_space = gym.spaces.Discrete(2)  # TODO
        self.observation_space = gym.spaces.Box(low=0, high=255, shape=(84, 84, 1))  # TODO
        self.steps = 0

    def reset(self):
        print("reset")
        return np.zeros((84, 84, 1), dtype=np.unit8)

    def step(self, action):
        print("step " + repr(action))
        print("at " + str(self.steps))
        self.steps += 1
        obs = np.zeros((84, 84, 1), dtype=np.uint8)
        reward = 0.0
        done = (self.steps % 10 == 0)
        print("done " + str(done))
        info = {'total_steps': self.steps}
        return obs, reward, done, info

    def seed(self, seed=None):
        print("seed " + str(seed))

    def render(self, mode='human'):
        print("render me for " + str(mode))

    def close(self):
        pass


def default_create_env(config):
    """Server env creation factory function"""
    return ServerEnv(config)


def default_create_action_space(env, config):
    """Create an action space that can be pickled
    (or None to leave it for the remote client to sort out).
    """
    return env.action_space


TIMEOUT = 300


def handle_client_connection(client_socket, create_env, create_action_space):
    """handle a client connection """
    msg = comms.recv_message(client_socket)
    config = pickle.loads(msg)
    print('Received env config {}'.format(repr(config)))

    if isinstance(config, gym.Env):
        # Client sent an actual environment - use it.
        env = config
    if isinstance(config, str):
        # Use string with gym env registry.
        env = gym.make(config)
    else:
        # Config is an object we pass to our server env factory method.
        env = create_env(config)

    print("created env " + repr(env))

    a_s = create_action_space(env, config)
    o_s = env.observation_space
    reply = cloudpickle.dumps((a_s, o_s))

    comms.send_message(client_socket, reply)

    while True:
        msg = comms.recv_message(client_socket)
        if msg is None:
            print("eof")
            break
        request = pickle.loads(msg)
        # print("Proxy env request " + repr(request))
        cmd, arg = request
        if cmd == 'r':
            obs = env.reset()
            reply = cloudpickle.dumps(obs)
            comms.send_message(client_socket, reply)
        elif cmd == 's':
            result = env.step(arg)
            reply = cloudpickle.dumps(result)
            comms.send_message(client_socket, reply)
        elif cmd == 'se':
            result = env.seed(arg)
            reply = cloudpickle.dumps(result)
            comms.send_message(client_socket, reply)
        elif cmd == 're':
            result = env.render(arg)
            reply = cloudpickle.dumps(result)
            comms.send_message(client_socket, reply)
        else:
            print('Skip unknown proxy env command!')
            break
    env.close()
    client_socket.close()


def serve_env(bind_port=BIND_PORT, create_env=default_create_env, create_action_space=default_create_action_space):
    """Serve proxy openai gym environmets"""
    print("serve_env")
    # traceback.print_stack()
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((BIND_IP, bind_port))
    server.listen(5)  # max backlog of connections

    print('proxy env listening on {}:{}'.format(BIND_IP, bind_port))

    while True:
        client_sock, address = server.accept()
        print('Proxy env accepted connection from {}:{}'.format(address[0], address[1]))
        client_sock.settimeout(TIMEOUT)
        client_handler = threading.Thread(
            target=handle_client_connection,
            args=(client_sock, create_env, create_action_space)
        )
        client_handler.start()


if __name__ == "__main__":
    arg_parser = ArgumentParser(description='Service to proxy an openai gym environment',
                                formatter_class=ArgumentDefaultsHelpFormatter)
    arg_parser.add_argument('--port', type=int, default=BIND_PORT, help='Optional port to bind to.')
    args = arg_parser.parse_args()

    serve_env(args.port)


