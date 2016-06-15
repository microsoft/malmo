# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------
# Tests overclocking the render speed by running a very simple task at a series of different frame sizes.

import MalmoPython
import os
import random
import sys
import time
import json
import errno
from timeit import default_timer as timer

MISSION_LENGTH=30

def GetMissionXML( width, height, prioritiseOffscreen ):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ProjectMalmo.microsoft.com Mission.xsd">
        <About>
            <Summary>Let's run! Size: ''' + width + ''' x ''' + height + '''</Summary>
        </About>

        <ModSettings>
            <PrioritiseOffscreenRendering>''' + prioritiseOffscreen + '''</PrioritiseOffscreenRendering>
        </ModSettings>

        <ServerSection>
            <ServerInitialConditions>
                <AllowSpawning>false</AllowSpawning>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="0" y1="226" z1="0" x2="0" y2="226" z2="1000" type="stone" variant="smooth_granite"/>
                    <DrawBlock x="0" y="226" z="130" type="emerald_block"/>
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="''' + str(MISSION_LENGTH * 1000) + '''"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Picasso</Name>
            <AgentStart>
                <Placement x="0" y="227" z="0"/>
            </AgentStart>
            <AgentHandlers>
                <ContinuousMovementCommands turnSpeedDegs="240"/>
                <ObservationFromDistance>
                    <Marker name="Start" x="0" y="227" z="0"/>
                </ObservationFromDistance>
                <AgentQuitFromTouchingBlockType>
                    <Block type="redstone_block" />
                </AgentQuitFromTouchingBlockType>
                <VideoProducer>
                    <Width>''' + width + '''</Width>
                    <Height>''' + height + '''</Height>
                </VideoProducer>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''
  
sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

validate = True
sizes = [(1024,768), (860,480), (640,256), (432,240), (400,400), (400,300), (320,240), (256,256), (224,144), (84,84), (80,80), (80,60)]

agent_host = MalmoPython.AgentHost()
agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY)

recordingsDirectory="Render_Speed_Test_Recordings"

try:
    os.makedirs(recordingsDirectory)
except OSError as exception:
    if exception.errno != errno.EEXIST: # ignore error if already existed
        raise

print "WELCOME TO THE RENDER SPEED TEST"
print "================================"
print "This will run the same simple mission with " + str(len(sizes)) + " different frame sizes."

for iRepeat in range(len(sizes) * 2):
    prioritiseOffscreen = "true" if iRepeat % 2 else "false"
    width,height = sizes[iRepeat/2]
    my_mission = MalmoPython.MissionSpec(GetMissionXML(str(width), str(height), prioritiseOffscreen), validate)
    # Set up a recording - MUST be done once for each mission - don't do this outside the loop!
    my_mission_record = MalmoPython.MissionRecordSpec(recordingsDirectory + "//RenderSpeed_Test" + str(iRepeat) + ".tgz");
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
    numFrames=0
    start = timer()

    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_video_frames_since_last_state > 0:
            numFrames = numFrames + world_state.number_of_video_frames_since_last_state

    end = timer()
    missionTimeMs = (end - start) * 1000
    dataShifted = (width * height * 3 * numFrames) / (1024*1024)
    
    print "==============================================================================================="
    print "Result of test " + str(iRepeat + 1) + ":"
    print "==============================================================================================="
    print "Frame size: " + str(width) + " x " + str(height)
    print "Priorising offscreen rendering: " + prioritiseOffscreen
    print "Frames received: " + str(numFrames)
    print "Average fps: " + "{0:.2f}".format(numFrames * 1000 / missionTimeMs)
    print "Frame data transferred: " + "{0:.2f}".format(dataShifted) + "MB"
    print "Data transfer rate: " + "{0:.2f}".format(dataShifted * 1000 / missionTimeMs) + "MB/s"
    print "==============================================================================================="
    print
    time.sleep(0.5) # Give mod a little time to get back to dormant state.