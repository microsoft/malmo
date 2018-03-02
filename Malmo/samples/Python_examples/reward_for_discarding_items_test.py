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

# Sample to demonstrate use of the RewardForDiscardingItem mission handler, and the DiscardCurrentItem command.
# Leaves a trail of bread-crumbs.

from builtins import range
import MalmoPython
import os
import random
import sys
import time
import json
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)

def GetMissionXML(summary, video_xml):
    ''' Build an XML mission string that uses the RewardForCollectingItem mission handler.'''
    
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>''' + summary + '''</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="-50" y1="226" z1="-50" x2="50" y2="226" z2="50" type="carpet" colour="RED" face="UP"/>
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="15000"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>The Full Caterpillar</Name>
            <AgentStart>
                <Placement x="0.5" y="227.0" z="0.5"/>
                <Inventory>
                    <InventoryItem slot="0" type="cookie" quantity="64"/>
                    <InventoryItem slot="1" type="fish" quantity="64"/>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <RewardForDiscardingItem>
                    <Item reward="-2" type="cookie"/>
                    <Item reward="10" type="fish"/>
                </RewardForDiscardingItem>
                <InventoryCommands/>
                <ChatCommands/>
                <ContinuousMovementCommands turnSpeedDegs="240"/>''' + video_xml + '''
            </AgentHandlers>
        </AgentSection>

    </Mission>'''
  

def SetVelocity(vel): 
    agent_host.sendCommand( "move " + str(vel) )

def SetTurn(turn):
    agent_host.sendCommand( "turn " + str(turn) )

validate = True
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
    my_mission = MalmoPython.MissionSpec(GetMissionXML("Let them eat fish/cookies #" + str(iRepeat + 1), malmoutils.get_video_xml(agent_host)),validate)
    # Set up a recording
    my_mission_record = malmoutils.get_default_recording_object(agent_host, "Mission_{}".format(iRepeat + 1))
    max_retries = 3
    for retry in range(max_retries):
        try:
            # Attempt to start the mission:
            agent_host.startMission( my_mission, my_client_pool, my_mission_record, 0, "itemDiscardTestExperiment" )
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
    turnCount = 0   # for keeping track of turn.
    discardTimer = 0
    # start running:
    agent_host.sendCommand("move 1")

    # main loop:
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_rewards_since_last_state > 0:
            # A reward signal has come in - see what it is:
            delta = world_state.rewards[0].getValue() 
            reward+=delta
            if delta==10:
                agent_host.sendCommand("chat " + random.choice(["Have a fish!", "Free trout!", "Fishy!", "Bleurgh, catch"]))
            elif delta==-2:
                agent_host.sendCommand("chat " + random.choice(["Cookies!", "Free cookies!", "Have a cookie.", "I'll just leave this here."]))

        if turnCount > 0:
            turnCount -= 1
            if turnCount == 0:
                agent_host.sendCommand("turn 0")
        
        if turnCount == 0 and random.random() > 0.8:
            agent_host.sendCommand("turn " + str(random.random() - 0.5))
            turnCount = random.randint(1,10)

        discardTimer+=1
        if discardTimer > 5:
            # Chuck an item:
            agent_host.sendCommand("discardCurrentItem")
            discardTimer = 0
            # And select the next item to chuck:
            hotbar="1" if (random.random() > 0.5) else "2"
            agent_host.sendCommand("hotbar." + hotbar + " 1")
            agent_host.sendCommand("hotbar." + hotbar + " 0")

        time.sleep(0.1)
        
    # mission has ended.
    print("Mission " + str(iRepeat+1) + ": Reward = " + str(reward))
    for error in world_state.errors:
        print("Error:",error.text)
    time.sleep(0.5) # Give the mod a little time to prepare for the next mission.
