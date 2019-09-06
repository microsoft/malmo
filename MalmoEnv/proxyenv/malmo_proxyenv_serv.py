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
import numpy as np

import proxyenv.server
from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter

import proxyenv.server
import makeproxyenv


def env_creator(env_id):
    def create_env_func(config):
        mission_file = config.get("mission_file", "MalmoPlatform9000/MalmoEnv/missions/findthegoal.xml")
        makeproxyenv.start_minecraft(env_id)
        env = makeproxyenv.create_env(env_id, mission_file)
        return env
    return create_env_func


def  create_action_space(env, config):
    action_space = env.action_space
    print("action space " + repr(action_space))
    return action_space


if __name__ == '__main__':
    arg_parser = ArgumentParser(description='example malmo env service',
                                formatter_class=ArgumentDefaultsHelpFormatter)
    arg_parser.add_argument('--port', type=int, default=50050, help='Optional port to bind proxyenv to.')
    arg_parser.add_argument('--env_id', type=int, default=0,
                            help='Optional environment id. Offsets the env port from 9000.')
    args = arg_parser.parse_args()
    proxyenv.server.serve_env(bind_port=args.port, create_env=env_creator(args.env_id), create_action_space=create_action_space)
