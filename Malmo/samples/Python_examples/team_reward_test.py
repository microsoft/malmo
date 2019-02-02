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

from builtins import range
import MalmoPython
import json
import logging
import math
import os
import random
import sys
import time
import re
import malmoutils

malmoutils.fix_print()

def safeStartMission(agent_host, my_mission, my_client_pool, my_mission_record, role, expId):
    used_attempts = 0
    max_attempts = 5
    print("Calling startMission for role", role)
    while True:
        try:
            # Attempt start:
            agent_host.startMission(my_mission, my_client_pool, my_mission_record, role, expId)
            break
        except MalmoPython.MissionException as e:
            errorCode = e.details.errorCode
            if errorCode == MalmoPython.MissionErrorCode.MISSION_SERVER_WARMING_UP:
                print("Server not quite ready yet - waiting...")
                time.sleep(2)
            elif errorCode == MalmoPython.MissionErrorCode.MISSION_INSUFFICIENT_CLIENTS_AVAILABLE:
                print("Not enough available Minecraft instances running.")
                used_attempts += 1
                if used_attempts < max_attempts:
                    print("Will wait in case they are starting up.", max_attempts - used_attempts, "attempts left.")
                    time.sleep(2)
            elif errorCode == MalmoPython.MissionErrorCode.MISSION_SERVER_NOT_FOUND:
                print("Server not found - has the mission with role 0 been started yet?")
                used_attempts += 1
                if used_attempts < max_attempts:
                    print("Will wait and retry.", max_attempts - used_attempts, "attempts left.")
                    time.sleep(2)
            else:
                print("Other error: ", str(e))
                print("Waiting will not help here - bailing immediately.")
                exit(1)
        if used_attempts == max_attempts:
            print("All chances used up - bailing now.")
            exit(1)
    print("startMission called okay.")

def safeWaitForStart(agent_hosts):
    print("Waiting for the mission to start", end=' ')
    start_flags = [False for a in agent_hosts]
    start_time = time.time()
    time_out = 120  # Allow a two minute timeout.
    while not all(start_flags) and time.time() - start_time < time_out:
        states = [a.peekWorldState() for a in agent_hosts]
        start_flags = [w.has_mission_begun for w in states]
        errors = [e for w in states for e in w.errors]
        if len(errors) > 0:
            print("Errors waiting for mission start:")
            for e in errors:
                print(e.text)
            print("Bailing now.")
            exit(1)
        time.sleep(0.1)
        print(".", end=' ')
    if time.time() - start_time >= time_out:
        print("Timed out while waiting for mission to start - bailing.")
        exit(1)
    print()
    print("Mission has started.")

# -- set up two agent hosts --
agent_host_simeon = MalmoPython.AgentHost()
agent_host_fred = MalmoPython.AgentHost()

# Use simeon's agenthost to hold the command-line options:
malmoutils.parse_command_line(agent_host_simeon)

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
      <ServerQuitFromTimeUp description="" timeLimitMs="5000000"/>
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
      <AgentQuitFromTouchingBlockType>
        <Block type="redstone_block"/>
      </AgentQuitFromTouchingBlockType>
      <ColourMapProducer>
        <Width>860</Width>
        <Height>480</Height>
      </ColourMapProducer>
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
      <DepthProducer>
        <Width>860</Width>
        <Height>480</Height>
      </DepthProducer>
    </AgentHandlers>
  </AgentSection>
</Mission>'''

my_mission = MalmoPython.MissionSpec(xml,True)

client_pool = MalmoPython.ClientPool()
client_pool.add( MalmoPython.ClientInfo('127.0.0.1',10000) )
client_pool.add( MalmoPython.ClientInfo('127.0.0.1',10001) )

# Fred's instructions for building a staircase, calculated in advance:
instructions=[]
for x in range(20):
    instructions.append("move -1")
    for y in range(x+1):
        instructions.append("strafe -1")
    instructions.append("move 1")
    for y in range(x+1):
        instructions.append("attack")
    for y in range(x+1):
        instructions.append("strafe 1")
    instructions.append("move 1")
    for y in range(x+1):
        instructions.append("strafe 1")
    instructions.append("move -1")
    for y in range(x+1):
        instructions.append("jumpuse")
    for y in range(x+1):
        instructions.append("strafe -1")

expected_reward_simeon = len(instructions)  # One point for every instruction acted on.
expected_reward_fred = 10000    # Reward when Simeon touches redstone block.

recordingsDirectory = malmoutils.get_recordings_directory(agent_host_simeon)
simeon_recording_spec = MalmoPython.MissionRecordSpec()
fred_recording_spec = MalmoPython.MissionRecordSpec()
if recordingsDirectory:
    # Command-line arguments requested a recording, so set it up:
    simeon_recording_spec.setDestination(recordingsDirectory + "//simeon_viewpoint.tgz")
    simeon_recording_spec.recordRewards()
    simeon_recording_spec.recordCommands()
    simeon_recording_spec.recordObservations()
    fred_recording_spec.setDestination(recordingsDirectory + "//fred_viewpoint.tgz")
    fred_recording_spec.recordRewards()
    fred_recording_spec.recordCommands()
    fred_recording_spec.recordObservations()
    if agent_host_simeon.receivedArgument("record_video"):
        # Simeon sees the world as a colour map:
        simeon_recording_spec.recordMP4(MalmoPython.FrameType.COLOUR_MAP, 24, 2000000, False)
        # Fred sees the world as a 32bpp depth map:
        fred_recording_spec.recordBitmaps(MalmoPython.FrameType.DEPTH_MAP)

safeStartMission(agent_host_simeon, my_mission, client_pool, simeon_recording_spec, 0, '' )
safeStartMission(agent_host_fred, my_mission, client_pool, fred_recording_spec, 1, '' )
safeWaitForStart([ agent_host_simeon, agent_host_fred ])

i = 0
sendNewInstruction = True
reward_fred = 0
reward_simeon = 0

# wait for the missions to end
# For convenience, this loop contains all logic for both agents.
# In a real scenario, each agent could have its own process, or at least thread.
while agent_host_simeon.peekWorldState().is_mission_running or agent_host_fred.peekWorldState().is_mission_running:
    if sendNewInstruction:
        print("Sending command:", instructions[i])
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
        print("Ready for next command")
        if i < len(instructions):
            sendNewInstruction = True
        else:
            agent_host_simeon.sendCommand("move 1") # Steps should be complete - let's walk!

    world_state = agent_host_fred.getWorldState()

    if world_state.number_of_rewards_since_last_state > 0:
        for reward in world_state.rewards:
            reward_fred += reward.getValue()

    for obs in world_state.observations:
        print("Observation!")
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
print('Simeon received {} (expected {})'.format(reward_simeon, expected_reward_simeon))
print('Fred received {} (expected {})'.format(reward_fred, expected_reward_fred))

if agent_host_simeon.receivedArgument("test") and (reward_fred != expected_reward_fred or reward_simeon != expected_reward_simeon):
    print("Rewards don't match - test failed!")
    exit(1)
