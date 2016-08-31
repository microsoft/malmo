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

# A sample that demonstrates a two-agent mission with discrete actions to dig and place blocks

import MalmoPython
import json
import logging
import math
import os
import random
import sys
import time

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

# -- set up two agent hosts --
agent_host1 = MalmoPython.AgentHost()
agent_host2 = MalmoPython.AgentHost()

try:
    agent_host1.parse( sys.argv )
except RuntimeError as e:
    print 'ERROR:',e
    print agent_host1.getUsage()
    exit(1)
if agent_host1.receivedArgument("help"):
    print agent_host1.getUsage()
    exit(0)


# -- set up the mission --
xml = '''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <About>
    <Summary/>
  </About>
  <ServerSection>
    <ServerInitialConditions>
      <Time>
        <StartTime>0</StartTime>
      </Time>
    </ServerInitialConditions>
    <ServerHandlers>
      <FlatWorldGenerator forceReset="true" generatorString="3;7,220*1,5*3,2;3;,biome_1" seed=""/>
      <ServerQuitFromTimeUp description="" timeLimitMs="10000"/>
      <ServerQuitWhenAnyAgentFinishes description=""/>
    </ServerHandlers>
  </ServerSection>

  <AgentSection mode="Survival">
    <Name>Ant</Name>
    <AgentStart>
      <Placement x="-1.5" y="227.0" z="0.5" pitch="30" yaw="0"/>
    </AgentStart>
    <AgentHandlers>
      <DiscreteMovementCommands/>
      <RewardForCollectingItem>
        <Item reward="1" type="dirt"/>
      </RewardForCollectingItem>
      <RewardForDiscardingItem>
        <Item reward="10" type="dirt"/>
      </RewardForDiscardingItem>
    </AgentHandlers>
  </AgentSection>

  <AgentSection mode="Survival">
    <Name>Bee</Name>
    <AgentStart>
      <Placement x="1.5" y="227.0" z="6.5" pitch="30" yaw="180"/>
    </AgentStart>
    <AgentHandlers>
      <DiscreteMovementCommands/>
      <RewardForCollectingItem>
        <Item reward="10" type="dirt"/>
      </RewardForCollectingItem>
      <RewardForDiscardingItem>
        <Item reward="100" type="dirt"/>
      </RewardForDiscardingItem>
    </AgentHandlers>
  </AgentSection>
  
</Mission>'''
my_mission = MalmoPython.MissionSpec(xml,True)

client_pool = MalmoPython.ClientPool()
client_pool.add( MalmoPython.ClientInfo('127.0.0.1',10000) )
client_pool.add( MalmoPython.ClientInfo('127.0.0.1',10001) )

agent_host1.startMission( my_mission, client_pool, MalmoPython.MissionRecordSpec(), 0, '' )
time.sleep(10)
agent_host2.startMission( my_mission, client_pool, MalmoPython.MissionRecordSpec(), 1, '' )

for agent_host in [ agent_host1, agent_host2 ]:
    print "Waiting for the mission to start",
    world_state = agent_host.peekWorldState()
    while not world_state.has_mission_begun:
        sys.stdout.write(".")
        time.sleep(0.1)
        world_state = agent_host.peekWorldState()
        for error in world_state.errors:
            print "Error:",error.text
    print

# perform a few actions
reps = 3
time.sleep(1)
for i in xrange(reps):
    agent_host1.sendCommand('attack 1')
    agent_host2.sendCommand('attack 1')
    time.sleep(1)
    agent_host1.sendCommand('use 1')
    agent_host2.sendCommand('use 1')
    time.sleep(1)
    
# wait for the missions to end    
while agent_host1.peekWorldState().is_mission_running or agent_host2.peekWorldState().is_mission_running:
    time.sleep(1)

# check the rewards obtained
expected_reward1 = reps*1  + reps*10  # reward of 1 for collecting, 10 for discarding
expected_reward2 = reps*10 + reps*100 # reward of 10 for collecting, 100 for discarding
world_state1 = agent_host1.getWorldState()
world_state2 = agent_host2.getWorldState()
reward1 = sum(reward.getValue() for reward in world_state1.rewards)
reward2 = sum(reward.getValue() for reward in world_state2.rewards)
print 'Agent 1 received',reward1
print 'Agent 2 received',reward2
assert reward1 == expected_reward1, 'ERROR: agent 1 should have received a reward of '+str(expected_reward1)+', not '+str(reward1)
assert reward2 == expected_reward2, 'ERROR: agent 2 should have received a reward of '+str(expected_reward2)+', not '+str(reward2)

