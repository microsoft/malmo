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

# Simple test of the "human level" controls.
# Essentially the same as the maze_runner sample, but this one uses a different
# action space - sending mouse movement commands to steer and a key press command to move.

# There's also a moving target decorator in the mix, to make things more interesting,
# which can also traverse the maze (though will mainly stay outside it).

# Fractionally more usefully, this can be run in "sweep mode" (always true when run as a test),
# which will sweep a range of values for the "dampening factor" (see below), and plot/save a graph
# showing how best to tune the mouse deltas used for steering.

from builtins import range
import MalmoPython
import os
import random
import sys
import time
import json
import errno
import malmoutils
import xml.etree.ElementTree

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
agent_host.addOptionalFlag("plot,p", "Sweep the damping factor and plot the results")
malmoutils.parse_command_line(agent_host)

TESTING = agent_host.receivedArgument("test")
SWEEP_DAMPENING_FACTOR = agent_host.receivedArgument("plot") or TESTING

def GetMissionXML( agent_host, seed, include_beacon ):
    beacon = '''<MovingTargetDecorator>
                    <ArenaBounds>
                       <min x="-1" y="226" z="-1"/>
                       <max x="40" y="226" z="40"/>
                    </ArenaBounds>
                    <StartPos x="10" y="226" z="40"/>
                    <Seed>random</Seed>
                    <UpdateSpeed>1</UpdateSpeed>
                    <PermeableBlocks type="redstone_block emerald_block purpur_block"/>
                    <BlockType type="beacon"/>
                </MovingTargetDecorator>''' if include_beacon else ""

    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Navigating via Mouse Commands</Summary>
        </About>

        <ServerSection>
            <ServerInitialConditions>
                <Time>
                    <StartTime>13000</StartTime>
                    <AllowPassageOfTime>false</AllowPassageOfTime>
                </Time>
                <AllowSpawning>false</AllowSpawning>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="-2" y1="225" z1="-2" x2="41" y2="225" z2="41" type="stone" variant="smooth_granite"/>
                    <DrawCuboid x1="-2" y1="226" z1="-2" x2="41" y2="226" z2="41" type="air"/>
                    <DrawCuboid x1="-1" y1="226" z1="-1" x2="40" y2="226" z2="40" type="purpur_block"/>
                </DrawingDecorator>
                <MazeDecorator>
                    <SizeAndPosition length="40" width="40" yOrigin="225" zOrigin="0" height="18"/>
                    <GapProbability variance="0.4">0.5</GapProbability>
                    <Seed>''' + seed + '''</Seed>
                    <MaterialSeed>random</MaterialSeed>
                    <AllowDiagonalMovement>false</AllowDiagonalMovement>
                    <StartBlock fixedToEdge="true" type="emerald_block" height="1"/>
                    <EndBlock fixedToEdge="true" type="redstone_block" height="12"/>
                    <PathBlock type="coal_block" height="1"/>
                    <FloorBlock type="stone" variant="smooth_granite"/>
                    <OptimalPathBlock type="purpur_block"/>
                    <GapBlock type="air"/>
                    <AddQuitProducer description="finished_maze"/>
                    <AddNavigationObservations/>
                </MazeDecorator>''' + beacon + '''
                <ServerQuitFromTimeUp timeLimitMs="450000" description="time_up"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>LittleMouse</Name>
            <AgentStart>
                <Placement x="-204" y="81" z="217"/>
            </AgentStart>
            <AgentHandlers>
                <HumanLevelCommands/>''' + malmoutils.get_video_xml(agent_host) + '''
                <AgentQuitFromTouchingBlockType>
                    <Block type="beacon" description="caught_beacon"/>
                    <Block type="stone" variant="smooth_granite" description="fell_off"/>
                </AgentQuitFromTouchingBlockType>
            </AgentHandlers>
        </AgentSection>
    </Mission>'''

validate = True
if SWEEP_DAMPENING_FACTOR:
    num_reps = 32
    seed = "15362312"   # Chosen by fair dice roll, guaranteed to be random.
else:
    num_reps = 30000
    seed = "random"

successful_runs = 0
run_data = []
for iRepeat in range(num_reps):
    if SWEEP_DAMPENING_FACTOR:
        # Sweep for a good dampening factor (see comments below)
        dampen_factor = (iRepeat + 1.0) / num_reps
        print("Running with dampening factor: ", dampen_factor)
    else:
        dampen_factor = 0.3 # Seems to work pretty well.

    my_mission_record = malmoutils.get_default_recording_object(agent_host, "Mission_{}".format(iRepeat + 1))
    my_mission = MalmoPython.MissionSpec(GetMissionXML(agent_host, seed, not SWEEP_DAMPENING_FACTOR), validate)

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

    print("Waiting for the mission to start", end=' ')
    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        print(".", end="")
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
        if len(world_state.errors):
            print()
            for error in world_state.errors:
                print("Error:",error.text)
                exit()
    print()

    # Track some statistics:
    total_abs_yaw_deltas = 0
    total_commands_sent = 0
    total_sign_changes = 0

    agent_host.sendCommand("forward 1")
    last_mouse_delta = None
    # main loop:
    while world_state.is_mission_running:
        if world_state.number_of_observations_since_last_state > 0:
            msg = world_state.observations[-1].text
            ob = json.loads(msg)
            current_yaw_delta = ob.get(u'yawDelta', 0)
            total_abs_yaw_deltas += abs(current_yaw_delta)
            # The observed yaw delta is normalised to [-1,1] - so multiply by
            # 180 to get degrees:
            current_yaw_delta *= 180
            # The mouse movement delta required for turning works out at the desired yaw delta (in degrees)
            # divided by 0.15 (this is derived from looking at the Minecraft source code)
            mouse_delta = current_yaw_delta / 0.15
            # but attempting to do the full turn in one action will result in horrible oversteering
            # and oscillation, so dampen, and only do a percentage of the turn.
            damped_mouse_delta = int(dampen_factor * mouse_delta)
            if last_mouse_delta == None or (damped_mouse_delta < 0) != (last_mouse_delta < 0):
                total_sign_changes += 1
            last_mouse_delta = damped_mouse_delta
            agent_host.sendCommand("moveMouse {} 0".format(damped_mouse_delta))
            total_commands_sent += 1
        world_state = agent_host.getWorldState()

    print("Mission has stopped.")
    # Parse the MissionEnded XML messasge:
    mission_end_tree = xml.etree.ElementTree.fromstring(world_state.mission_control_messages[-1].text)
    ns_dict = {"malmo":"http://ProjectMalmo.microsoft.com"}
    stat = mission_end_tree.find("malmo:Status", ns_dict).text
    hr_stat = mission_end_tree.find("malmo:HumanReadableStatus", ns_dict).text
    print("Mission over. Status: ", stat, end=' ')
    if len(hr_stat):
        print(" - " + hr_stat)
    if TESTING and hr_stat not in ["finished_maze", "caught_beacon", "fell_off"]:
        print("FAILED - we should have either caught the beacon, or reached the end of the maze.")
        exit(1)
    if hr_stat in ["finished_maze", "caught_beacon"]:
        # falling off isn't an automatic fail, since it is very likely to happen,
        # especially when sweeping for a good dampening factor.
        # But if we *never* have a successful run, there's something to worry about.
        successful_runs += 1

    run = {
        "result":hr_stat,
        "total_abs_yaw_deltas":total_abs_yaw_deltas,
        "commands_sent":total_commands_sent,
        "reversals":total_sign_changes,
        "damping_factor":dampen_factor
    }
    if SWEEP_DAMPENING_FACTOR:
        print(run)
    run_data.append(run)
    time.sleep(0.5) # Give mod a little time to get back to dormant state.

if SWEEP_DAMPENING_FACTOR:
    import matplotlib
    import numpy
    import pylab

    damp_vals = [x["damping_factor"] for x in run_data]
    reversals = [x["reversals"] for x in run_data]
    total_yaw = [x["total_abs_yaw_deltas"] for x in run_data]
    commands = [x["commands_sent"] for x in run_data]
    
    plot_reversals = pylab.plot(damp_vals, reversals, 'r-', label='Reversals')
    plot_yaw = pylab.plot(damp_vals, total_yaw, 'g-', label='Total abs yaw delta')
    plot_commands = pylab.plot(damp_vals, commands, 'b-', label='Commands sent')
    pylab.xlabel('Damping factor')
    pylab.legend()
    pylab.title("Sweep of dampening factor.")
    if TESTING:
        pylab.savefig(malmoutils.get_recordings_directory(agent_host) + "//dampening_factor_sweep.png")
    else:
        pylab.show()

if TESTING and successful_runs == 0:
    print("No successful runs!")
    exit(1)