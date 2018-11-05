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

import malmoenv
import time
import json


def test_mission():
    return '''<?xml version="1.0" encoding="UTF-8" ?> 
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Let's run!</Summary>
        </About>
        
        <ServerSection>
            <ServerInitialConditions>
                <AllowSpawning>false</AllowSpawning>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*168:1,41;3;biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="0" y1="226" z1="0" x2="0" y2="226" z2="1000" type="stone" variant="smooth_granite"/>
                    <DrawBlock x="0" y="226" z="10" type="emerald_block"/>
                    <DrawBlock x="0" y="226" z="11" type="redstone_block"/>
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="30000"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Britney</Name>
            <AgentStart>
                <Placement x="0.5" y="227" z="0.5"/>
            </AgentStart>
            <AgentHandlers>
                <DiscreteMovementCommands/>
                <ObservationFromDistance>
                    <Marker name="Start" x="0.5" y="227" z="0.5"/>
                </ObservationFromDistance>
                <RewardForReachingPosition>
                    <Marker oneshot="true" reward="100" tolerance="0.1" x="0.5" y="227" z="10.5"/>
                    <Marker oneshot="true" reward="-1000" tolerance="0.1" x="0.5" y="227" z="11.5"/>
                </RewardForReachingPosition>
                <AgentQuitFromReachingPosition>
                    <Marker tolerance="0.1" x="0.5" y="227" z="10.5"/>
                </AgentQuitFromReachingPosition>
               <VideoProducer want_depth="false">
                    <Width>320</Width>
                    <Height>240</Height>
                </VideoProducer>
            </AgentHandlers>
        </AgentSection>
    </Mission>'''


if __name__ == '__main__':

    xml = test_mission()
    env = malmoenv.make()

    env.init(xml, 9000, action_filter=['movesouth'])
    obs = env.reset()

    dist = 1.0
    rewards = 0
    done = False

    while not done:
        action = env.action_space.sample()
        print(action)
        obs, reward, done, info = env.step(action)
        print("reward: " + str(reward))
        print("done: " + str(done))
        print("obs: " + str(obs))
        print("info" + info)
        if info != "":
            info_ = json.loads(info)
            if info_["distanceFromStart"] != dist:
                print("Distance from start estimated: " + str(dist) + " got " + str(info_["distanceFromStart"]))
            dist = info_["distanceFromStart"] + 1.0

        rewards += reward
        time.sleep(.1)

    env.close()

    if reward != 100:
        print("TEST FAIL - expected last reward of 100 - got " + str(rewards))
        exit(-1)
    if rewards != 100:
        print("TEST FAIL - expected total reward of 100 - got " + str(rewards))
        exit(-1)

    print("PASS")
