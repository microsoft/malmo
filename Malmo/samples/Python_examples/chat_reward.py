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

# Tutorial sample #1: Run simple mission

from builtins import range
import MalmoPython
import os
import sys
import time
import random
import malmoutils

malmoutils.fix_print()

# Create default Malmo objects:
agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)
    
items = {'red_flower':'flower',
         'apple':'apple',
         'iron_sword':'sword',
         'iron_pickaxe':'pickaxe',
         'diamond_sword':'sword'
         }
obj_id = list(items.keys())[random.randint(0, len(items)-1)]

mission_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <About>
    <Summary>Name the first item you see.</Summary>
  </About>

  <ServerSection>
      <ServerInitialConditions>
            <Time>
                <StartTime>6000</StartTime>
                <AllowPassageOfTime>false</AllowPassageOfTime>
            </Time>
            <Weather>clear</Weather>
            <AllowSpawning>false</AllowSpawning>
      </ServerInitialConditions>
    <ServerHandlers>
      <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1"/>
      <DrawingDecorator>
        <!-- coordinates for cuboid are inclusive -->
        <DrawCuboid x1="0" y1="46" z1="0" x2="7" y2="52" z2="7" type="quartz_block" /> <!-- limits of our arena -->
        <DrawCuboid x1="1" y1="47" z1="1" x2="6" y2="51" z2="6" type="air" /> <!-- limits of our arena -->
        <DrawCuboid x1="1" y1="50" z1="1" x2="6" y2="49" z2="6" type="glowstone" />            <!-- limits of our arena -->
        <DrawItem    x="4"   y="47"  z="2" type="'''+obj_id+'''" />
      </DrawingDecorator>
      <ServerQuitFromTimeUp timeLimitMs="5000"/>
      <ServerQuitWhenAnyAgentFinishes/>
    </ServerHandlers>
  </ServerSection>

  <AgentSection mode="Survival">
    <Name>Chatty</Name>
    <AgentStart>
      <Placement x="3" y="47.0" z="3" pitch="30" yaw="270"/>
    </AgentStart>
    <AgentHandlers>
      <ObservationFromFullStats/>
      <VideoProducer want_depth="false">
          <Width>640</Width>
          <Height>480</Height>
      </VideoProducer>
      <DiscreteMovementCommands />
      <ChatCommands />
      <RewardForSendingMatchingChatMessage>
        <ChatMatch reward="100.0" regex="'''+items[obj_id]+'''" description="Anything that matches the object."/>
      </RewardForSendingMatchingChatMessage>
      <RewardForSendingCommand reward="-1"/>
    </AgentHandlers>
  </AgentSection>
</Mission>
'''

my_mission = MalmoPython.MissionSpec(mission_xml, True)
my_mission_record = malmoutils.get_default_recording_object(agent_host, "chat_recording")

# Attempt to start a mission:
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

# Loop until mission starts:
print("Waiting for the mission to start ", end=' ')
world_state = agent_host.getWorldState()
while not world_state.has_mission_begun:
    print(".", end="")
    time.sleep(0.1)
    world_state = agent_host.getWorldState()
    for error in world_state.errors:
        print("Error:",error.text)

print()
print("Mission running ", end=' ')

if world_state.is_mission_running:
    time.sleep(0.5)
    print("\nSending action: chat %s" % items[obj_id])
    agent_host.sendCommand("chat %s" % items[obj_id])
    time.sleep(1.5)

# Loop until mission ends:
while world_state.is_mission_running:
    print(".", end="")
    time.sleep(0.5)
    world_state = agent_host.getWorldState()
    for reward in world_state.rewards:
        if reward.getValue() > 0:
            print("\nReceived reward: %.2f" % reward.getValue())
    for error in world_state.errors:
        print("Error:",error.text)

print()
print("Mission ended")
# Mission has ended.
