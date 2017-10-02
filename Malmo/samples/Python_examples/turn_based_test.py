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

from builtins import range
description_text='''
This python script is designed to test the turn scheduler, which was introduced to enable the
creation of turn-based multi-agent scenarios. The turn scheduler forces agents to take turns in
sending commands. If a command is sent when it's not an agent's turn, that command will be ignored.
To enforce this behaviour, the agent must send a "key" with each command. Commands with a missing
or incorrect key are rejected. The key is a randomly generated string, and each key is only good for
one command. The keys are provided through the observation json, and are only sent to an agent when it
is that agent's turn. In this test there are three agents, running in separate threads. Each agent simply
moves forward one square whenever it is their "go", and the agent which reaches the ends of the track
first wins. All agents start the same distance from the goal, so, obviously, the agent which starts first
should always win. The "TurnBasedCommands" section of the agent's XML allows the play order to be defined -
each agent gets to request a position in the play order. [Obviously, if all players request to go first,
the scheduler will only be able to honour one of those requests. For details of what happens when the turn
schedule is poorly specified, see addUsernameToTurnSchedule and saveTurnSchedule in the WaitingForAgentsEpisode
section of ServerStateMachine.java.]
This script, therefore, tests that whichever agent goes first always wins. It also checks that the agents are
taking their turns in the correct order by using this piece of text: before the mission starts, the text is
"demuxed" into three separate lists, and each agent is given a list. When each agent takes their turn, they
spit out the next word of their list. Assuming the agents take their turns in the correct order, this text
should be successfully reconstructed. Any turns taken out of order will result in words appearing out of order.
So if you find you can read this, things working are.'''

reconstructed_text=""

import MalmoPython
import os
import sys
import time
import json
import threading
import xml.etree.ElementTree

TESTING = False
agent_host = MalmoPython.AgentHost()    # Purely for parsing command line
try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print('ERROR:',e)
    print(agent_host.getUsage())
    exit(1)
if agent_host.receivedArgument("help"):
    print(agent_host.getUsage())
    exit(0)
if agent_host.receivedArgument("test"):
    TESTING = True

