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

# Similar to run_mission.py, but tests the Python _str_ bindings.

from builtins import range
import MalmoPython
import os
import random
import sys
import time
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)

print(agent_host)

my_mission = MalmoPython.MissionSpec()
my_mission.timeLimitInSeconds( 10 )
my_mission.requestVideo( 320, 240 )
my_mission.rewardForReachingPosition( 19.5, 0.0, 19.5, 100.0, 1.1 )
print(my_mission)

my_mission_record = malmoutils.get_default_recording_object(agent_host, "saved_data")
print(my_mission_record)

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
    time.sleep(0.1)
    world_state = agent_host.getWorldState()
    print(world_state)
    for error in world_state.errors:
        print("Error:",error.text)
print()

# main loop:
while world_state.is_mission_running:
    agent_host.sendCommand( "move 1" )
    agent_host.sendCommand( "turn " + str(random.random()*2-1) )
    time.sleep(0.5)
    world_state = agent_host.getWorldState()
    print(world_state)
    for reward in world_state.rewards:
        print(reward)
    for frame in world_state.video_frames:
        print(frame)
    for obs in world_state.observations:
        print(obs)
    for error in world_state.errors:
        print("Error:",error.text)

print("Mission has stopped.")
