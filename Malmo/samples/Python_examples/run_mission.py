# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

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
my_mission.rewardForReachingPosition( 19, 0, 19, 100.0, 1.1 )

my_mission_record = MalmoPython.MissionRecordSpec("./saved_data.tgz")
my_mission_record.recordCommands()
my_mission_record.recordMP4(20, 400000)
my_mission_record.recordRewards()
my_mission_record.recordObservations()

try:
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
        print "Summed reward:",reward.value
    for error in world_state.errors:
        print "Error:",error.text
    for frame in world_state.video_frames:
        print "Frame:",frame.width,'x',frame.height,':',frame.channels,'channels'
        #image = Image.frombytes('RGB', (frame.width, frame.height), str(bytearray(frame.pixels)) ) # to convert to a PIL image
print "Mission has stopped."
