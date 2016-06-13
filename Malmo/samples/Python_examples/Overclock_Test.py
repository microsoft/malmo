# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------
# Tests overclocking by running a very simple task at a series of different speeds.
# How to interpret results:

# If the distance travelled INCREASES with speed then it's likely that the SERVER overclocking has failed
# but the CLIENT overclocking hasn't (eg client can travel 5x faster but server is still counting down in wallclock time)

# If the distance travelled DECREASES with speed then it's likely that the SERVER overclocking has worked,
# but the CLIENT overclocking hasn't (eg server is counting down faster, but client is moving the same speed)

# If the distance travelled stays roughly the same then either overclocking has worked on both client and server...
# or it's not worked on either - which would be pretty obvious from the other statistics (especially wall-time length).

# If any such errors occur, the most likely explanation will be that the Forge or Minecraft versions have changed,
# in which case the OverclockingClassTransformer will need updating (server), or TimeHelper.setMinecraftClientClockSpeed
# will need updating (client).

# This test can also give a rough indication of suitable tick rates for your system - if the actual and desired
# results stay fairly close to each other, then that setting is a viable one. (Eg on my system, a requested tick length
# of 2ms results in an actual tick length of 4.8ms, which means attempting to run Minecraft at 25 times normal speed is
# a no-no, though 5x or even 10x is feasible.)

import MalmoPython
import os
import random
import sys
import time
import json
import errno
from timeit import default_timer as timer

MISSION_LENGTH=30

def GetMissionXML( msPerTick ):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ProjectMalmo.microsoft.com Mission.xsd">
        <About>
            <Summary>How fast can we run?</Summary>
        </About>

        <ServerSection>
            <ServerInitialConditions>
                <AllowSpawning>false</AllowSpawning>
                <Time>
                    <MsPerTick>''' + msPerTick + '''</MsPerTick>
                </Time>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <MazeDecorator>
                    <SizeAndPosition length="200" width="1" yOrigin="225" zOrigin="0" xOrigin="0" height="180"/>
                    <GapProbability>0.0</GapProbability>
                    <Seed>random</Seed>
                    <MaterialSeed>random</MaterialSeed>
                    <AllowDiagonalMovement>false</AllowDiagonalMovement>
                    <StartBlock fixedToEdge="true" type="emerald_block" height="1"/>
                    <EndBlock fixedToEdge="true" type="redstone_block" height="12"/>
                    <PathBlock type="stone" variant="smooth_granite"/>
                    <FloorBlock type="stone" variant="smooth_granite"/>
                    <GapBlock type="air"/>
                </MazeDecorator>
                <ServerQuitFromTimeUp timeLimitMs="''' + str(MISSION_LENGTH * 1000) + '''"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Eric Liddell</Name>
            <AgentStart>
                <Placement x="-204" y="81" z="217"/>
            </AgentStart>
            <AgentHandlers>
                <ContinuousMovementCommands turnSpeedDegs="240"/>
                <ObservationFromDistance>
                    <Marker name="Start" x="0" y="225" z="0"/>
                </ObservationFromDistance>
                <AgentQuitFromTouchingBlockType>
                    <Block type="redstone_block" />
                </AgentQuitFromTouchingBlockType>
                <VideoProducer>
                    <Width>860</Width>
                    <Height>480</Height>
                </VideoProducer>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''
  
sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

validate = True
tickLengths = [100, 50, 40, 30, 20, 25, 10, 5, 2]

agent_host = MalmoPython.AgentHost()
agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY)

recordingsDirectory="Overclock_Test_Recordings"

try:
    os.makedirs(recordingsDirectory)
except OSError as exception:
    if exception.errno != errno.EEXIST: # ignore error if already existed
        raise

for iRepeat in range(len(tickLengths)):
    msPerTick = tickLengths[iRepeat]
    my_mission = MalmoPython.MissionSpec(GetMissionXML(str(msPerTick)),validate)
    # Set up a recording - MUST be done once for each mission - don't do this outside the loop!
    my_mission_record = MalmoPython.MissionRecordSpec("Overclock_Test" + str(iRepeat) + ".tgz");
    my_mission_record.recordRewards()
    my_mission_record.recordObservations()
    my_mission_record.recordMP4(120,1200000) # Attempt to record at 120fps
    try:
        agent_host.startMission( my_mission, my_mission_record )
    except RuntimeError as e:
        print "Error starting mission:",e
        exit(1)

    world_state = agent_host.getWorldState()
    while not world_state.is_mission_running:
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
        if len(world_state.errors):
            print
            for error in world_state.errors:
                print "Error:",error.text
                exit()
    print

    # main loop:
    agent_host.sendCommand("move 1")    # just go forwards, max speed.
    numObs=0
    distance=0
    numFrames=0
    start = timer()

    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_observations_since_last_state > 0:
            numObs = numObs + world_state.number_of_observations_since_last_state
            msg = world_state.observations[-1].text
            ob = json.loads(msg)
            distance = ob.get(u'distanceFromStart', 0)
        if world_state.number_of_video_frames_since_last_state > 0:
            numFrames = numFrames + world_state.number_of_video_frames_since_last_state

    end = timer()
    missionTimeMs = (end - start) * 1000

    actual_mspertick = missionTimeMs / numObs
    desired_distance = 4.317 * MISSION_LENGTH   # normal walking speed is around 4.317 m/s
    desired_walltime = MISSION_LENGTH * msPerTick / 50  # normal ticklength is 50ms
    desired_observations = MISSION_LENGTH * 20  # default is 20hz

    print "==============================================================================================="
    print "Result of test " + str(iRepeat + 1) + ":"
    print "==============================================================================================="
    print "\t\tDesired\t|\tActual"
    print "MsPerTick:\t" + str(msPerTick) + "\t|\t" + "{0:.2f}".format(actual_mspertick)
    print "Obs:\t\t" + "{0:.2f}".format(desired_observations) + "\t|\t" + str(numObs)
    print "Distance:\tc" + "{0:.2f}".format(desired_distance) + "\t|\t" + "{0:.2f}".format(distance)
    print "Frames:\t\t???\t|\t" + str(numFrames)
    print "Wall time:\t" + str(desired_walltime) + "s\t|\t" + "{0:.2f}".format(missionTimeMs/1000) + "s"
    print "==============================================================================================="
    print
    time.sleep(0.5) # Give mod a little time to get back to dormant state.
   