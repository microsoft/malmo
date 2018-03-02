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

# Sample to demonstrate use of MovingTargetDecorator.
# Creates two moving targets - one which moves as fast as possible, and one which is turn-based, and
# will wait for the agent to take its turn.

from builtins import input
from builtins import range
import MalmoPython
import os
import random
import sys
import time
import json
import random
import errno
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)

def GetMissionXML(summary):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>''' + summary + '''</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="-50" y1="226" z1="-50" x2="50" y2="227" z2="50" type="lapis_block"/>
                </DrawingDecorator>
                <MovingTargetDecorator>
                    <ArenaBounds>
                       <min x="-50" y="226" z="-50"/>
                       <max x="50" y="226" z="50"/>
                    </ArenaBounds>
                    <StartPos x="3" y="226" z="0"/>
                    <Seed>random</Seed>
                    <UpdateSpeed>turnbased</UpdateSpeed>
                    <PermeableBlocks type="air obsidian"/>
                    <BlockType type="glowstone"/>
                </MovingTargetDecorator>
                <MovingTargetDecorator>
                    <ArenaBounds>
                       <min x="-50" y="226" z="-50"/>
                       <max x="50" y="226" z="50"/>
                    </ArenaBounds>
                    <StartPos x="-3" y="226" z="0"/>
                    <Seed>random</Seed>
                    <UpdateSpeed>1</UpdateSpeed>
                    <PermeableBlocks type="air obsidian"/>
                    <BlockType type="beacon"/>
                </MovingTargetDecorator>
                <MazeDecorator>
                    <SizeAndPosition length="20" width="20" yOrigin="226" zOrigin="-10" xOrigin="-10" height="20"/>
                    <GapProbability>0.1</GapProbability>
                    <Seed>random</Seed>
                    <MaterialSeed>random</MaterialSeed>
                    <AllowDiagonalMovement>false</AllowDiagonalMovement>
                    <StartBlock fixedToEdge="false" type="emerald_block" height="0"/>
                    <EndBlock fixedToEdge="false" type="redstone_block" height="0"/>
                    <PathBlock type="obsidian" height="0"/>
                    <FloorBlock type="obsidian"/>
                    <GapBlock type="stained_hardened_clay" height="1"/>
                </MazeDecorator>
                <ServerQuitFromTimeUp timeLimitMs="150000"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Creative">
            <Name>Chevy</Name>
            <AgentStart>
                <Placement x="0.5" y="227.0" z="0.5"/>
            </AgentStart>
            <AgentHandlers>
                <TurnBasedCommands requestedPosition="1">
                    <DiscreteMovementCommands/>
                </TurnBasedCommands>
                <AgentQuitFromTouchingBlockType>
                    <Block type="glowstone"/>
                </AgentQuitFromTouchingBlockType>''' + malmoutils.get_video_xml(agent_host) + '''
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

validate = True
my_client_pool = MalmoPython.ClientPool()
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10000))

if agent_host.receivedArgument("test"):
    num_reps = 1
else:
    num_reps = 30000

for iRepeat in range(num_reps):
    my_mission = MalmoPython.MissionSpec(GetMissionXML("Moving target #" + str(iRepeat)),validate)
    my_mission_record = malmoutils.get_default_recording_object(agent_host, "Mission_" + str(iRepeat))
    max_retries = 3
    for retry in range(max_retries):
        try:
            # Attempt to start the mission:
            agent_host.startMission( my_mission, my_client_pool, my_mission_record, 0, "movingTargetTestExperiment" )
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print("Error starting mission",e)
                print("Is the game running?")
                exit(1)
            else:
                time.sleep(2)

    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        time.sleep(0.1)
        world_state = agent_host.getWorldState()

    # main loop:
    turn_key = ""
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_observations_since_last_state > 0:
            msg = world_state.observations[-1].text
            ob = json.loads(msg)
            new_turn_key = ob.get(u'turn_key', "")
            turn_index = ob.get(u'turn_number',0)
            if len(new_turn_key) > 0 and new_turn_key != turn_key:
                if agent_host.receivedArgument("test"):
                    nb = random.choice(["movenorth","movesouth","moveeast","movewest"])
                else:
                    nb = input('Enter command: ')
                agent_host.sendCommand(nb, str(new_turn_key))
                turn_key = new_turn_key

    # mission has ended.
    time.sleep(0.5) # Give the mod a little time to prepare for the next mission.
