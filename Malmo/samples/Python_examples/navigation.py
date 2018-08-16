from __future__ import print_function
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

# Tests the AgentQuitFromReachingPosition handler under discrete movement.
# Agent just has to move forward a set number of times.
# A positive reward will be fired for touching the quit square.
# A large negative reward will be fired for touching the *following* square.
# In an ideal world, the agent will send exactly the right number of commands,
# only the first of these rewards will fire, and the mission will end.

# The actual behaviour depends upon the speed at which commands are sent.
# See https://github.com/Microsoft/malmo/issues/104 for some details.

from builtins import range
import MalmoPython
import os
import random
import sys
import time
import json
import errno
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()

agent_host.addOptionalFlag("stop,s", "Stop after required number of steps.")

agent_host.addOptionalFlag("wait,w", "Number of seconds to wait between sending commands.", 0.1)

malmoutils.parse_command_line(agent_host)

def GetMissionXML(num, video_xml):
    return '''<?xml version="1.0" encoding="UTF-8" standalone="no ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

        <About>
            <Summary>Navigation through survival world.</Summary>
        </About>

        <ModSettings>
            <MsPerTick>1</MsPerTick>
        </ModSettings>

        <ServerSection>
            <ServerInitialConditions>
                <Time>
                    <StartTime>6000</StartTime>
                    <AllowPassageOfTime>false</AllowPassageOfTime>
                </Time>
                <Weather>clear</Weather>
                <AllowSpawning>false</AllowSpawning>
            </ServerInitialConditions>
            <ServerHandlers>
                <DefaultWorldGenerator forceReset="true"/>
                <NavigationDecorator radius="64" placement="surface" discoveryRadius="1" randomizeCompassLocation="false">
                    <origin xCoordinate="0.0" yCoordinate="80.0" zCoordinate="0.0"/>
                    <block type="diamond_block"/>
                </NavigationDecorator>
                <ServerQuitFromTimeUp timeLimitMs="300000"/>
                <ServerQuitWhenAnyAgentFinishes/>
            </ServerHandlers>
            </ServerSection>

        <AgentSection mode="Survival">
            <Name>Columbus</Name>
            <AgentStart>
                <Placement x="0.0" y="80.0" z="0.0" pitch="0.0" yaw="0"/>
                <Inventory>
                    <InventoryObject slot="0" type="compass"/>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromCompass/>
                <VideoProducer want_depth="false">
                    <Width>640</Width>
                    <Height>480</Height>
                </VideoProducer>
                <DiscreteMovementCommands>
                    <ModifierList type="deny-list">
                        <command>attack</command>
                    </ModifierList>
                </DiscreteMovementCommands>
                <RewardForTouchingBlockType>
                    <Block reward="100.0" type="diamond_block" behaviour="onceOnly"/>
                </RewardForTouchingBlockType>
                <RewardForSendingCommand reward="-1.0"/>
                <AgentQuitFromTouchingBlockType>
                    <Block type="diamond_block"/>
                </AgentQuitFromTouchingBlockType>
            </AgentHandlers>
        </AgentSection>
    </Mission>'''

validate = True

WAIT_TIME = agent_host.getFloatArgument("wait")
NUM_REPEATS = 20
STOP = False

agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.KEEP_ALL_OBSERVATIONS)
agent_host.setRewardsPolicy(MalmoPython.RewardsPolicy.KEEP_ALL_REWARDS)

for iRepeat in range(NUM_REPEATS):
    mission = MalmoPython.MissionSpec(GetMissionXML(iRepeat, malmoutils.get_video_xml(agent_host)), validate)
    # Set up recording
    mission_record = malmoutils.get_default_recording_object(agent_host, "Navigation" + str(iRepeat))
    max_retries = 3
    for retry in range(max_retries):
        try:
            agent_host.startMission(mission, mission_record)
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print("Error starting mission", e)
                exit(1)
            else:
                time.sleep(2)

    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        time.sleep(0.01)
        print(".", end="")
        world_state = agent_host.getWorldState()
        if len(world_state.errors):
            print()
            for error in world_state.errors:
                print("Error:", error.text)
                exit()
    print()

    # Main loop
    distance = 0
    yaw = 0
    commands_sent = 0
    total_rewards = 0
    while world_state.is_mission_running:
        if not STOP:
            agent_host.sendCommand("movesouth 1")
            commands_sent += 1
            print("C", ent="")
        time.sleep(WAIT_TIME)
        world_state = agent_host.getWorldState()
        if world_state.number_of_observations_since_last_state > 0:
            for ob in world_state.observations:
                jsonobj = json.loads(ob.text)
                distance = jsonobj.get(u'distance', 0)
                yaw = jsonobj.get(u'offset', 0)
                print('O{0:.0f}'.format(distance), end="")
                print('O{0:.0f}'.format(yaw), end="")
        if world_state.number_of_rewards_since_last_state > 0:
            for reward in world_state.rewards:
                if reward.getValue() == 0:
                    print("r", end="")
                elif reward.getValue() < 0:
                    print("neg", end="")
                elif reward.getValue() > 0:
                    print("pos", end="")
                else:
                    print("?", end="")
                total_rewards += reward.getValue()
        if world_state.is_mission_running:
            print("Run", end="")
        else:
            print("Not run", end="")
        print(" ", end="")
        
    print()
    print("Mission Ended - sent " + str(commands_sent) + " commands; final reward: " + str(total_rewards))
    print()
time.sleep(1)