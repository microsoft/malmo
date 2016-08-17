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

# A sample intended to demonstrate how to robustly obtain frame-action pairs for different
# types of movement.

import MalmoPython
import json
import logging
import math
import os
import random
import sys
import time
import Tkinter as tk

save_images = False
if save_images:        
    from PIL import Image
    
class RandomAgent:

    def __init__(self, agent_host ):
        self.rep = 0
        self.agent_host = agent_host

    def waitForInitialState( self ):
        # wait for a valid observation
        world_state = self.agent_host.peekWorldState()
        while world_state.is_mission_running and all(e.text=='{}' for e in world_state.observations):
            world_state = self.agent_host.peekWorldState()
        # wait for a frame to arrive after that
        num_frames_seen = world_state.number_of_video_frames_since_last_state
        while world_state.is_mission_running and world_state.number_of_video_frames_since_last_state == num_frames_seen:
            world_state = self.agent_host.peekWorldState()
        world_state = self.agent_host.getWorldState()
        for err in world_state.errors:
            print err

        if not world_state.is_mission_running:
            return 0 # mission already ended
            
        assert len(world_state.video_frames) > 0, 'No video frames!?'
        
        obs = json.loads( world_state.observations[-1].text )
        prev_x = obs[u'XPos']
        prev_z = obs[u'ZPos']
        print 'Initial position:',prev_x,',',prev_z
        
        if save_images:
            # save the frame, for debugging
            frame = world_state.video_frames[-1]
            image = Image.frombytes('RGB', (frame.width, frame.height), str(frame.pixels) )
            self.iFrame = 0
            self.rep = self.rep + 1
            image.save( 'rep_' + str(self.rep).zfill(3) + '_saved_frame_' + str(iFrame).zfill(4) + '.png' )
            
        return world_state

    def waitForNextState( self ):            
        require_move = True
        check_expected_position = True
        
        # wait for the position to have changed and a reward received
        print 'Waiting for data...',
        while True:
            world_state = self.agent_host.peekWorldState()
            if not world_state.is_mission_running:
                print 'mission ended.'
                break
            if len(world_state.rewards) > 0 and not all(e.text=='{}' for e in world_state.observations):
                obs = json.loads( world_state.observations[-1].text )
                curr_x = obs[u'XPos']
                curr_z = obs[u'ZPos']
                if require_move:
                    if math.hypot( curr_x - prev_x, curr_z - prev_z ) > tol:
                        print 'received.'
                        break
                else:
                    print 'received.'
                    break
        # wait for a frame to arrive after that
        num_frames_seen = world_state.number_of_video_frames_since_last_state
        while world_state.is_mission_running and world_state.number_of_video_frames_since_last_state == num_frames_seen:
            world_state = self.agent_host.peekWorldState()
            
        num_frames_before_get = len(world_state.video_frames)
        
        world_state = self.agent_host.getWorldState()
        for err in world_state.errors:
            print err

        if save_images:
            # save the frame, for debugging
            if world_state.is_mission_running:
                assert len(world_state.video_frames) > 0, 'No video frames!?'
                frame = world_state.video_frames[-1]
                image = Image.frombytes('RGB', (frame.width, frame.height), str(frame.pixels) )
                self.iFrame = self.iFrame + 1
                image.save( 'rep_' + str(self.rep).zfill(3) + '_saved_frame_' + str(self.iFrame).zfill(4) + '.png' )
            
        if world_state.is_mission_running:
            assert len(world_state.video_frames) > 0, 'No video frames!?'
            num_frames_after_get = len(world_state.video_frames)
            assert num_frames_after_get >= num_frames_before_get, 'Fewer frames after getWorldState!?'
            frame = world_state.video_frames[-1]
            obs = json.loads( world_state.observations[-1].text )
            curr_x = obs[u'XPos']
            curr_z = obs[u'ZPos']
            print 'New position from observation:',curr_x,',',curr_z,'after action:',self.actions[self.prev_a], #NSWE
            if check_expected_position:
                expected_x = prev_x + [0,0,-1,1][self.prev_a]
                expected_z = prev_z + [-1,1,0,0][self.prev_a]
                if math.hypot( curr_x - expected_x, curr_z - expected_z ) > tol:
                    print ' - ERROR DETECTED! Expected:',expected_x,',',expected_z
                    raw_input("Press Enter to continue...")
                else:
                    print 'as expected.'
                curr_x_from_render = frame.xPos
                curr_z_from_render = frame.zPos
                print 'New position from render:',curr_x_from_render,',',curr_z_from_render,'after action:',self.actions[self.prev_a], #NSWE
                if math.hypot( curr_x_from_render - expected_x, curr_z_from_render - expected_z ) > tol:
                    print ' - ERROR DETECTED! Expected:',expected_x,',',expected_z
                    raw_input("Press Enter to continue...")
                else:
                    print 'as expected.'
            else:
                print
            prev_x = curr_x
            prev_z = curr_z
            
        return world_state
        
    def act( self ):
        pass # TODO

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

agent_host = MalmoPython.AgentHost()
agent_host.addOptionalStringArgument('mission_file',
    'Path/to/file from which to load the mission.', '../Sample_missions/cliff_walking_1.xml')

try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print 'ERROR:',e
    print agent_host.getUsage()
    exit(1)
if agent_host.receivedArgument("help"):
    print agent_host.getUsage()
    exit(0)


agent = RandomAgent( agent_host )

# -- set up the mission -- #
mission_file = agent_host.getStringArgument('mission_file')
with open(mission_file, 'r') as f:
    print "Loading mission from %s" % mission_file
    mission_xml = f.read()
    my_mission = MalmoPython.MissionSpec(mission_xml, True)

my_clients = MalmoPython.ClientPool()
my_clients.add(MalmoPython.ClientInfo('127.0.0.1', 10000)) # add Minecraft machines here as available

max_retries = 3
agentID = 0
expID = 'tabular_q_learning'

my_mission_record = MalmoPython.MissionRecordSpec( "./save_%s-map%d-rep%d.tgz" % (expID, imap, i) )
my_mission_record.recordCommands()
my_mission_record.recordMP4(20, 400000)
my_mission_record.recordRewards()
my_mission_record.recordObservations()

for retry in range(max_retries):
    try:
        agent_host.startMission( my_mission, my_clients, my_mission_record, agentID, "%s-%d" % (expID, i) )
        break
    except RuntimeError as e:
        if retry == max_retries - 1:
            print "Error starting mission:",e
            exit(1)
        else:
            time.sleep(2.5)

print "Waiting for the mission to start",
world_state = agent_host.getWorldState()
while not world_state.has_mission_begun:
    sys.stdout.write(".")
    time.sleep(0.1)
    world_state = agent_host.getWorldState()
    for error in world_state.errors:
        print "Error:",error.text
print

# the main loop:
world_state = agent.waitForInitialState()
while world_state.is_mission_running:
    world_state = agent.waitForNextState()
    agent.act()
