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

import malmoenv
import argparse
from pathlib import Path
import time


if __name__ == '__main__':

    parser = argparse.ArgumentParser(description='malmovnv test')
    parser.add_argument('--mission', type=str, default='agentSingle.xml', help='the mission xml')
    parser.add_argument('--port', type=int, default=9000, help='the mission server port')
    parser.add_argument('--port2', type=int, default=9000, help="(Multi-agent) the agent's mission port")
    parser.add_argument('--rounds', type=int, default=1, help='the number of resets to perform - default is 1')
    parser.add_argument('--episode', type=int, default=0, help='the start episode - default is 0')
    parser.add_argument('--role', type=int, default=0, help='the agent role - defaults to 0')
    parser.add_argument('--experimentUniqueId', type=str, default=None,
                        help="the experiment's unique id. Generated if not specified")
    args = parser.parse_args()

    xml = Path(args.mission).read_text()
    env = malmoenv.make()

    env.init(xml, args.port, port2=args.port2, role=args.role, exp_uid=args.experimentUniqueId, episode=args.episode)

    for i in range(args.rounds):
        print("reset " + str(i))
        obs = env.reset()

        done = False
        while not done:
            action = env.action_space.sample()

            obs, reward, done, info = env.step(action)
            print("reward: " + str(reward))
            print("done: " + str(done))
            print("obs: " + str(obs))
            # time.sleep(3)
            time.sleep(.05)

    env.close()
