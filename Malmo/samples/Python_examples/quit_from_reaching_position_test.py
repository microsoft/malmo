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
agent_host.addOptionalIntArgument( "length,l", "Number of steps required to reach goal square.", 10)
# Eg set the length to 0 or 1 to test https://github.com/Microsoft/malmo/issues/23

agent_host.addOptionalFlag( "stop,s", "Stop after required number of steps.")
# Eg if length is set to 10, will send 10 move commands and then wait for the mission to end.
# This can be used to test that commands are all being acted on, regardless of the speed they are sent at,
# and can give some indication of the latency between sending the final command and receiving the mission ended message.

agent_host.addOptionalFloatArgument( "wait,w", "Number of seconds to wait between sending commands.", 0.1)
# Setting this to something slow (eg 0.1) should show a clear cycle of commands/observations/rewards,
# and a quit triggered after the correct number of commands.
# Setting this to something faster (eg 0.05) should still show a clear cylce of commands/observations/rewards,
# but there may be extra commands sent unnecessarily at the end (which shouldn't be acted on).
# Setting this to something extreme (eg 0.01) should show behaviour whereby the commands get clustered together,
# but the agent should still quit correctly.
malmoutils.parse_command_line(agent_host)

def GetMissionXML(num, video_xml):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Let's run! #''' + str(num) + '''</Summary>
        </About>

        <ServerSection>
            <ServerInitialConditions>
                <AllowSpawning>false</AllowSpawning>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*168:1,41;3;biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="0" y1="226" z1="0" x2="0" y2="226" z2="1000" type="stone" variant="smooth_granite"/>
                    <DrawBlock x="0" y="226" z="''' + str(PATH_LENGTH) + '''" type="emerald_block"/>
                    <DrawBlock x="0" y="226" z="''' + str(PATH_LENGTH+1) + '''" type="redstone_block"/>
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="''' + str(MISSION_LENGTH * 1000) + '''"/>
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
                    <Marker oneshot="true" reward="100" tolerance="0.1" x="0.5" y="227" z="''' + str(PATH_LENGTH + 0.5) + '''"/>
                    <Marker oneshot="true" reward="-1000" tolerance="0.1" x="0.5" y="227" z="''' + str(PATH_LENGTH+1.5) + '''"/>
                </RewardForReachingPosition>      
                <AgentQuitFromReachingPosition>
                    <Marker tolerance="0.1" x="0.5" y="227" z="''' + str(PATH_LENGTH+0.5) + '''"/>
                </AgentQuitFromReachingPosition>''' + video_xml + '''
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

validate = True

PATH_LENGTH = agent_host.getIntArgument("length")
STOP = agent_host.receivedArgument("stop")
WAIT_TIME = agent_host.getFloatArgument("wait")
MISSION_LENGTH = 30
NUM_REPEATS = 10

if agent_host.receivedArgument("test"):
    print("Using test settings (overrides other command-line arguments).")
    NUM_REPEATS = 1
    WAIT_TIME = 0.2
    STOP = True
    PATH_LENGTH = 20
 
agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.KEEP_ALL_OBSERVATIONS)
agent_host.setRewardsPolicy(MalmoPython.RewardsPolicy.KEEP_ALL_REWARDS)

for iRepeat in range(NUM_REPEATS):
    my_mission = MalmoPython.MissionSpec(GetMissionXML(iRepeat, malmoutils.get_video_xml(agent_host)), validate)
    # Set up a recording
    my_mission_record = malmoutils.get_default_recording_object(agent_host, "QuitFromReachingPosition_Test" + str(iRepeat));
    max_retries = 3
    for retry in range(max_retries):
        try:
            agent_host.startMission( my_mission, my_mission_record )
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print("Error starting mission:",e)
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
                print("Error:",error.text)
                exit()
    print()

    # main loop:
    distance = 0
    commands_sent = 0
    total_rewards = 0
    while world_state.is_mission_running:
        if commands_sent < PATH_LENGTH or not STOP:
            agent_host.sendCommand("movesouth 1")
            commands_sent += 1
            print("C", end="")
        time.sleep(WAIT_TIME)
        world_state = agent_host.getWorldState()
        if world_state.number_of_observations_since_last_state > 0:
            for ob in world_state.observations:
                jsob = json.loads(ob.text)
                distance = jsob.get(u'distanceFromStart', 0)
                print('O{0:.0f}'.format(distance), end="")
        if world_state.number_of_rewards_since_last_state > 0:
            for rew in world_state.rewards:
                if rew.getValue()  == 0:
                    print("r", end="")
                elif rew.getValue()  == 100:
                    print("R", end="")
                elif rew.getValue()  == -1000:
                    print("*", end="")
                else:
                    print("?", end="")
                total_rewards += rew.getValue() 
        if world_state.is_mission_running:
            print("T", end="")
        else:
            print("F", end="")
        print(" ", end="")
    print()
    print("Mission Ended - sent " + str(commands_sent) + " commands; final reward: " + str(total_rewards))
    if total_rewards != 100:
        print("ERROR - FAILED TO GET CORRECT REWARD!")
        if total_rewards < 0:
            print("We overran! Quit producer did not produce a quit quickly enough!")
    print()

    if agent_host.receivedArgument("test"):
        if commands_sent != PATH_LENGTH or total_rewards != 100:
            print("Number of commands sent, or total rewards received, did not match expectations.")
            exit(1)
time.sleep(1)
