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

# Sample to demonstrate use of RewardForMissionEnd, RewardForTouchingBlockType, and AgentQuitFromTouchingBlockType.
# Creates an arena with randomly positioned "mines".
# Quit handlers label their outcomes (via "description"), and these labels are associated with rewards via RewardForMissionEnd.

# Mission will end if the agent touches stained_glass, water, or redstone_block (specified in AgentQuitFromTouchingBlockType)
# These outcomes will give a reward of 100, -800 and 400 respectively (specified in RewardForMissionEnd)
# There is also a reward of -900 for running out of time (see ServerQuitFromTimeUp), and -1000 for dying.

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

def GetMissionXML():
    ''' Build an XML mission string that uses the RewardForCollectingItem mission handler.'''
    
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Nom nom nom</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="-21" y1="226" z1="-21" x2="21" y2="226" z2="21" type="stained_glass" colour="PINK"/>
                    <DrawCuboid x1="-20" y1="226" z1="-20" x2="20" y2="226" z2="20" type="emerald_block" />
                </DrawingDecorator>
                <DrawingDecorator>
                    ''' + GetMineDrawingXML() + '''
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="15000" description="out_of_time"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>The Hungry Caterpillar</Name>
            <AgentStart>
                <Placement x="0.5" y="227.0" z="0.5"/>
                <Inventory>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <VideoProducer>
                    <Width>640</Width>
                    <Height>480</Height>
                </VideoProducer>
                <RewardForMissionEnd rewardForDeath="-1000.0">
                    <Reward description="out_of_time" reward="-900.0"/>
                    <Reward description="out_of_arena" reward="100.0"/>
                    <Reward description="drowned" reward="-800.0"/>
                    <Reward description="found_goal" reward="400.0"/>
                </RewardForMissionEnd>
                <RewardForTouchingBlockType>
                    <Block type="obsidian" reward="100.0"/>
                </RewardForTouchingBlockType>
                <AgentQuitFromTouchingBlockType>
                    <Block type="stained_glass" description="out_of_arena"/>
                    <Block type="water" description="drowned"/>
                    <Block type="redstone_block" description="found_goal"/>
                </AgentQuitFromTouchingBlockType>
                <ContinuousMovementCommands turnSpeedDegs="240"/>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''
  
  
def GetMineDrawingXML():
    ''' Build an XML string that contains some randomly positioned "mines"'''
    xml=""
    for item in range(100):
        x = str(random.randint(-20,20))
        z = str(random.randint(-20,20))
        type = random.choice(["water", "lava", "redstone_block", "obsidian"])
        xml += '''<DrawBlock x="''' + x + '''" y="226" z="''' + z + '''" type="''' + type + '''"/>'''
    return xml

def SetVelocity(vel): 
    agent_host.sendCommand( "move " + str(vel) )

def SetTurn(turn):
    agent_host.sendCommand( "turn " + str(turn) )

validate = True
my_mission = MalmoPython.MissionSpec(GetMissionXML(),validate)

# Create a pool of Minecraft Mod clients.
# By default, mods will choose consecutive mission control ports, starting at 10000,
# so running four mods locally should produce the following pool by default (assuming nothing else
# is using these ports):
my_client_pool = MalmoPython.ClientPool()
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10000))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10001))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10002))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10003))

if agent_host.receivedArgument("test"):
    num_reps = 1
else:
    num_reps = 30000

for iRepeat in range(num_reps):
    # Set up a recording
    my_mission_record = malmoutils.get_default_recording_object(agent_host, "Mission_" + str(iRepeat + 1))
    max_retries = 3
    for retry in range(max_retries):
        try:
            # Attempt to start the mission:
            agent_host.startMission( my_mission, my_client_pool, my_mission_record, 0, "missionEndTestExperiment" )
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

    reward = 0.0    # keep track of reward for this mission.
    turncount = 0
    # start running:
    SetVelocity(1)

    # main loop:
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_rewards_since_last_state > 0:
            # A reward signal has come in - see what it is:
            delta = world_state.rewards[0].getValue()
            if delta != 0:
                print("New reward: " + str(delta))
                reward += delta

        if turncount > 0:
            turncount -= 1  # Decrement the turn count
            if turncount == 0:
                SetTurn(0)  # Stop turning
        elif random.random() < 0.2:
            SetTurn(random.random() - 0.5)
            turncount = random.randint(1,10)
        time.sleep(0.1)
        
    # mission has ended.
    print("Mission " + str(iRepeat+1) + ": Reward = " + str(reward))
    time.sleep(0.5) # Give the mod a little time to prepare for the next mission.
