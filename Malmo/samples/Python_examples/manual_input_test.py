# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------
# Manual test of the command handlers.
# Creates a small maze, then allows the user to type commands directly to control the agent.
# A full list of the commands available can be found in the MissionHandlers.xsd - Schemas/MissionHndlers.html
# eg typing "tpy 255" will teleport the agent to a y-position of 255 (and then let him plummet to his death).
# typing "turn 0.5" will begin the agent spinning on the spot, etc.

import MalmoPython
import os
import random
import sys
import time
import json

def GetMissionXML( current_seed ):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ProjectMalmo.microsoft.com Mission.xsd">
  
    <About>
        <Summary>Manual input test</Summary>
    </About>
     
    <ServerSection>
        <ServerHandlers>
            <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
            <MazeDecorator>
                <SizeAndPosition length="64" width="64" yOrigin="215" zOrigin="0" height="180"/>
                <GapProbability variance="0.4">0.5</GapProbability>
                <Seed>''' + str(current_seed) + '''</Seed>
                <MaterialSeed>random</MaterialSeed>
                <AllowDiagonalMovement>false</AllowDiagonalMovement>
                <StartBlock fixedToEdge="true" type="emerald_block" height="1"/>
                <EndBlock fixedToEdge="true" type="redstone_block" height="12"/>
                <PathBlock type="glowstone stained_glass dirt" colour="WHITE ORANGE MAGENTA LIGHT_BLUE YELLOW LIME PINK GRAY SILVER CYAN PURPLE BLUE BROWN GREEN RED BLACK" height="1"/>
                <FloorBlock type="air water lava"/>
                <SubgoalBlock type="beacon sea_lantern glowstone"/>
                <GapBlock type="stained_hardened_clay lapis_ore sponge air" colour="WHITE ORANGE MAGENTA LIGHT_BLUE YELLOW LIME PINK GRAY SILVER CYAN PURPLE BLUE BROWN GREEN RED BLACK" height="3" heightVariance="3"/>
            </MazeDecorator>
            <ServerQuitWhenAnyAgentFinishes />
        </ServerHandlers>
    </ServerSection>

    <AgentSection>
        <Name>James Bond</Name>
        <AgentStart>
            <Placement x="-204" y="81" z="217"/> 
        </AgentStart>
        <AgentHandlers>
            <VideoProducer>
                <Width>320</Width>
                <Height>240</Height>
            </VideoProducer>
            <ContinuousMovementCommands />
            <AbsoluteMovementCommands />
            <DiscreteMovementCommands />
            <InventoryCommands />
            <AgentQuitFromTouchingBlockType>
                <Block type="redstone_block"/>
            </AgentQuitFromTouchingBlockType>
        </AgentHandlers>
    </AgentSection>

  </Mission>'''
  

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

validate = True
my_mission = MalmoPython.MissionSpec(GetMissionXML("random"),validate)
my_mission.observeRecentCommands()

agent_host = MalmoPython.AgentHost()
try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print 'ERROR:',e
    print agent_host.getUsage()
    exit(1)
if agent_host.receivedArgument("help"):
    print agent_host.getUsage()
    exit(0)

agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY)

my_mission_record = MalmoPython.MissionRecordSpec()

try:
    agent_host.startMission( my_mission, my_mission_record )
except RuntimeError as e:
    print "Error starting mission:",e
    exit(1)

print "Waiting for the mission to start",
world_state = agent_host.getWorldState()
while not world_state.is_mission_running:
    sys.stdout.write(".")
    time.sleep(0.1)
    world_state = agent_host.getWorldState()
print

if agent_host.receivedArgument("test"):
    exit(0)

# main loop:
while world_state.is_mission_running:
    nb = raw_input('Enter command: ')
    agent_host.sendCommand(nb)
    world_state = agent_host.getWorldState()

print "Mission has stopped."
