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

from builtins import range
import MalmoPython
import json
import math
import os
import random
import sys
import time
import errno
from timeit import default_timer as timer
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)

# Test that AbsoluteMovementCommand teleportation works for long distances.

WIDTH=860
HEIGHT=480

def genItems():
    items = ""
    for x in range(10):
        for z in range(10):
            items += '<DrawBlock x="' + str(x * 1000) + '" y="3" z="' + str(z * 1000) + '" type="redstone_block"/>'
            items += '<DrawItem x="' + str(x * 1000) + '" y="10" z="' + str(z * 1000) + '" type="emerald"/>'
    return items

def startMission(agent_host, xml):
    my_mission = MalmoPython.MissionSpec(xml, True)
    my_mission_record = malmoutils.get_default_recording_object(agent_host, "teleport_results")
    max_retries = 3
    for retry in range(max_retries):
        try:
            agent_host.startMission( my_mission, my_mission_record )
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print("Error starting mission",e)
                print("Is the game running?")
                exit(1)
            else:
                time.sleep(2)

    world_state = agent_host.peekWorldState()
    while not world_state.has_mission_begun:
        time.sleep(0.1)
        world_state = agent_host.peekWorldState()
        for error in world_state.errors:
            print("Error:",error.text)
        if len(world_state.errors) > 0:
            exit(1)

def processFrame(frame):
    '''Label each pixel as either red, green or blue, and return the percentages of each.'''
    red_total = 0
    green_total = 0
    blue_total = 0
    num_pixels = WIDTH * HEIGHT
    for pixel in range(0, num_pixels*3, 3):
        r = frame[pixel]
        g = frame[pixel+1]
        b = frame[pixel+2]

        # We're not worrying about cases where r==g, g==b, etc, since
        # the pixels are very obviously either red, green or blue.
        if r > g:
            if r > b:
                red_total += 1
            else:
                blue_total += 1
        else:
            if g > b:
                green_total += 1
            else:
                blue_total += 1

    red_total = int(100 * red_total / num_pixels)
    blue_total = int(100 * blue_total / num_pixels)
    green_total = int(100 * green_total / num_pixels)
    return red_total, green_total, blue_total

worldXML = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Teleportastic</Summary>
        </About>

        <ServerSection>
            <ServerInitialConditions>
                <Time>
                    <StartTime>1000</StartTime>
                    <AllowPassageOfTime>false</AllowPassageOfTime> <!-- Keep steady daylight to make image parsing simple -->
                </Time>
                <Weather>clear</Weather> <!-- Keep steady weather to make image parsing simple -->
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;;1;" forceReset="true" destroyAfterUse="true"/>
                <DrawingDecorator>''' + genItems() + '''</DrawingDecorator>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Brundlefly</Name>
            <AgentStart>
                <Placement x="-100.5" y="4" z="400.5" yaw="0" pitch="90"/>  <!-- Look down at the ground -->
                <Inventory/>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullInventory/>
                <AbsoluteMovementCommands/>
                <ContinuousMovementCommands/>
                <MissionQuitCommands/>
                <RewardForCollectingItem>
                    <Item type="emerald" reward="1"/>
                </RewardForCollectingItem>
                <VideoProducer>
                    <Width>''' + str(WIDTH) + '''</Width>
                    <Height>''' + str(HEIGHT) + '''</Height>
                </VideoProducer>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

startMission(agent_host, worldXML)
world_state = agent_host.peekWorldState()

# This test can get stuck, since Minecraft sometimes won't redraw the scene unless the agent moves slightly,
# and we don't move until Minecraft redraws the scene.
# To get around this, set a gentle rotation:
agent_host.sendCommand("turn 0.01")

# Teleport to each location in turn, see if we collect the right number of emeralds,
# and check we get the right image for each location.
total_reward = 0
for x in range(10):
    for z in range(10):
        teleport_x = x * 1000 + 0.5
        teleport_z = z * 1000 + 0.5
        tp_command = "tp " + str(teleport_x)+ " 4 " + str(teleport_z)
        print("Sending command: " + tp_command)
        agent_host.sendCommand(tp_command)
        # Hang around until the image stabilises and the reward is collected.
        # While the chunk is loading, everything will be blue.
        # Once the frame is rendering correctly, we should be looking at a field of grass
        # with a single redstone square in the middle.
        good_frame = False
        collected_reward = False
        start = timer()
        end_reward = None
        end_frame = None
        while not good_frame or not collected_reward:
            world_state = agent_host.getWorldState()
            if not world_state.is_mission_running:
                print("Mission ended prematurely - error.")
                exit(1)
            if not collected_reward and world_state.number_of_rewards_since_last_state > 0:
                total_reward += world_state.rewards[-1].getValue()
                print("Total reward: " + str(total_reward))
                collected_reward = True
                end_reward = timer()
            if not good_frame and world_state.number_of_video_frames_since_last_state > 0:
                frame_x = world_state.video_frames[-1].xPos
                frame_z = world_state.video_frames[-1].zPos
                if math.fabs(frame_x - teleport_x) < 0.001 and math.fabs(frame_z - teleport_z) < 0.001:
                    r,g,b = processFrame(world_state.video_frames[-1].pixels)
                    if b == 0:
                        good_frame = True
                        end_frame = timer()
        print("Took " + "{0:.2f}".format((end_frame - start) * 1000) + "ms to stabilise frame; " + "{0:.2f}".format((end_reward - start) * 1000) + "ms to collect reward.")

# Visited all the locations - quit the mission.
agent_host.sendCommand("quit")
while world_state.is_mission_running:
    world_state = agent_host.peekWorldState()

print("Teleport mission over.")
world_state = agent_host.getWorldState()
if world_state.number_of_rewards_since_last_state > 0:
    total_reward += world_state.rewards[-1].getValue()
if total_reward != 100:
    print("Got incorrect reward (" + str(total_reward) + ") - should have received 100 for collecting 100 emeralds.")
    exit(1)

print("Test successful")
exit(0)
