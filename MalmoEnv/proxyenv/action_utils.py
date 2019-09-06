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
from collections import OrderedDict


def translate_action_space(action_space):
    """Translate action space into simple gym space objects."""
    if isinstance(action_space, gym.spaces.Dict):
        action_space2 = {}
        for action in action_space.spaces:
            # print("translate sub action space" + action)
            sub_space = translate_action_space(action_space[action])
            if sub_space is not None:
                action_space2[action] = sub_space
        action_space2 = gym.spaces.Dict(action_space2)
    elif isinstance(action_space, gym.spaces.Discrete):
        # print("translate discrete " + repr(action_space.n))
        action_space2 = gym.spaces.Discrete(action_space.n)
    elif isinstance(action_space, gym.spaces.Box):
        # print("translate box " + repr(action_space))
        action_space2 = gym.spaces.Box(action_space.low.flatten()[0],
                                       action_space.high.flatten()[0],
                                       action_space.shape, action_space.dtype)
    else:
        print("Warning - unimplemented in action space " + repr(action_space))
        action_space2 = None
    return action_space2


class DictToMultiDiscreteActionWrapper(gym.ActionWrapper):

    def __init__(self, env):
        action_space = env.action_space
        self.action_space_names = []
        # Keep everything as int (not gym's np.int64 usage)
        action_space_dims = []
        for action in action_space.spaces:
            space = action_space.spaces[action]
            if isinstance(space, gym.spaces.Discrete):
                self.action_space_names.append(action)
                action_space_dims.append(np.int32(space.n))
        env.action_space = gym.spaces.MultiDiscrete(action_space_dims)
        env.action_space.dtype = np.dtype(int)
        env.action_space.nvec = env.action_space.nvec.astype(np.int32)

        print("as multi discrete " + repr(env.action_space))
        super().__init__(env)

    def action(self, action):
        action2 = OrderedDict()
        for i, name in enumerate(self.action_space_names):
            action2[name] = int(action[i])  # Minerl incorrectly tests for int (and not np.int64)
        print(action2)
        return action2


class DictToDiscreteActionWrapper(gym.ActionWrapper):

    def __init__(self, env):
        action_space = env.action_space
        self.action_space_name_values = []

        for action in action_space.spaces:
            space = action_space.spaces[action]
            if isinstance(space, gym.spaces.Discrete):
                for i in range(np.int32(space.n)):
                    self.action_space_name_values.append((action, int(i)))

        env.action_space = gym.spaces.Discrete(len(self.action_space_name_values))
        env.action_space.dtype = np.dtype(int)

        print("as discrete " + repr(env.action_space))
        super().__init__(env)

    def action(self, action):
        action_name, action_value = self.action_space_name_values[action]
        action2 = dict()
        action2[action_name] = int(action_value)  # Minerl incorrectly tests for int (and not np.int64) 
        return action2


if __name__ == "__main__":
    action_space = gym.spaces.Dict({"attack": gym.spaces.Discrete(2),
                                    "back": gym.spaces.Discrete(2), "forward": gym.spaces.Discrete(2),
                                    "camera": gym.spaces.Box(low=-180, high=180, shape=(2,), dtype=np.float32)})
    action_space = translate_action_space(action_space)
    print(repr(action_space))

    env = gym.Env()
    env.action_space = action_space
    env2 = DictToMultiDiscreteActionWrapper(env)
    try:
        env2.step(env2.action_space.sample())
    except NotImplementedError:
        pass

    env = gym.Env()
    env.action_space = action_space
    env2 = DictToDiscreteActionWrapper(env)
    try:
        env2.step(env2.action_space.sample())
    except NotImplementedError:
        pass
