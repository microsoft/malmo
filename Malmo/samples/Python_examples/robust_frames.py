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

# A sample intended to demonstrate how to robustly obtain frame-action pairs for different types
# of movement.
#
# To do this we assume that the move always succeeds - that there are always blocks to stand on,
# no blocks in the way, and no animals or other effects that could move us. If this assumption is
# not valid for your experiment then you will need to find a different approach to obtain frame-
# action pairs robustly.

from future import standard_library
standard_library.install_aliases()
from builtins import range
from builtins import object
import MalmoPython
import json
import logging
import math
import os
import random
import sys
import time
import malmoutils
if sys.version_info[0] == 2:
    # Workaround for https://github.com/PythonCharmers/python-future/issues/262
    import Tkinter as tk
else:
    import tkinter as tk
    
malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)

save_images = False
if save_images:        
    from PIL import Image
    
class RandomAgent(object):

    def __init__(self, agent_host, action_set ):
        self.rep = 0
        self.agent_host = agent_host
        self.action_set = action_set
        self.tolerance = 0.01

    def waitForInitialState( self ):
        '''Before a command has been sent we wait for an observation of the world and a frame.'''
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
            self.prev_x   = obs[u'XPos']
            self.prev_y   = obs[u'YPos']
            self.prev_z   = obs[u'ZPos']
            self.prev_yaw = obs[u'Yaw']
            print('Initial position:',self.prev_x,',',self.prev_y,',',self.prev_z,'yaw',self.prev_yaw)
            
            if save_images:
                # save the frame, for debugging
                frame = world_state.video_frames[-1]
                image = Image.frombytes('RGB', (frame.width, frame.height), bytes(frame.pixels) )
                self.iFrame = 0
                self.rep = self.rep + 1
                image.save( 'rep_' + str(self.rep).zfill(3) + '_saved_frame_' + str(self.iFrame).zfill(4) + '.png' )
            
        return world_state

    def waitForNextState( self ):
        '''After each command has been sent we wait for the observation to change as expected and a frame.'''
        # wait for the observation position to have changed
        print('Waiting for observation...', end=' ')
        while True:
            world_state = self.agent_host.peekWorldState()
            if not world_state.is_mission_running:
                print('mission ended.')
                break
            if not all(e.text=='{}' for e in world_state.observations):
                obs = json.loads( world_state.observations[-1].text )
                self.curr_x   = obs[u'XPos']
                self.curr_y   = obs[u'YPos']
                self.curr_z   = obs[u'ZPos']
                self.curr_yaw = obs[u'Yaw']
                if self.require_move:
                    if math.fabs( self.curr_x - self.prev_x ) > self.tolerance or\
                       math.fabs( self.curr_y - self.prev_y ) > self.tolerance or\
                       math.fabs( self.curr_z - self.prev_z ) > self.tolerance:
                        print('received a move.')
                        break
                elif self.require_yaw_change:
                    if math.fabs( self.curr_yaw - self.prev_yaw ) > self.tolerance:
                        print('received a turn.')
                        break
                else:
                    print('received.')
                    break
        # wait for the render position to have changed
        print('Waiting for render...', end=' ')
        while True:
            world_state = self.agent_host.peekWorldState()
            if not world_state.is_mission_running:
                print('mission ended.')
                break
            if len(world_state.video_frames) > 0:
                frame = world_state.video_frames[-1]
                curr_x_from_render   = frame.xPos
                curr_y_from_render   = frame.yPos
                curr_z_from_render   = frame.zPos
                curr_yaw_from_render = frame.yaw
                if self.require_move:
                    if math.fabs( curr_x_from_render - self.prev_x ) > self.tolerance or\
                       math.fabs( curr_y_from_render - self.prev_y ) > self.tolerance or\
                       math.fabs( curr_z_from_render - self.prev_z ) > self.tolerance:
                        print('received a move.')
                        break
                elif self.require_yaw_change:
                    if math.fabs( curr_yaw_from_render - self.prev_yaw ) > self.tolerance:
                        print('received a turn.')
                        break
                else:
                    print('received.')
                    break
            
        num_frames_before_get = len(world_state.video_frames)
        world_state = self.agent_host.getWorldState()

        if save_images:
            # save the frame, for debugging
            if world_state.is_mission_running:
                assert len(world_state.video_frames) > 0, 'No video frames!?'
                frame = world_state.video_frames[-1]
                image = Image.frombytes('RGB', (frame.width, frame.height), bytes(frame.pixels) )
                self.iFrame = self.iFrame + 1
                image.save( 'rep_' + str(self.rep).zfill(3) + '_saved_frame_' + str(self.iFrame).zfill(4) + '.png' )
            
        if world_state.is_mission_running:
            assert len(world_state.video_frames) > 0, 'No video frames!?'
            num_frames_after_get = len(world_state.video_frames)
            assert num_frames_after_get >= num_frames_before_get, 'Fewer frames after getWorldState!?'
            frame = world_state.video_frames[-1]
            obs = json.loads( world_state.observations[-1].text )
            self.curr_x   = obs[u'XPos']
            self.curr_y   = obs[u'YPos']
            self.curr_z   = obs[u'ZPos']
            self.curr_yaw = obs[u'Yaw']
            print('New position from observation:',self.curr_x,',',self.curr_y,',',self.curr_z,'yaw',self.curr_yaw, end=' ')
            if math.fabs( self.curr_x   - self.expected_x   ) > self.tolerance or\
               math.fabs( self.curr_y   - self.expected_y   ) > self.tolerance or\
               math.fabs( self.curr_z   - self.expected_z   ) > self.tolerance or\
               math.fabs( self.curr_yaw - self.expected_yaw ) > self.tolerance:
                print(' - ERROR DETECTED! Expected:',self.expected_x,',',self.expected_y,',',self.expected_z,'yaw',self.expected_yaw)
                exit(1)
            else:
                print('as expected.')
            curr_x_from_render   = frame.xPos
            curr_y_from_render   = frame.yPos
            curr_z_from_render   = frame.zPos
            curr_yaw_from_render = frame.yaw
            print('New position from render:',curr_x_from_render,',',curr_y_from_render,',',curr_z_from_render,'yaw',curr_yaw_from_render, end=' ')
            if math.fabs( curr_x_from_render   - self.expected_x   ) > self.tolerance or\
               math.fabs( curr_y_from_render   - self.expected_y   ) > self.tolerance or \
               math.fabs( curr_z_from_render   - self.expected_z   ) > self.tolerance or \
               math.fabs( curr_yaw_from_render - self.expected_yaw ) > self.tolerance:
                print(' - ERROR DETECTED! Expected:',self.expected_x,',',self.expected_y,',',self.expected_z,'yaw',self.expected_yaw)
                exit(1)
            else:
                print('as expected.')
            self.prev_x   = self.curr_x
            self.prev_y   = self.curr_y
            self.prev_z   = self.curr_z
            self.prev_yaw = self.curr_yaw
            
        return world_state
        
    def act( self ):
        '''Take an action from the action_set and set the expected outcome so we can wait for it.'''
        if self.action_set == 'discrete_absolute':
            actions = ['movenorth 1', 'movesouth 1', 'movewest 1', 'moveeast 1']
        elif self.action_set == 'discrete_relative':
            actions = ['move 1', 'move -1', 'turn 1', 'turn -1']
        elif self.action_set == 'teleport':
            while True:
                dx = random.randint(-10,10)
                dz = random.randint(-10,10)
                if not dx == 0 or not dz == 0:
                    break
            self.expected_x = self.prev_x + dx
            self.expected_y = self.prev_y
            self.expected_z = self.prev_z + dz
            self.expected_yaw = self.prev_yaw
            self.require_move = True
            self.require_yaw_change = False
            actions = ['tp '+str(self.expected_x)+' '+str(self.expected_y)+' '+str(self.expected_z)]
        else:
            print('ERROR: Unsupported action set:',self.action_set)
            exit(1)
        i_action = random.randint(0,len(actions)-1)
        action = actions[ i_action ]
        print('Sending',action)
        self.agent_host.sendCommand( action )
        if self.action_set == 'discrete_absolute':
            self.expected_x = self.prev_x + [0,0,-1,1][i_action]
            self.expected_y = self.prev_y
            self.expected_z = self.prev_z + [-1,1,0,0][i_action]
            self.expected_yaw = self.prev_yaw
            self.require_move = True
            self.require_yaw_change = False
        elif self.action_set == 'discrete_relative':
            if i_action == 0 or i_action == 1:
                i_yaw = indexOfClosest( [0,90,180,270], self.prev_yaw )
                forward = [ (0,1), (-1,0), (0,-1), (1,0) ][ i_yaw ]
                if i_action == 0:
                    self.expected_x = self.prev_x + forward[0]
                    self.expected_z = self.prev_z + forward[1]
                else:
                    self.expected_x = self.prev_x - forward[0]
                    self.expected_z = self.prev_z - forward[1]
                self.expected_y = self.prev_y
                self.expected_yaw = self.prev_yaw
                self.require_move = True
                self.require_yaw_change = False
            else:
                self.expected_x = self.prev_x
                self.expected_y = self.prev_y
                self.expected_z = self.prev_z
                self.expected_yaw = math.fmod( 360 + self.prev_yaw + [90,-90][i_action-2], 360 )
                self.require_move = False
                self.require_yaw_change = True
        elif self.action_set == 'teleport':
            pass
        else:
            print('ERROR: Unsupported action set:',self.action_set)
            exit(1)
            
