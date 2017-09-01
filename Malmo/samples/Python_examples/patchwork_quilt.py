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

# Stress test of the maze decorator and mission lifecycle - populates the playing arean with 30,000 small (16x16) mazes,
# one at a time, and runs each mission for 1 second, recording commands and video.

import MalmoPython
import os
import errno
import random
import sys
import time
import json
import uuid

def GetMissionXML( current_seed, xorg, yorg, zorg, iteration ):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Tiny Maze #''' + str(iteration) + '''</Summary>
        </About>

        <ServerSection>
            <ServerInitialConditions>
                <Time>
                    <StartTime>14000</StartTime>
                    <AllowPassageOfTime>true</AllowPassageOfTime>
                </Time>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <MazeDecorator>
                    <SizeAndPosition length="16" width="16" xOrigin="''' + str(xorg) + '''" yOrigin="''' + str(yorg) + '''" zOrigin="''' + str(zorg) + '''" height="8"/>
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
                        <WaypointItem type="cookie"/>
                    </Waypoints>
                    <AddNavigationObservations/>
                </MazeDecorator>
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>James Bond</Name>
            <AgentStart>
                <Placement x="-203.5" y="81.0" z="217.5"/>
            </AgentStart>
            <AgentHandlers>
                <ContinuousMovementCommands turnSpeedDegs="840">
                    <ModifierList type="deny-list"> <!-- Example deny-list: prevent agent from strafing -->
                        <command>strafe</command>
                    </ModifierList>
                </ContinuousMovementCommands>
                <VideoProducer>
                    <Width>320</Width>
                    <Height>240</Height>
                </VideoProducer>
                <AgentQuitFromTouchingBlockType>
                    <Block type="redstone_block" />
                </AgentQuitFromTouchingBlockType>
                <AgentQuitFromTimeUp timeLimitMs="1000"/>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
validate = True
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

# Create a pool of Minecraft Mod clients:
my_client_pool = MalmoPython.ClientPool()
# Add the default client - port 10000 on the local machine:
my_client = MalmoPython.ClientInfo("127.0.0.1", 10000)
my_client_pool.add(my_client)
# Add extra clients here:
# eg my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10001)) etc

# Create a unique identifier - different each time this script is run.
# In multi-agent missions all agents must pass the same experimentID, in order to prevent agents from joining the wrong experiments.
experimentID = uuid.uuid4()

# Create a folder to put our recordings - the platform will not create missing folders itself, it will simply throw an exception.
recordingsDirectory="QuiltRecordings"
try:
    os.makedirs(recordingsDirectory)
except OSError as exception:
    if exception.errno != errno.EEXIST: # ignore error if already existed
        raise

if agent_host.receivedArgument("test"):
    num_reps = 1
else:
    num_reps = 30000

# Set up a recording
my_mission_record = MalmoPython.MissionRecordSpec()
my_mission_record.recordCommands()
my_mission_record.recordMP4(24,400000)

for iRepeat in range(num_reps):
    # Find the point at which to create the maze:
    xorg = (iRepeat % 64) * 16
    zorg = ((iRepeat / 64) % 64) * 16
    yorg = 200 + ((iRepeat / (64*64)) % 64) * 8

    print "Mission " + str(iRepeat) + " --- starting at " + str(xorg) + ", " + str(yorg) + ", " + str(zorg)

    # Create a mission:
    my_mission = MalmoPython.MissionSpec(GetMissionXML(iRepeat, xorg, yorg, zorg, iRepeat), validate)
    
    # Give the recording a destination filename:
    my_mission_record.setDestination(recordingsDirectory + "//" + "Quilt_" + str(iRepeat) + ".tgz")
    max_retries = 3
    for retry in range(max_retries):
        try:
            # Attempt to start the mission:
            agent_host.startMission( my_mission, my_client_pool, my_mission_record, 0, str(experimentID) )
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print "Error starting mission",e
                exit(1)
            else:
                time.sleep(2)

    print "Waiting for the mission to start",
    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
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

    print "Mission has stopped."
    time.sleep(0.5)  # Short pause to allow the Mod to get ready for the next mission.