def GetMissionXML(summary):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>''' + summary + '''</Summary>
        </About>

        <ModSettings>
            <MsPerTick>20</MsPerTick>
        </ModSettings>
        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,6*103;2;" forceReset="false"/>
                <DrawingDecorator>
                    <DrawCuboid x1="-2" y1="5" z1="-1" x2="1" y2="10" z2="114" type="air"/>
                    <DrawCuboid x1="-2" y1="6" z1="-1" x2="2" y2="7" z2="114" type="iron_block"/>
                    <DrawCuboid x1="-1" y1="7" z1="0" x2="1" y2="7" z2="113" type="obsidian"/>
                    <DrawCuboid x1="-1" y1="7" z1="0" x2="1" y2="7" z2="0" type="emerald_block"/>
                    <DrawCuboid x1="-1" y1="7" z1="113" x2="1" y2="7" z2="113" type="redstone_block"/>
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="150000"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Fayette</Name>
            <AgentStart>
                <Placement x="-0.5" y="10.0" z="0.5"/>
                <Inventory>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullStats/>
                <TurnBasedCommands requestedPosition="1">
                    <DiscreteMovementCommands/>
                </TurnBasedCommands>
                <AgentQuitFromTouchingBlockType>
                    <Block type="redstone_block" description="Fayette won!"/>
                </AgentQuitFromTouchingBlockType>
                <RewardForTouchingBlockType>
                    <Block type="redstone_block" reward="100"/>
                </RewardForTouchingBlockType>
            </AgentHandlers>
        </AgentSection>

        <AgentSection mode="Survival">
            <Name>Valerie</Name>
            <AgentStart>
                <Placement x="0.5" y="10.0" z="0.5"/>
                <Inventory>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullStats/>
                <TurnBasedCommands requestedPosition="2">
                    <DiscreteMovementCommands/>
                </TurnBasedCommands>
                <AgentQuitFromTouchingBlockType>
                    <Block type="redstone_block" description="Valerie won!"/>
                </AgentQuitFromTouchingBlockType>
                <RewardForTouchingBlockType>
                    <Block type="redstone_block" reward="100"/>
                </RewardForTouchingBlockType>
            </AgentHandlers>
        </AgentSection>

        <AgentSection mode="Survival">
            <Name>Sheila</Name>
            <AgentStart>
                <Placement x="1.5" y="10.0" z="0.5"/>
                <Inventory>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullStats/>
                <TurnBasedCommands requestedPosition="3">
                    <DiscreteMovementCommands/>
                </TurnBasedCommands>
                <AgentQuitFromTouchingBlockType>
                    <Block type="redstone_block" description="Sheila won!"/>
                </AgentQuitFromTouchingBlockType>
                <RewardForTouchingBlockType>
                    <Block type="redstone_block" reward="100"/>
                </RewardForTouchingBlockType>
            </AgentHandlers>
        </AgentSection>
    </Mission>'''

class ThreadedAgent(threading.Thread):
    def __init__(self, role, clientPool, missionXML):
        threading.Thread.__init__(self)
        self.role = role
        self.client_pool = clientPool
        self.mission_xml = missionXML
        self.agent_host = MalmoPython.AgentHost()
        #self.agent_host.setDebugOutput(False)
        self.mission = MalmoPython.MissionSpec(missionXML, True)
        self.mission_record = MalmoPython.MissionRecordSpec()
        self.reward = 0
        self.mission_end_message = ""
        self.error = None

    def setWords(self, words):
        self.words = words

    def run(self):
        max_retries = 10
        attempt = 0
        while True:
            try:
                # Attempt to start the mission. This can throw with a number of different errors - see the MissionErrorCodes.
                self.agent_host.startMission(self.mission, self.client_pool, self.mission_record, self.role, "TurnBasedTest")
                break
            except MalmoPython.MissionException as e:
                # The only error codes we want to handle are those relating to availability of resources.
                if e.details.errorCode in [MalmoPython.MissionErrorCode.MISSION_INSUFFICIENT_CLIENTS_AVAILABLE,
                                           MalmoPython.MissionErrorCode.MISSION_SERVER_NOT_FOUND]:
                    # 1: Insufficient_clients means that not enough Minecraft instances are running;
                    # 2: Server_not_found means that the agent responsible for creating the server (ie role 0) hasn't yet called startMission.
                    # We respond to both of these cases by waiting and retrying - but not indefinitely, since there is
                    # no guarantee that either of these problems will be remedied (eg the user might never launch another Minecraft instance.)
                    attempt += 1
                    if attempt == max_retries:
                        self.error = "Failed to start mission after " + str(max_retries) + " attempts."
                        exit(1)
                    time.sleep(1)
                elif e.details.errorCode == MalmoPython.MissionErrorCode.MISSION_SERVER_WARMING_UP:
                    # 3: Server_warming_up means that role 0 has called startMission, but the integrated server hasn't yet
                    # come online. We wait for this indefinitely, since (unless something goes wrong) it should only be a matter
                    # of letting Minecraft get itself ready.
                    time.sleep(1)
                else:
                    # Any other errors - eg garbled mission requests - can not be solved by simply waiting, so bail now.
                    self.error = "Fatal error with mission:", e
                    exit(1)

        world_state = self.agent_host.getWorldState()
        max_seconds_to_wait = 20
        while not world_state.has_mission_begun and len(world_state.errors) == 0:
            time.sleep(0.1)
            max_seconds_to_wait -= 0.1
            if max_seconds_to_wait <= 0:
                self.error = "Timed out waiting for other agents."
                exit(1)
            world_state = self.agent_host.getWorldState()

        if len(world_state.errors) > 0:
            self.error = "Mission failed to start while waiting for other agents: "
            for e in world_state.errors:
                self.error += e.text
            exit(1)

        self.runMissionLoop()

    def runMissionLoop(self):
        global reconstructed_text
        turn_key = ""
        while (True):
            world_state = self.agent_host.getWorldState()
            if not world_state.is_mission_running:
                break
            if world_state.number_of_observations_since_last_state > 0:
                obvsText = world_state.observations[-1].text
                data = json.loads(obvsText) # observation comes in as a JSON string...
                new_turn_key = data.get(u'turn_key', "")
                turn_index = data.get(u'turn_number',0)
                if len(new_turn_key) > 0 and new_turn_key != turn_key:
                    # Our turn to take a move!
                    # First, print our word. We do this *before* calling sendCommand
                    # to ensure thread-safety; no other agent will be able to print
                    # anything until we've sent our command.
                    if turn_index <= len(self.words):
                        word = self.words[turn_index - 1]
                        reconstructed_text += word + " "
                        print(word, end=' ')
                    self.agent_host.sendCommand("move 1", str(new_turn_key))
                    turn_key = new_turn_key
            time.sleep(0.001) # Helps python thread scheduler if we sleep a bit

        if len(world_state.rewards):
            self.reward = world_state.rewards[-1].getValue()

        # Parse the MissionEnded XML messasge:
        if len(world_state.mission_control_messages):
            mission_end_tree = xml.etree.ElementTree.fromstring(world_state.mission_control_messages[-1].text)
            ns_dict = {"malmo":"http://ProjectMalmo.microsoft.com"}
            hr_stat = mission_end_tree.find("malmo:HumanReadableStatus", ns_dict).text
            if hr_stat and len(hr_stat):
                self.mission_end_message = hr_stat
        # Close our agent hosts:
        time.sleep(2)
        self.agent_host = None

if sys.version_info[0] == 2:
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
else:
    import functools
    print = functools.partial(print, flush=True)

# Create a pool of Minecraft Mod clients.
# By default, mods will choose consecutive mission control ports, starting at 10000,
# so running four mods locally should produce the following pool by default (assuming nothing else
# is using these ports):
my_client_pool = MalmoPython.ClientPool()
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10000))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10001))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10002))

words = description_text.split()
# Pad to a multiple of num_agents:
if len(words) % 3 > 0:
    for i in range(3-(len(words)%3)):
        words.append("----")
full_text = " ".join(words)

iterations = 10 if TESTING else 30000
for mission_no in range(iterations):
    reconstructed_text = ""
    mission_xml = GetMissionXML("Race!")
    agents = [ThreadedAgent(0, my_client_pool, mission_xml),
              ThreadedAgent(1, my_client_pool, mission_xml),
              ThreadedAgent(2, my_client_pool, mission_xml)]

    num_agents = len(agents)

    for i in range(num_agents):
        stream = [words[j] for j in range(i, len(words), num_agents)]
        agents[i].setWords(stream)

    for agent in agents:
        agent.start()

    for agent in agents:
        agent.join()

    print()
    num_errors = 0
    for agent in agents:
        print(agent.role, agent.reward, agent.mission_end_message)
        if agent.error:
            print("ERROR FROM AGENT", agent.role, ":", agent.error)
            num_errors += 1
    if TESTING and num_errors:
        exit(1)

    reconstructed_text = reconstructed_text.strip() # Deal with trailing space.
    if full_text != reconstructed_text:
        print("ERROR!")
        print("Expected: ", full_text)
        print("Received: ", reconstructed_text)
        if TESTING:
            exit(1)
