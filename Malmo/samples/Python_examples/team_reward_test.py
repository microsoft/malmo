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

# A sample that demonstrates the use of reward sharing.
# Two agents:
#   Simeon the Stylite (https://en.wikipedia.org/wiki/Simeon_Stylites)
#   Fred Dibnah (https://en.wikipedia.org/wiki/Fred_Dibnah)

# Goal: Simeon must safely walk from his starting point to a goal square...
# but his starting point is at the top of a pole, 21 blocks above ground level, and the goal
# is at the bottom of a shaft, 20 blocks below ground level.
# Simeon is only allowed to send two types of command: chat commands, and a continuous move command.
# Fred only receives chat observations.

# Reward structure: Simeon receives a reward of 1 whenever Fred sends a command.
# Fred receives a reward of 10000 when Simeon reaches the goal.

# One solution implemented here:
# Fred follows instructions sent to it in the form of chat messages.
# Simeon instructs Fred to dig/build a staircase that enables him to walk straight down to the goal.

import MalmoPython
import json
import logging
import math
import os
import random
import sys
import time
import re

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

# -- set up two agent hosts --
agent_host_simeon = MalmoPython.AgentHost()
agent_host_fred = MalmoPython.AgentHost()

try:
    agent_host_simeon.parse( sys.argv )
except RuntimeError as e:
    print 'ERROR:',e
    print agent_host_simeon.getUsage()
    exit(1)
if agent_host_simeon.receivedArgument("help"):
    print agent_host_simeon.getUsage()
    exit(0)


# -- set up the mission --
xml = '''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <About>
    <Summary/>
  </About>
  <ModSettings>
    <MsPerTick>10</MsPerTick>   <!-- Because it's pretty boring watching Fred build steps for five minutes... -->
  </ModSettings>
  <ServerSection>
    <ServerInitialConditions>
      <Time>
        <StartTime>0</StartTime>
      </Time>
    </ServerInitialConditions>
    <ServerHandlers>
      <FlatWorldGenerator forceReset="true" generatorString="3;2*4,225*22;1;" seed=""/>
      <DrawingDecorator>
        <DrawCuboid x1="-2" y1="247" z1="-2" x2="-2" y2="227" z2="-2" type="stained_glass" colour="PINK"/>
        <DrawCuboid x1="-44" y1="227" z1="-2" x2="-44" y2="206" z2="-2" type="air"/>
        <DrawBlock x="-44" y="206" z="-2" type="redstone_block"/>
      </DrawingDecorator>
      <ServerQuitFromTimeUp description="" timeLimitMs="500000"/>
      <ServerQuitWhenAnyAgentFinishes description=""/>
    </ServerHandlers>
  </ServerSection>

  <AgentSection mode="Survival">
    <Name>SimeonTheStylite</Name>
    <AgentStart>
      <Placement x="-1.5" y="249.0" z="-1.5" pitch="60" yaw="90"/>
    </AgentStart>
    <AgentHandlers>
      <ContinuousMovementCommands>
        <ModifierList type="allow-list">
          <command>move</command>
        </ModifierList>
      </ContinuousMovementCommands>
      <ChatCommands/>
      <RewardForTouchingBlockType>
        <Block reward="10000" type="redstone_block" distribution="FredDibnah:1"/>
      </RewardForTouchingBlockType>
    </AgentHandlers>
  </AgentSection>

  <AgentSection mode="Survival">
    <Name>FredDibnah</Name>
    <AgentStart>
      <Placement x="-22.5" y="227.0" z="-1.5" pitch="90" yaw="180"/>
    </AgentStart>
    <AgentHandlers>
      <DiscreteMovementCommands autoJump="true" autoFall="true"/>
      <ObservationFromChat/>
      <RewardForSendingCommand reward="1" distribution="SimeonTheStylite:1"/>
    </AgentHandlers>
  </AgentSection>
</Mission>'''

my_mission = MalmoPython.MissionSpec(xml,True)

client_pool = MalmoPython.ClientPool()
client_pool.add( MalmoPython.ClientInfo('127.0.0.1',10000) )
client_pool.add( MalmoPython.ClientInfo('127.0.0.1',10001) )

# Fred's instructions for building a staircase, calculated in advance:
instructions=[]
for x in xrange(20):
    instructions.append("move -1")
    for y in xrange(x+1):
        instructions.append("strafe -1")
    instructions.append("move 1")
    for y in xrange(x+1):
        instructions.append("attack")
    for y in xrange(x+1):
        instructions.append("strafe 1")
    instructions.append("move 1")
    for y in xrange(x+1):
        instructions.append("strafe 1")
    instructions.append("move -1")
    for y in xrange(x+1):
        instructions.append("jumpuse")
    for y in xrange(x+1):
        instructions.append("strafe -1")

expected_reward_simeon = len(instructions)  # One point for every instruction acted on.

agent_host_simeon.startMission( my_mission, client_pool, MalmoPython.MissionRecordSpec(), 0, '' )
time.sleep(10)
agent_host_fred.startMission( my_mission, client_pool, MalmoPython.MissionRecordSpec(), 1, '' )

for agent_host in [ agent_host_simeon, agent_host_fred ]:
    print "Waiting for the mission to start",
    world_state = agent_host.peekWorldState()
    while not world_state.has_mission_begun:
        sys.stdout.write(".")
        time.sleep(0.1)
        world_state = agent_host.peekWorldState()
        for error in world_state.errors:
            print "Error:",error.text
    print

time.sleep(1)
i = 0
sendNewInstruction = True
reward_fred = 0
reward_simeon = 0

# wait for the missions to end
# For convenience, this loop contains all logic for both agents.
# In a real scenario, each agent could have its own process, or at least thread.
while agent_host_simeon.peekWorldState().is_mission_running or agent_host_fred.peekWorldState().is_mission_running:
    if sendNewInstruction:
        print "Sending command:", instructions[i]
        agent_host_simeon.sendCommand("chat " + instructions[i])
        sendNewInstruction = False

    # Check to see if Simeon has received a reward:
    if agent_host_simeon.peekWorldState().number_of_rewards_since_last_state > 0:
        world_state = agent_host_simeon.getWorldState()
        for reward in world_state.rewards:
            reward_simeon += reward.getValue()
        # Simeon's reward structure is such that he only gets rewards when Fred sends a command,
        # and Fred only sends a command when Simeon has sent a chat message.
        # So the presence of a reward signal indicates that it's time for the next chat message.
        i += 1
        print "Ready for next command"
        if i < len(instructions):
            sendNewInstruction = True
        else:
            agent_host_simeon.sendCommand("move 1") # Steps should be complete - let's walk!

    world_state = agent_host_fred.getWorldState()

    if world_state.number_of_rewards_since_last_state > 0:
        for reward in world_state.rewards:
            reward_fred += reward.getValue()

    for obs in world_state.observations:
        print "Observation!"
        msg = obs.text
        ob = json.loads(msg)
        chat = ob.get(u'Chat', "")
        for command in chat:
            parts = command.split("> ")
            if len(parts) > 1:
                agent_host_fred.sendCommand(str(parts[1]))

# check the rewards obtained
world_state1 = agent_host_simeon.getWorldState()
world_state2 = agent_host_fred.getWorldState()
reward_simeon += sum(reward.getValue() for reward in world_state1.rewards)
reward_fred += sum(reward.getValue() for reward in world_state2.rewards)
print 'Simeon received',reward_simeon
print 'Fred received',reward_fred