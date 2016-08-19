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

    def __init__(self, agent_host, action_set ):
        self.rep = 0
        self.agent_host = agent_host
        self.action_set = action_set
        self.tolerance = 0.01
        if action_set == 'discrete_absolute':
            self.actions = ['movenorth 1', 'movesouth 1', 'movewest 1', 'moveeast 1']
            self.require_move = True
        else:
            print 'ERROR: Unsupported action set:',action_set
            exit(1)

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

        if world_state.is_mission_running:
                
            assert len(world_state.video_frames) > 0, 'No video frames!?'
            
            obs = json.loads( world_state.observations[-1].text )
            self.prev_x = obs[u'XPos']
            self.prev_z = obs[u'ZPos']
            print 'Initial position:',self.prev_x,',',self.prev_z
            
            if save_images:
                # save the frame, for debugging
                frame = world_state.video_frames[-1]
                image = Image.frombytes('RGB', (frame.width, frame.height), str(frame.pixels) )
                self.iFrame = 0
                self.rep = self.rep + 1
                image.save( 'rep_' + str(self.rep).zfill(3) + '_saved_frame_' + str(iFrame).zfill(4) + '.png' )
            
        return world_state

    def waitForNextState( self ):
       
        # wait for the position to have changed and a reward received
        print 'Waiting for data...',
        while True:
            world_state = self.agent_host.peekWorldState()
            if not world_state.is_mission_running:
                print 'mission ended.'
                break
            if len(world_state.rewards) > 0 and not all(e.text=='{}' for e in world_state.observations):
                obs = json.loads( world_state.observations[-1].text )
                self.curr_x = obs[u'XPos']
                self.curr_z = obs[u'ZPos']
                if self.require_move:
                    if math.hypot( self.curr_x - self.prev_x, self.curr_z - self.prev_z ) > self.tolerance:
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
            self.curr_x = obs[u'XPos']
            self.curr_z = obs[u'ZPos']
            print 'New position from observation:',self.curr_x,',',self.curr_z,
            if math.hypot( self.curr_x - self.expected_x, self.curr_z - self.expected_z ) > self.tolerance:
                print ' - ERROR DETECTED! Expected:',self.expected_x,',',self.expected_z
                raw_input("Press Enter to continue...")
            else:
                print 'as expected.'
            curr_x_from_render = frame.xPos
            curr_z_from_render = frame.zPos
            print 'New position from render:',curr_x_from_render,',',curr_z_from_render,
            if math.hypot( curr_x_from_render - self.expected_x, curr_z_from_render - self.expected_z ) > self.tolerance:
                print ' - ERROR DETECTED! Expected:',self.expected_x,',',self.expected_z
                raw_input("Press Enter to continue...")
            else:
                print 'as expected.'
            self.prev_x = self.curr_x
            self.prev_z = self.curr_z
            
        return world_state
        
    def act( self ):
        i_action = random.randint(0,len(self.actions)-1)
        action = self.actions[ i_action ]
        print 'Sending',action
        self.agent_host.sendCommand( action )
        if self.action_set == 'discrete_absolute':
            self.expected_x = self.prev_x + [0,0,-1,1][i_action]
            self.expected_z = self.prev_z + [-1,1,0,0][i_action]
        

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

# -- set up the agent host --
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

# -- set up the mission --
xml = '''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://ProjectMalmo.microsoft.com Mission.xsd">
  <About>
    <Summary/>
  </About>
  <ModSettings>
    <MsPerTick>1</MsPerTick>
  </ModSettings>
  <ServerSection>
    <ServerHandlers>
      <FlatWorldGenerator forceReset="false" generatorString="3;7,220*1,5*3,2;3;,biome_1" seed=""/>
      <ServerQuitFromTimeUp description="" timeLimitMs="130000"/>
      <ServerQuitWhenAnyAgentFinishes description=""/>
    </ServerHandlers>
  </ServerSection>
  <AgentSection mode="Survival">
    <Name>Cristina</Name>
    <AgentStart/>
    <AgentHandlers>
      <ObservationFromFullStats/>
      <VideoProducer viewpoint="0" want_depth="false">
        <Width>320</Width>
        <Height>240</Height>
      </VideoProducer>
      <RewardForReachingPosition dimension="0">
        <Marker oneshot="true" reward="100" tolerance="1.100000023841858" x="19.5" y="0" z="19.5"/>
      </RewardForReachingPosition>
      <RewardForSendingCommand reward="0" />
      <DiscreteMovementCommands/>
    </AgentHandlers>
  </AgentSection>
</Mission>'''
my_mission = MalmoPython.MissionSpec(xml,True)

max_retries = 3

my_mission_record = MalmoPython.MissionRecordSpec( 'robust_frames.tgz' )
my_mission_record.recordCommands()
my_mission_record.recordMP4(20, 400000)
my_mission_record.recordRewards()
my_mission_record.recordObservations()

for retry in range(max_retries):
    try:
        agent_host.startMission( my_mission, my_mission_record )
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
action_set = 'discrete_absolute'
agent = RandomAgent( agent_host, action_set )
world_state = agent.waitForInitialState()
while world_state.is_mission_running:
    agent.act()
    world_state = agent.waitForNextState()
