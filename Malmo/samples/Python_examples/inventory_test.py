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

import MalmoPython
import json
import math
import os
import random
import sys
import time
from collections import namedtuple

InventoryObject = namedtuple('InventoryObject', 'type, colour, variation, quantity, inventory, index')
InventoryObject.__new__.__defaults__ = ("", "", "", 0, "", 0)

missionXML = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Chests and Stuff</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,44*49,73,35:1,159:4,95:13,35:13,159:11,95:10,159:14,159:6,35:6,95:6;157;decoration,lake" />
                <DrawingDecorator>
                    <DrawContainer x="0" y="56" z="422" type="chest">
                        <object type="boat" quantity="4"/>
                        <object type="paper" quantity="1"/>
                        <object type="bucket" quantity="100"/>
                    </DrawContainer>
                    <DrawContainer x="0" y="56" z="423" type="dispenser">
                        <object type="apple" quantity="100"/>
                        <object type="cake" quantity="100"/>
                    </DrawContainer>
                    <DrawContainer x="0" y="56" z="424" type="dropper">
                        <object type="skull" quantity="100"/>
                        <object type="map" quantity="100"/>
                    </DrawContainer>
                    <DrawContainer x="0" y="56" z="425" type="ender_chest">
                        <object type="rabbit" quantity="100"/>
                        <object type="carrot_on_a_stick" quantity="100"/>
                    </DrawContainer>
                    <DrawContainer x="0" y="56" z="426" type="hopper">
                        <object type="quartz" quantity="10"/>
                        <object type="diamond" quantity="10"/>
                        <object type="prismarine" quantity="10"/>
                    </DrawContainer>
                    <DrawContainer x="0" y="56" z="427" type="red_shulker_box">
                        <object type="black_shulker_box" quantity="100"/>
                    </DrawContainer>
                    <DrawContainer x="0" y="56" z="428" type="ender_chest">
                        <object type="black_shulker_box" quantity="100"/>
                    </DrawContainer>
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="60000" description="out_of_time"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Lovejoy</Name>
            <AgentStart>
                <Placement x="0.5" y="56.0" z="420.5"/>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullInventory flat="false"/>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
my_mission = MalmoPython.MissionSpec(missionXML,True)
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

my_mission_record = MalmoPython.MissionRecordSpec()
max_retries = 3
for retry in range(max_retries):
    try:
        agent_host.startMission( my_mission, my_mission_record )
        break
    except RuntimeError as e:
        if retry == max_retries - 1:
            print "Error starting mission",e
            print "Is the game running?"
            exit(1)
        else:
            time.sleep(2)

world_state = agent_host.peekWorldState()
while not world_state.has_mission_begun:
    time.sleep(0.1)
    world_state = agent_host.peekWorldState()
    
while world_state.is_mission_running:
    world_state = agent_host.peekWorldState()
    if world_state.number_of_observations_since_last_state > 0:
        obs = json.loads(world_state.observations[-1].text)
        inv = [InventoryObject(**k) for k in obs[u'inventory']]
        for i in inv:
            print i.quantity, " x ", i.type, i.inventory
        #print obs

# mission has ended.
print "Mission over - feel free to explore the world."
