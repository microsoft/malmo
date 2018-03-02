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

# Tests overclocking by running a very simple task at a series of different speeds,
# and offers some basic evaluation of the results.
# The results will be noisy, but may give some indication of sensible operational parameters.
# For more relevant results, tailor the mission xml to reflect your actual needs (eg set the video to the
# actual size you are after, add whatever observation producers you will be using, etc.)

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
malmoutils.parse_command_line(agent_host)

MISSION_LENGTH=30
FRAME_WIDTH=432
FRAME_HEIGHT=240

def GetMissionXML( msPerTick ):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Let's run! Current tick length: ''' + msPerTick + '''ms</Summary>
        </About>

        <ModSettings>
            <MsPerTick>''' + msPerTick + '''</MsPerTick>
        </ModSettings>

        <ServerSection>
            <ServerInitialConditions>
                <AllowSpawning>false</AllowSpawning>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,159:14;3;biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="0" y1="226" z1="0" x2="0" y2="226" z2="1000" type="stone" variant="smooth_granite"/>
                    <DrawBlock x="0" y="226" z="130" type="emerald_block"/>
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="''' + str(MISSION_LENGTH * 1000) + '''"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Eric Liddell</Name>
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
                    <Width>''' + str(FRAME_WIDTH) + '''</Width>
                    <Height>''' + str(FRAME_HEIGHT) + '''</Height>
                </VideoProducer>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

validate = True
tickLengths = [100, 50, 40, 30, 25, 20, 15, 10, 8, 5, 4, 2]
timeTest = []
obsTest = []
frameTest = []
wallclockTimes = []
distances = []

agent_host.setObservationsPolicy(MalmoPython.ObservationsPolicy.LATEST_OBSERVATION_ONLY)

print("WELCOME TO THE OVERCLOCK TEST")
print("=============================")
print("This will run the same simple mission with " + str(len(tickLengths)) + " different tick lengths.")
print("(Each test should run faster than the previous one.)")

for iRepeat in range(len(tickLengths)):
    msPerTick = tickLengths[iRepeat]
    my_mission = MalmoPython.MissionSpec(GetMissionXML(str(msPerTick)),validate)
    # Set up a recording
    my_mission_record = malmoutils.get_default_recording_object(agent_host, "Overclock_Test_" + str(msPerTick) + "ms_per_tick");
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

    actual_mspertick = old_div(missionTimeMs, numObs)
    desired_distance = 4.317 * MISSION_LENGTH   # normal walking speed is around 4.317 m/s
    desired_walltime = MISSION_LENGTH * msPerTick / 50  # normal ticklength is 50ms
    desired_observations = MISSION_LENGTH * 20  # default is 20hz

    print("===============================================================================================")
    print("Result of test " + str(iRepeat + 1) + ":")
    print("===============================================================================================")
    print("\t\tDesired\t|\tActual")
    print("MsPerTick:\t" + str(msPerTick) + "\t|\t" + "{0:.2f}".format(actual_mspertick))
    print("Obs:\t\t" + "{0:.2f}".format(desired_observations) + "\t|\t" + str(numObs))
    print("Distance:\tc" + "{0:.2f}".format(desired_distance) + "\t|\t" + "{0:.2f}".format(distance))
    print("Frames:\t\t???\t|\t" + str(numFrames))
    print("Wall time:\t" + str(desired_walltime) + "s\t|\t" + "{0:.2f}".format(old_div(missionTimeMs,1000)) + "s")
    print("===============================================================================================")
    print()
    time.sleep(0.5) # Give mod a little time to get back to dormant state.

    timeTest.append(True if abs(desired_walltime-old_div(missionTimeMs,1000)) < 1 else False)
    obsTest.append(True if abs(desired_observations-numObs) < 20 else False)
    frameTest.append(True if numFrames >= numObs else False)
    wallclockTimes.append(old_div(missionTimeMs,1000))
    distances.append(distance)

# Do some interpretation on the results:
# First off, simple test to see if server overclocking has worked:
t = old_div(sum([(MISSION_LENGTH-v)*(MISSION_LENGTH-v) for v in wallclockTimes]),len(wallclockTimes))
if t < 20:
    print("ERROR: All missions seemed to take around " + str(MISSION_LENGTH) + " seconds.")
    print("This indicates that the server tick length hasn't changed at all.")
    print("In other words, OVERCLOCKING IS NOT WORKING ON YOUR SYSTEM.")
    print()
    print("Possible causes:")
    print("\tIf running Minecraft from Eclipse, make sure you have the following line in your VM arguments:")
    print("\t\t-Dfml.coreMods.load=com.microsoft.Malmo.OverclockingPlugin")
    print("\tOtherwise, it's possible that you are using a different Minecraft build or Forge build to that")
    print("\twhich the coremod expects.")
    print("\tOverclockingClassTransformer may need updating.")
    print()
    exit(1) # Other tests will be meaningless if overclocking has failed completely.

# Test to see whether client overclocking has worked.
# If it hasn't, the distance travelled will decrease as the server ticks get faster.
# Only do this test where the observation tests were good!
error = 0
count = 0
for index in range(0, len(tickLengths)):
    if obsTest[index]:
        error+=(desired_distance-distances[index])*(desired_distance-distances[index])
        count+=1
if count > 0:
    error = old_div(error,count)
    if error > 40:
        print("ERROR: We don't seem to be running the correct distance.")
        print("This indicates that the client tick length isn't changing correctly.")
        print("OVERCLOCKING IS NOT WORKING ON YOUR SYSTEM.")
        print()
        print("Possible causes:")
        print("\tYou may be using a different Minecraft or Forge than expected.")
        print("\tTimeHelper.setMinecraftClientClockSpeed may need updating.")
        exit(1) # Other tests will be meaningless if overclocking has failed completely.
        
# Time test:
# Expected behaviour: slowest ticks should be solidly good, fastest ticks solidly bad, stuff in between who knows.
print("RESULTS OF TIME TEST:")
print("=====================")
print()
index = 0
while index < len(frameTest) and frameTest[index]:
    index+=1
if index==len(frameTest):
    print("Time test passed for all clock speeds! Go ahead and overclock.")
elif index==0:
    print("Time test was a disaster - nothing is reliable.")
else:
    print("Time is reliable with tick lengths of " + str(tickLengths[index-1]) + "ms and longer.")
index = len(frameTest) - 1
while index >= 0 and not frameTest[index]:
    index-=1
if index != len(frameTest)-1 and index >= 0:
    print("Time is definitely unreliable with tick lengths of " + str(tickLengths[index+1]) + "ms and shorter.")
    print("Anything in between is a gamble.")
print()
print()

# Observation test:
# Do something similar to time test. Expected that longer ticks will be reliable, but there will be a breaking
# point as ticks get shorter, when the server can no longer run as fast as we request.
print("RESULTS OF OBSERVATION TEST:")
print("============================")
print()
index = 0
while index < len(obsTest) and obsTest[index]:
    index+=1
if index==len(obsTest):
    print("Observation test passed for all clock speeds! Go ahead and overclock.")
elif index==0:
    print("Observation test was a disaster - nothing is reliable.")
else:
    print("Observations are reliable with tick lengths of " + str(tickLengths[index-1]) + "ms and longer.")
index = len(obsTest) - 1
while index >= 0 and not obsTest[index]:
    index-=1
if index != len(obsTest)-1 and index >= 0:
    print("Observations are definitely unreliable with tick lengths of " + str(tickLengths[index+1]) + "ms and shorter.")
    print("Anything in between is a gamble.")
print()
print()

# Frame test:
# Assuming that the user wants to be able to get frame-action pairs - ie there should be at least as many
# frames as observations, this finds the point at which observations outnumber the pairs.
# Expected behaviour is that frameTest should contain a run of passess followed by a run of fails.
print("RESULTS OF FRAME TEST:")
print("======================")
print()
count = 0
index = 0
for x in range(1, len(frameTest)):
    if frameTest[x] != frameTest[x-1]:
        count+=1
        index = x
if count > 1:
    print("ERROR: frame tests are unreliable - don't know how to interpret results!")
else:
    print("For frames of " + str(FRAME_WIDTH) + " x " + str(FRAME_HEIGHT) + ":")
    if count == 0:
        print("Render speed kept up with clock speed for all tests!")
    else:
        print("Render speed matches clock speed between " + str(tickLengths[index-1]) + "ms/tick and " + str(tickLengths[index]) + "ms/tick.")
        print("(At speeds greater than " + str(tickLengths[index]) + "ms/tick you will no longer be capable of receiving frame/action pairs.)")
        print("(If you are not interested in collecting video data, this is not a problem.)")
