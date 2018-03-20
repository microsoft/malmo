from __future__ import print_function
from __future__ import division
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

# Tests overclocking the render speed by running a very simple task at a series of different frame sizes.

from builtins import range
from past.utils import old_div
import MalmoPython
import os
import random
import sys
import time
import json
import errno
from timeit import default_timer as timer
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
agent_host.addOptionalFlag("plot,p", "Plot the results of the render speed tests")
malmoutils.parse_command_line(agent_host)

def GetMissionXML( width, height, prioritiseOffscreen ):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Let's run! Size: ''' + width + ''' x ''' + height + '''</Summary>
        </About>

        <ModSettings>
            <PrioritiseOffscreenRendering>''' + prioritiseOffscreen + '''</PrioritiseOffscreenRendering>
        </ModSettings>

        <ServerSection>
            <ServerInitialConditions>
                <AllowSpawning>false</AllowSpawning>
                <Time>
                    <StartTime>1000</StartTime>
                    <AllowPassageOfTime>false</AllowPassageOfTime>
                </Time>
                <Weather>clear</Weather>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,121;3;biome_1" />
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
                <Placement x="0.5" y="227.0" z="0.5"/>
            </AgentStart>
            <AgentHandlers>
                <ContinuousMovementCommands turnSpeedDegs="240"/>
                <ObservationFromDistance>
                    <Marker name="Start" x="0.5" y="227.0" z="0.5"/>
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

TESTING = agent_host.receivedArgument("test")
SHOW_PLOT = TESTING or agent_host.receivedArgument("plot")

if TESTING:
    MISSION_LENGTH=5
else:
    MISSION_LENGTH=10

if SHOW_PLOT:
    import matplotlib
    import numpy
    import pylab

validate = True
sizes = [(1920,1200), (1280, 920), (1024,768), (860,480), (640,256), (400,400), (400,300), (432,240), (320,240), (256,256), (224,144), (84,84), (80,80), (80,60)]

num_pixels=[]
fps_offscreen=[]
fps_onscreen=[]
datarate_offscreen=[]
datarate_onscreen=[]

agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY)

print("WELCOME TO THE RENDER SPEED TEST")
print("================================")
print("This will run the same simple mission with " + str(len(sizes)) + " different frame sizes.")

for iRepeat in range(len(sizes) * 2):
    prioritiseOffscreen = "true" if iRepeat % 2 else "false"
    width,height = sizes[old_div(iRepeat,2)]
    if iRepeat % 2:
        num_pixels.append(width*height)
    my_mission = MalmoPython.MissionSpec(GetMissionXML(str(width), str(height), prioritiseOffscreen), validate)
    # Set up a recording
    my_mission_record = malmoutils.get_default_recording_object(agent_host, "RenderSpeed_Test" + str(iRepeat));
    max_retries = 3
    for retry in range(max_retries):
        try:
            agent_host.startMission( my_mission, my_mission_record )
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print("Error starting mission:",e)
                exit(1)
            else:
                time.sleep(2)

    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
        if len(world_state.errors):
            print()
            for error in world_state.errors:
                print("Error:",error.text)
                exit()
    print()

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
    dataShifted = old_div((width * height * 3 * numFrames), (1024*1024))
    averagefps = numFrames * 1000 / missionTimeMs
    datarate = dataShifted * 1000 / missionTimeMs
    
    print("===============================================================================================")
    print("Result of test " + str(iRepeat + 1) + ":")
    print("===============================================================================================")
    print("Frame size: " + str(width) + " x " + str(height))
    print("Prioritising offscreen rendering: " + prioritiseOffscreen)
    print("Frames received: " + str(numFrames))
    print("Average fps: " + "{0:.2f}".format(averagefps))
    print("Frame data transferred: " + "{0:.2f}".format(dataShifted) + "MB")
    print("Data transfer rate: " + "{0:.2f}".format(datarate) + "MB/s")
    print("===============================================================================================")
    print()

    if iRepeat % 2:
        fps_offscreen.append(averagefps)
        datarate_offscreen.append(datarate)
    else:
        fps_onscreen.append(averagefps)
        datarate_onscreen.append(datarate)
        
    time.sleep(0.5) # Give mod a little time to get back to dormant state.

if SHOW_PLOT:
    # Now plot some graphs:
    plot_fpsoff = pylab.plot(num_pixels, fps_offscreen, 'r', label='render speed (no onscreen updates)')
    plot_fpson = pylab.plot(num_pixels, fps_onscreen, 'g', label='render speed (with onscreen updates)')
    plot_dataoff = pylab.plot(num_pixels, datarate_offscreen, 'b', label='data transfer speed (no onscreen updates)')
    plot_dataon = pylab.plot(num_pixels, datarate_onscreen, 'y', label='data transfer speed (with onscreen updates)')
    pylab.xlabel("Frame size (pixels)")
    pylab.ylabel("MB/s or frames/s")
    pylab.legend()
    pylab.title("Plot of render and data-transfer speeds for varying frame sizes, with and without onscreen rendering")
    if TESTING:
        pylab.savefig(malmoutils.get_recordings_directory(agent_host) + "//render_test_results.png")
    else:
        pylab.show()
