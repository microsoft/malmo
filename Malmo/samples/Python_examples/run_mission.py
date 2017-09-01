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

import MalmoPython
import os
import random
import sys
import time
#from PIL import Image

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

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

my_mission = MalmoPython.MissionSpec()
my_mission.timeLimitInSeconds( 10 )
my_mission.requestVideo( 320, 240 )
my_mission.rewardForReachingPosition( 19.5, 0.0, 19.5, 100.0, 1.1 )

my_mission_record = MalmoPython.MissionRecordSpec("./saved_data.tgz")
my_mission_record.recordCommands()
my_mission_record.recordMP4(20, 400000)
my_mission_record.recordRewards()
my_mission_record.recordObservations()

max_retries = 3
for retry in range(max_retries):
    try:
        agent_host.startMission( my_mission, my_mission_record )
        break
    except RuntimeError as e:
        if retry == max_retries - 1:
            print "Error starting mission:",e
            exit(1)
        else:
            time.sleep(2)

print "Waiting for the mission to start",
world_state = agent_host.getWorldState()
while not world_state.has_mission_begun:
    sys.stdout.write(".")
    time.sleep(0.1)
    world_state = agent_host.getWorldState()
    for error in world_state.errors:
        print "Error:",error.text
print

# main loop:
while world_state.is_mission_running:
    agent_host.sendCommand( "move 1" )
    agent_host.sendCommand( "turn " + str(random.random()*2-1) )
    time.sleep(0.5)
    world_state = agent_host.getWorldState()
    print "video,observations,rewards received:",world_state.number_of_video_frames_since_last_state,world_state.number_of_observations_since_last_state,world_state.number_of_rewards_since_last_state
    for reward in world_state.rewards:
        print "Summed reward:",reward.getValue()
    for error in world_state.errors:
        print "Error:",error.text
    for frame in world_state.video_frames:
        print "Frame:",frame.width,'x',frame.height,':',frame.channels,'channels'
        #image = Image.frombytes('RGB', (frame.width, frame.height), str(frame.pixels) ) # to convert to a PIL image
print "Mission has stopped."
