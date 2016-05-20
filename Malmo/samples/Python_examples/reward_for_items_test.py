# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------
# Sample to demonstrate use of RewardForCollectingItem mission handler - creates a map with randomly distributed food items, each of which
# gives the agent a certain reward. Agent runs around collecting items, and turns left or right depending on the detected reward.

import MalmoPython
import os
import random
import sys
import time
import json
import random
import errno

def GetMissionXML(summary, itemDrawingXML):
    ''' Build an XML mission string that uses the RewardForCollectingItem mission handler.'''
    
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ProjectMalmo.microsoft.com Mission.xsd">
        <About>
            <Summary>''' + summary + '''</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="-50" y1="226" z1="-50" x2="50" y2="226" z2="50" type="carpet" colour="RED" face="UP"/>
                    ''' + itemDrawingXML + '''
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="15000"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>The Hungry Caterpillar</Name>
            <AgentStart>
                <Placement x="0" y="227" z="0"/>
                <Inventory>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <VideoProducer>
                    <Width>480</Width>
                    <Height>320</Height>
                </VideoProducer>
                <RewardForCollectingItem>
                    <Item reward="2" type="fish porkchop beef chicken rabbit mutton"/>
                    <Item reward="1" type="potato egg carrot"/>
                    <Item reward="-1" type="apple melon"/>
                    <Item reward="-2" type="sugar cake cookie pumpkin_pie"/>
                </RewardForCollectingItem>
                <ContinuousMovementCommands turnSpeedDegs="240"/>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''
  
  
def GetItemDrawingXML():
    ''' Build an XML string that contains 400 randomly positioned bits of food'''
    xml=""
    for item in range(400):
        x = str(random.randint(-50,50))
        z = str(random.randint(-50,50))
        type = random.choice(["sugar", "cake", "cookie", "pumpkin_pie", "fish", "porkchop", "beef", "chicken", "rabbit", "mutton", "potato", "egg", "carrot", "apple", "melon"])
        xml += '''<DrawItem x="''' + x + '''" y="250" z="''' + z + '''" type="''' + type + '''"/>'''
    return xml

def SetVelocity(vel): 
    try:
        agent_host.sendCommand( "move " + str(vel) )
    except RuntimeError as e:
        print "Failed to send command:",e
        pass

def SetTurn(turn):
    try:
        agent_host.sendCommand( "turn " + str(turn) )
    except RuntimeError as e:
        print "Failed to send command:",e
        pass

recordingsDirectory="EatingRecordings"
try:
    os.makedirs(recordingsDirectory)
except OSError as exception:
    if exception.errno != errno.EEXIST: # ignore error if already existed
        raise

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

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

agent_host = MalmoPython.AgentHost()
itemdrawingxml = GetItemDrawingXML()

for iRepeat in range(30000):
    my_mission = MalmoPython.MissionSpec(GetMissionXML("Nom nom nom run #" + str(iRepeat), itemdrawingxml),validate)
    launchedMission=False
    while not launchedMission:
        try:
            # Set up a recording - MUST be done once for each mission - don't do this outside the loop!
            my_mission_record = MalmoPython.MissionRecordSpec(recordingsDirectory + "//" + "Mission_" + str(iRepeat) + ".tgz")
            my_mission_record.recordRewards()
            my_mission_record.recordMP4(24,400000)
            # And attempt to start the mission:
            agent_host.startMission( my_mission, my_client_pool, my_mission_record, 0, "itemTestExperiment" )
            launchedMission=True
        except RuntimeError as e:
            print "Error starting mission",e
            print "Is the game running?"
            exit(1)

    world_state = agent_host.getWorldState()
    while not world_state.is_mission_running:
        time.sleep(0.1)
        world_state = agent_host.getWorldState()

    reward = 0.0    # keep track of reward for this mission.
    turncount = 0   # for counting turn time.
    # start running:
    SetVelocity(1)

    # main loop:
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_rewards_since_last_state > 0:
            # A reward signal has come in - see what it is:
            delta = world_state.rewards[0].value
            if delta != 0:
                # The total reward has changed - use this to determine our turn.
                reward += delta
                turncount = delta
                if turncount < 0:   # Turn left
                    turncount = 1-turncount
                    SetTurn(-1)     # Start turning
                else:               # Turn right
                    turncount = 1+turncount
                    SetTurn(1)      # Start turning
        if turncount > 0:
            turncount -= 1  # Decrement the turn count
            if turncount == 0:
                SetTurn(0)  # Stop turning
        time.sleep(0.1)
        
    # mission has ended.
    print "Mission " + str(iRepeat+1) + ": Reward = " + str(reward)
    for error in world_state.errors:
        print "Error:",error.text
    time.sleep(0.5) # Give the mod a little time to prepare for the next mission.