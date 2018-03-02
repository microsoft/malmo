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

# Quit command example

from builtins import range
import MalmoPython
import os
import sys
import time
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)

missionXML='''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
            <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            
              <About>
                <Summary>If at first you don't succeed, give up.</Summary>
              </About>
              
              <ServerSection>
                <ServerHandlers>
                  <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1"/>
                  <ServerQuitFromTimeUp timeLimitMs="30000"/>
                  <ServerQuitWhenAnyAgentFinishes/>
                </ServerHandlers>
              </ServerSection>
              
              <AgentSection mode="Survival">
                <Name>QuitBot</Name>
                <AgentStart/>
                <AgentHandlers>
                  <ObservationFromFullStats/>
                  <ContinuousMovementCommands turnSpeedDegs="180"/>
                  <ChatCommands />
                  <MissionQuitCommands quitDescription="give_up"/>
                  <RewardForMissionEnd>
                    <Reward description="give_up" reward="-1000"/>
                  </RewardForMissionEnd>''' + malmoutils.get_video_xml(agent_host) + '''
                </AgentHandlers>
              </AgentSection>
            </Mission>'''

# Create default Malmo objects:

my_mission = MalmoPython.MissionSpec(missionXML, True)
my_mission_record = malmoutils.get_default_recording_object(agent_host, "Mission_1")

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

count = 0

# Loop until mission ends:
while world_state.is_mission_running:
    print(".", end="")
    time.sleep(0.5)
    if count == 10:
        print()
        print("Giving up!")
        agent_host.sendCommand("quit")
    count += 1
    world_state = agent_host.getWorldState()
    for error in world_state.errors:
        print("Error:",error.text)
    for reward in world_state.rewards:
        print("Reward:",reward.getValue())
print()
print("Mission ended")
# Mission has ended.
