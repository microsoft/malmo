# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------
# Basic test of multi-agent mission concept.
# To use:
# 1: Start two mods (make sure the ClientPool is set up to point to them, see below.)
# 2: Start agent one - eg "python multimaze.py"
# 3: Start agent two - eg "python multimaze --role 1"
# They should find each other and begin running missions.

import MalmoPython
import os
import random
import sys
import time
import json

def GetMissionXML( current_seed, xorg, yorg, zorg ):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ProjectMalmo.microsoft.com Mission.xsd">
        <About>
            <Summary>Run the maze!</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,198*1,5*3,2;3;,biome_1" />
                <MazeDecorator>
                    <SizeAndPosition length="32" width="32" xOrigin="''' + str(xorg) + '''" yOrigin="''' + str(yorg) + '''" zOrigin="''' + str(zorg) + '''" height="8"/>
                    <GapProbability variance="0.4">0.5</GapProbability>
                    <Seed>''' + str(current_seed) + '''</Seed>
                    <MaterialSeed>random</MaterialSeed>
                    <AllowDiagonalMovement>false</AllowDiagonalMovement>
                    <StartBlock fixedToEdge="true" type="emerald_block" height="1"/>
                    <EndBlock fixedToEdge="true" type="redstone_block" height="8"/>
                    <PathBlock type="glowstone stained_glass dirt" colour="WHITE ORANGE MAGENTA LIGHT_BLUE YELLOW LIME PINK GRAY SILVER CYAN PURPLE BLUE BROWN GREEN RED BLACK" height="1"/>
                    <FloorBlock type="stone"/>
                    <SubgoalBlock type="beacon sea_lantern glowstone"/>
                    <OptimalPathBlock type="dirt grass snow"/>
                    <GapBlock type="stained_hardened_clay lapis_ore sponge air" colour="WHITE ORANGE MAGENTA LIGHT_BLUE YELLOW LIME PINK GRAY SILVER CYAN PURPLE BLUE BROWN GREEN RED BLACK" height="3" heightVariance="3"/>
                    <Waypoints quantity="10">
                        <WaypointItem>cookie</WaypointItem>
                    </Waypoints>
                </MazeDecorator>
                <ServerQuitFromTimeUp timeLimitMs="60000"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Agnes</Name>
            <AgentStart>
                <Placement x="-204" y="81" z="217"/>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromMazeOptimalPath />
                <ObservationFromChat />
                <ContinuousMovementCommands turnSpeedDegs="840">
                    <ModifierList type="deny-list"> <!-- Example deny-list: prevent agent from strafing -->
                        <command>strafe</command>
                    </ModifierList>
                </ContinuousMovementCommands>
                <ChatCommands />
                <AgentQuitFromTouchingBlockType>
                    <Block type="redstone_block" />
                </AgentQuitFromTouchingBlockType>
            </AgentHandlers>
        </AgentSection>
        
        <AgentSection mode="Survival">
            <Name>Gertrude</Name>
            <AgentStart>
                <Placement x="-204" y="81" z="217"/>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromMazeOptimalPath />
                <ObservationFromChat />
                <ContinuousMovementCommands turnSpeedDegs="840">
                    <ModifierList type="deny-list"> <!-- Example deny-list: prevent agent from strafing -->
                        <command>strafe</command>
                    </ModifierList>
                </ContinuousMovementCommands>
                <ChatCommands />
                <AgentQuitFromTouchingBlockType>
                    <Block type="redstone_block" />
                </AgentQuitFromTouchingBlockType>
            </AgentHandlers>
        </AgentSection>
  </Mission>'''
  
sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
agent_host = MalmoPython.AgentHost()
agent_host.addOptionalIntArgument( "role,r", "For multi-agent missions, the role of this agent instance", 0)
try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print 'ERROR:',e
    print agent_host.getUsage()
    exit(1)
if agent_host.receivedArgument("help"):
    print agent_host.getUsage()
    exit(0)

role = agent_host.getIntArgument("role")
print "Will run as role",role

agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY)

# Create a client pool here - this assumes two local mods with default ports,
# but you could have two mods on different machines, and specify their IP address here.
client_pool = MalmoPython.ClientPool()
client_pool.add( MalmoPython.ClientInfo( "127.0.0.1", 10000 ) )
client_pool.add( MalmoPython.ClientInfo( "127.0.0.1", 10001 ) )

chat_frequency = 30 # if we send chat messages too frequently the agent will be disconnected for spamming
num_steps_since_last_chat = 0

for iRepeat in range(0, 30000):

    xorg = (iRepeat % 64) * 32
    zorg = ((iRepeat / 64) % 64) * 32
    yorg = 200 + ((iRepeat / (64*64)) % 64) * 8

    print "Mission " + str(iRepeat) + " --- starting at " + str(xorg) + ", " + str(yorg) + ", " + str(zorg)
    
    validate = True
    my_mission = MalmoPython.MissionSpec(GetMissionXML(iRepeat, xorg, yorg, zorg), validate)
    
    try:
        my_mission_record = MalmoPython.MissionRecordSpec()
        unique_experiment_id = "" # if needed, can be used to disambiguate multiple running copies of the same mission
        agent_host.startMission( my_mission, client_pool, my_mission_record, role, unique_experiment_id )
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

    # main loop:
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        while world_state.number_of_observations_since_last_state < 1 and world_state.is_mission_running:
            time.sleep(0.05)
            world_state = agent_host.getWorldState()

        if world_state.is_mission_running:
            msg = world_state.observations[0].text
            ob = json.loads(msg)
            current_yaw_delta = ob.get(u'yawDelta', 0)
            current_speed = 1-abs(current_yaw_delta)
            
            agent_host.sendCommand( "move " + str(current_speed) )
            agent_host.sendCommand( "turn " + str(current_yaw_delta) )
            if num_steps_since_last_chat >= chat_frequency:
                agent_host.sendCommand( "chat " + "hello from agent " + str(role) )
                num_steps_since_last_chat = 0
            else:
                num_steps_since_last_chat = num_steps_since_last_chat + 1

    print "Mission has stopped."

    print 'Sleeping to give the clients a chance to reset...',
    if role > 0:
        time.sleep(10)
    else:
        time.sleep(5)
    print 'done.'