def indexOfClosest( arr, val ):
    '''Return the index in arr of the closest float value to val.'''
    i_closest = None
    for i,v in enumerate(arr):
        d = math.fabs( v - val )
        if i_closest == None or d < d_closest:
            i_closest = i
            d_closest = d
    return i_closest

# -- set up the mission --
xml = '''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <About>
    <Summary/>
  </About>
  <ModSettings>
    <MsPerTick>1</MsPerTick>
  </ModSettings>
  <ServerSection>
    <ServerHandlers>
      <FlatWorldGenerator forceReset="true" generatorString="3;7,220*1,5*3,2;3;,biome_1" seed=""/>
      <ServerQuitFromTimeUp description="" timeLimitMs="250000"/>
      <ServerQuitWhenAnyAgentFinishes description=""/>
    </ServerHandlers>
  </ServerSection>
  <AgentSection mode="Survival">
    <Name>Cristina</Name>
    <AgentStart>
      <Placement x="0.5" y="227.0" z="0.5" pitch="30" yaw="0"/>
    </AgentStart>
    <AgentHandlers>
      <ObservationFromFullStats/>
      <VideoProducer viewpoint="0" want_depth="false">
        <Width>320</Width>
        <Height>240</Height>
      </VideoProducer>
      <RewardForReachingPosition dimension="0">
        <Marker oneshot="true" reward="100" tolerance="1.1" x="19.5" y="0" z="19.5"/>
      </RewardForReachingPosition>
    </AgentHandlers>
  </AgentSection>
</Mission>'''
my_mission = MalmoPython.MissionSpec(xml,True)

# -- test each action set in turn --
max_retries = 3
action_sets = ['discrete_absolute','discrete_relative', 'teleport']
for action_set in action_sets:

    if action_set == 'discrete_absolute':
        my_mission.allowAllDiscreteMovementCommands()
    elif action_set == 'discrete_relative':
        my_mission.allowAllDiscreteMovementCommands()
    elif action_set == 'teleport':
        my_mission.allowAllAbsoluteMovementCommands()
    else:
        print('ERROR: Unsupported action set:',action_set)
        exit(1)

    my_mission_recording = malmoutils.get_default_recording_object(agent_host, action_set)
    for retry in range(max_retries):
        try:
            agent_host.startMission( my_mission, my_mission_recording )
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print("Error starting mission:",e)
                exit(1)
            else:
                time.sleep(2.5)

    print("Waiting for the mission to start", end=' ')
    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        print(".", end="")
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
        for error in world_state.errors:
            print("Error:",error.text)
    print()

    # the main loop:
    agent = RandomAgent( agent_host, action_set )
    world_state = agent.waitForInitialState()
    while world_state.is_mission_running:
        agent.act()
        world_state = agent.waitForNextState()
