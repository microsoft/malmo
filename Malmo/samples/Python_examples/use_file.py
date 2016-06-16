# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

import MalmoPython
import os
import random
import sys
import time
import json

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

agent_host = MalmoPython.AgentHost()
agent_host.addOptionalStringArgument( "file", "An XML mission specification file to use - see Sample_missions folder.", "" )
try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print 'ERROR:',e
    print agent_host.getUsage()
    exit(1)
if agent_host.receivedArgument("help"):
    print agent_host.getUsage()
    exit(0)

if agent_host.receivedArgument("test"):
    exit(0) # TODO: find a way to usefully run this sample as an integration test

input_file_name = agent_host.getStringArgument( "file" )
if input_file_name == "":
    print '\nERROR: Supply a file to load on the command line.\n'
    print agent_host.getUsage()
    exit(1)
    
validate = True
mission_file = open( agent_host.getStringArgument( "file" ), 'r' )
my_mission = MalmoPython.MissionSpec(mission_file.read(),validate)

for iRepeat in range(30000):

    try:
        my_mission_record = MalmoPython.MissionRecordSpec()
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
    print

    # main loop:
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        while world_state.number_of_observations_since_last_state < 1 and world_state.is_mission_running:
            print "Waiting for observations..."
            time.sleep(0.05)
            world_state = agent_host.getWorldState()

        if world_state.is_mission_running:
            print "Got " + str(world_state.number_of_observations_since_last_state) + " observations since last state."
            msg = world_state.observations[0].text

    print "Mission has stopped."
    time.sleep(0.5) # Give mod a little time to get back to dormant state.
