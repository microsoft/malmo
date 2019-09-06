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
import proxyenv.client
from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter
import proxyenv.action_utils

arg_parser = ArgumentParser(description='example minerl env runner',
                            formatter_class=ArgumentDefaultsHelpFormatter)
arg_parser.add_argument('--host', type=str, default='localhost', help='Optional host to connect to.')
arg_parser.add_argument('--port', type=int, default=50050, help='Optional port to connect to.')
args = arg_parser.parse_args()

config = {"mission": "MineRLNavigateDense-v0"}

env = proxyenv.client.ProxyEnv(args.host, args.port, config)
if isinstance(env.action_space, gym.spaces.Dict):
    env = proxyenv.action_utils.DictToMultiDiscreteActionWrapper(env)

env.reset()

done = False
while not done:
    action = env.action_space.sample()
    print("action " + repr(action))
    obs, reward, done, info = env.step(action)
    print("obs " + repr(obs))
    print("reward " + repr(reward))
    print("done " + repr(done))
    print("info " + repr(info))

env.close()
