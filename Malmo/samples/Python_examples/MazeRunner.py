# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

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
            <Summary>Run the maze!</Summary>
        </About>

        <ServerSection>
            <ServerInitialConditions>
                <Time>
                    <StartTime>14000</StartTime>
                    <AllowPassageOfTime>false</AllowPassageOfTime>
                </Time>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <MazeDecorator>
                    <SizeAndPosition length="64" width="64" yOrigin="225" zOrigin="0" height="180"/>
                    <GapProbability variance="0.4">0.5</GapProbability>
                    <Seed>''' + str(current_seed) + '''</Seed>
                    <MaterialSeed>random</MaterialSeed>
                    <AllowDiagonalMovement>false</AllowDiagonalMovement>
                    <StartBlock fixedToEdge="true" type="emerald_block" height="1"/>
                    <EndBlock fixedToEdge="true" type="redstone_block" height="12"/>
                    <PathBlock type="glowstone stained_glass dirt" colour="WHITE ORANGE MAGENTA LIGHT_BLUE YELLOW LIME PINK GRAY SILVER CYAN PURPLE BLUE BROWN GREEN RED BLACK" height="1"/>
                    <FloorBlock type="air water lava"/>
                    <SubgoalBlock type="beacon sea_lantern glowstone tnt"/>
                    <OptimalPathBlock type="dirt grass snow tnt"/>
                    <GapBlock type="stained_hardened_clay lapis_ore sponge air" colour="WHITE ORANGE MAGENTA LIGHT_BLUE YELLOW LIME PINK GRAY SILVER CYAN PURPLE BLUE BROWN GREEN RED BLACK" height="3" heightVariance="3"/>
                </MazeDecorator>
                <ServerQuitFromTimeUp timeLimitMs="30000"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Creative">
            <Name>James Bond</Name>
            <AgentStart>
                <Placement x="-204" y="81" z="217"/>
                <Inventory>
                    <InventoryItem slot="1" type="diamond_pickaxe"/>
                    <InventoryBlock slot="2" type="tnt" quantity="32"/>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromMazeOptimalPath />
                <ContinuousMovementCommands turnSpeedDegs="840">
                    <ModifierList type="deny-list"> <!-- Example deny-list: prevent agent from strafing -->
                        <command>strafe</command>
                    </ModifierList>
                </ContinuousMovementCommands>
                <AgentQuitFromTouchingBlockType>
                    <Block type="redstone_block" />
                </AgentQuitFromTouchingBlockType>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''
  
sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

validate = True
my_mission = MalmoPython.MissionSpec(GetMissionXML("random"),validate)

agent_host = MalmoPython.AgentHost()
agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY)

for iRepeat in range(30000):

    try:
        my_mission_record = MalmoPython.MissionRecordSpec()
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
        if len(world_state.errors):
            print
            for error in world_state.errors:
                print "Error:",error.text
                exit()
    print

    # main loop:
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        while world_state.number_of_observations_since_last_state < 1 and world_state.is_mission_running:
            print "Waiting for observations..."
            time.sleep(0.05)
            world_state = agent_host.getWorldState()

        if world_state.is_mission_running:
            print "Got " + str(world_state.number_of_observations_since_last_state) + " observations since last state."
            msg = world_state.observations[0].text
            ob = json.loads(msg)
            current_yaw_delta = ob.get(u'yawDelta', 0)
            current_speed = 1-abs(current_yaw_delta)
            print "Got observation: " + str(current_yaw_delta)
            
            try:
                agent_host.sendCommand( "move " + str(current_speed) )
                agent_host.sendCommand( "turn " + str(current_yaw_delta) )
            except RuntimeError as e:
                print "Failed to send command:",e
                pass

    print "Mission has stopped."
    time.sleep(0.5) # Give mod a little time to get back to dormant state.