# -- set up another mission, with continuous actions --

xml = '''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <About>
    <Summary/>
  </About>
  <ServerSection>
    <ServerInitialConditions>
      <Time>
        <StartTime>0</StartTime>
      </Time>
    </ServerInitialConditions>
    <ServerHandlers>
      <FlatWorldGenerator forceReset="true" generatorString="3;7,220*1,5*3,2;3;,biome_1" seed=""/>
      <DrawingDecorator>
        <DrawBlock type="sand" x="-2" y="227" z="3" />
        <DrawBlock type="sponge" x="1" y="227" z="3" />
      </DrawingDecorator>
      <ServerQuitFromTimeUp description="" timeLimitMs="10000"/>
      <ServerQuitWhenAnyAgentFinishes description=""/>
    </ServerHandlers>
  </ServerSection>

  <AgentSection mode="Survival">
    <Name>Ant</Name>
    <AgentStart>
      <Placement x="-1.5" y="227.0" z="0.5" pitch="25" yaw="0"/>
    </AgentStart>
    <AgentHandlers>
      <ContinuousMovementCommands/>
      <RewardForCollectingItem>
        <Item reward="1" type="sand"/>
      </RewardForCollectingItem>
      <RewardForDiscardingItem>
        <Item reward="10" type="sand"/>
      </RewardForDiscardingItem>
    </AgentHandlers>
  </AgentSection>

  <AgentSection mode="Survival">
    <Name>Bee</Name>
    <AgentStart>
      <Placement x="1.5" y="227.0" z="6.5" pitch="25" yaw="180"/>
    </AgentStart>
    <AgentHandlers>
      <ContinuousMovementCommands/>
      <RewardForCollectingItem>
        <Item reward="10" type="sponge"/>
      </RewardForCollectingItem>
      <RewardForDiscardingItem>
        <Item reward="100" type="sponge"/>
      </RewardForDiscardingItem>
    </AgentHandlers>
  </AgentSection>
  
</Mission>'''
my_mission = MalmoPython.MissionSpec(xml,True)

client_pool = MalmoPython.ClientPool()
client_pool.add( MalmoPython.ClientInfo('127.0.0.1',10000) )
client_pool.add( MalmoPython.ClientInfo('127.0.0.1',10001) )

agent_host1.startMission( my_mission, client_pool, MalmoPython.MissionRecordSpec(), 0, '' )
time.sleep(10)
agent_host2.startMission( my_mission, client_pool, MalmoPython.MissionRecordSpec(), 1, '' )

for agent_host in [ agent_host1, agent_host2 ]:
    print "Waiting for the mission to start",
    world_state = agent_host.peekWorldState()
    while not world_state.has_mission_begun:
        sys.stdout.write(".")
        time.sleep(0.1)
        world_state = agent_host.peekWorldState()
        for error in world_state.errors:
            print "Error:",error.text
    print

# perform a few actions
time.sleep(1)
agent_host1.sendCommand('attack 1')
agent_host2.sendCommand('attack 1')
time.sleep(1)
agent_host1.sendCommand('attack 0')
agent_host2.sendCommand('attack 0')
agent_host1.sendCommand('move 1')
agent_host2.sendCommand('move 1')
time.sleep(1)
agent_host1.sendCommand('move 0')
agent_host2.sendCommand('move 0')
agent_host1.sendCommand('use 1')
agent_host2.sendCommand('use 1')
time.sleep(1)
agent_host1.sendCommand('use 0')
agent_host2.sendCommand('use 0')
    
# wait for the missions to end    
while agent_host1.peekWorldState().is_mission_running or agent_host2.peekWorldState().is_mission_running:
    time.sleep(1)

# check the rewards obtained
expected_reward1 = 1 + 10   # reward of 1 for collecting, 10 for discarding
expected_reward2 = 10 + 100 # reward of 10 for collecting, 100 for discarding
world_state1 = agent_host1.getWorldState()
world_state2 = agent_host2.getWorldState()
reward1 = sum(reward.getValue() for reward in world_state1.rewards)
reward2 = sum(reward.getValue() for reward in world_state2.rewards)
print 'Agent 1 received',reward1
print 'Agent 2 received',reward2
assert reward1 == expected_reward1, 'ERROR: agent 1 should have received a reward of '+str(expected_reward1)+', not '+str(reward1)
assert reward2 == expected_reward2, 'ERROR: agent 2 should have received a reward of '+str(expected_reward2)+', not '+str(reward2)
