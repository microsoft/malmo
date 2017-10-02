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

# Test of note blocks.
# Create three rows of noteblocks, tuned according to a random tone-row, and triggered by tripwires.
# As entities move around in the arena, they will play notes.
# Noteblocks use a different instrument sound depending on the block they sit on top of.
# Note that they need an air block directly above them in order to sound.
# For best results, adjust your Minecraft sound options - put Noteblocks on 100% and reduce everything else.

from builtins import range
import MalmoPython
import json
import math
import os
import random
import sys
import time

def genNoteblocks(x1, z1, x2, z2):
    pitches = [
        "F_sharp_3",
        "G3",
        "G_sharp_3",
        "A3",
        "A_sharp_3",
        "B3",
        "C4",
        "C_sharp_4",
        "D4",
        "D_sharp_4",
        "E4",
        "F4",
        "F_sharp_4",
        "G4",
        "G_sharp_4",
        "A4",
        "A_sharp_4",
        "B4",
        "C5",
        "C_sharp_5",
        "D5",
        "D_sharp_5",
        "E5",
        "F5",
        "F_sharp_5"]

    # Randomise the pitches:
    tone_row = pitches
    for i in range(len(tone_row)):
        s = random.randint(0, len(tone_row) - 1 - i)
        tone_row[i], tone_row[i + s] = tone_row[i + s], tone_row[i]

    # Draw wall around the playing arena:
    xml = '''<DrawCuboid x1="{left}" y1="227" z1="{front}" x2="{right}" y2="240" z2="{back}" type="stone"/>
             <DrawCuboid x1="{inner_left}" y1="227" z1="{inner_front}" x2="{inner_right}" y2="240" z2="{inner_back}" type="air"/>'''.format(left = x1 - 2, right = x2 + 1, front = z1 - 1, back = z2 + 1, inner_left = x1 - 1, inner_right = x2, inner_front = z1, inner_back = z2)

    # Draw the rows of triggers and supports:
    xml += '''<DrawLine x1="{left}" y1="{y_lower}" z1="{front}" x2="{right}" y2="{y_lower}" z2="{front}" type="lapis_block"/>
             <DrawLine x1="{left}" y1="{y_lower}" z1="{back}" x2="{right}" y2="{y_lower}" z2="{back}" type="lapis_block"/>
             <DrawLine x1="{left}" y1="{y_lower}" z1="{inner_back}" x2="{right}" y2="{y_lower}" z2="{inner_back}" type="tripwire_hook"/>
             <DrawLine x1="{left}" y1="{y_lower}" z1="{inner_front}" x2="{right}" y2="{y_lower}" z2="{inner_front}" type="tripwire_hook" face="SOUTH"/>
             <DrawLine x1="{left}" y1="{y_upper}" z1="{front}" x2="{right}" y2="{y_upper}" z2="{front}" type="soul_sand"/>
             <DrawLine x1="{left}" y1="{y_upper}" z1="{back}" x2="{right}" y2="{y_upper}" z2="{back}" type="soul_sand"/>
             <DrawLine x1="{left}" y1="{y_upper}" z1="{inner_back}" x2="{right}" y2="{y_upper}" z2="{inner_back}" type="tripwire_hook"/>
             <DrawLine x1="{left}" y1="{y_upper}" z1="{inner_front}" x2="{right}" y2="{y_upper}" z2="{inner_front}" type="tripwire_hook" face="SOUTH"/>'''.format(left = x1, right = x2, front = z1, back = z2, inner_front = z1 + 1, inner_back = z2 - 1, y_lower = 227, y_upper = 230)
    xml += '''<DrawLine x1="{left}" y1="{y_lower}" z1="{front}" x2="{left}" y2="{y_lower}" z2="{back}" type="planks"/>
             <DrawLine x1="{right}" y1="{y_lower}" z1="{front}" x2="{right}" y2="{y_lower}" z2="{back}" type="planks"/>
             <DrawLine x1="{inner_left}" y1="{y_lower}" z1="{front}" x2="{inner_left}" y2="{y_lower}" z2="{back}" type="tripwire_hook" face="EAST"/>
             <DrawLine x1="{inner_right}" y1="{y_lower}" z1="{front}" x2="{inner_right}" y2="{y_lower}" z2="{back}" type="tripwire_hook" face="WEST"/>'''.format(left = x1 - 1, inner_left = x1, right = x2 + 1, inner_right = x2, front = z1 + 1, back = z2 - 1, y_lower = 228)

    # Draw the trip wires and noteblocks:
    for x in range(x1, x2 + 1):
        xml += '<DrawLine x1="{x}" y1="{y_lower}" z1="{front}" x2="{x}" y2="{y_lower}" z2="{back}" type="tripwire"/>'.format(x = x, front = z1 + 2, back = z2 - 2, y_lower = 227)
        xml += '<DrawLine x1="{x}" y1="{y_upper}" z1="{front}" x2="{x}" y2="{y_upper}" z2="{back}" type="tripwire"/>'.format(x = x, front = z1 + 2, back = z2 - 2, y_upper = 230)

        xml += '<DrawBlock x="{x}" y="228" z="{front}" type="noteblock" variant="{pitch}"/>'.format(x = x, front = z1, pitch = tone_row[(x - x1) % 25])
        xml += '<DrawBlock x="{x}" y="231" z="{front}" type="noteblock" variant="{pitch}"/>'.format(x = x, front = z2, pitch = tone_row[(x - x1) % 25])
    for z in range(z1 + 1, z2):
        xml += '<DrawLine x1="{left}" y1="228" z1="{z}" x2="{right}" y2="228" z2="{z}" type="tripwire"/>'.format(left = x1 + 1, z = z, right = x2 - 1)
        xml += '<DrawBlock x="{x}" y="229" z="{z}" type="noteblock" variant="{pitch}"/>'.format(x = x1 - 1, z = z, pitch = tone_row[(z - z1) % 25])

    # Add some entities:
    for i in range(10):
        xml += '<DrawEntity x="{}" y="229" z="{}" type="{}"/>'.format(random.randint(x1, x2), random.randint(z1, z2), random.choice(["Rabbit","Rabbit","Rabbit", "Sheep"]))
    return xml

missionXML = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Mobophone</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <DrawingDecorator>''' + genNoteblocks(-20, -20, 20, 20) + '''</DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="240000" />
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Webern</Name>
            <AgentStart>
                <Placement x="0.5" y="227.0" z="0.5"/>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullStats/>
                <ContinuousMovementCommands turnSpeedDegs="420"/>
            </AgentHandlers>
        </AgentSection>
    </Mission>'''

if sys.version_info[0] == 2:
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
else:
    import functools
    print = functools.partial(print, flush=True)
my_mission = MalmoPython.MissionSpec(missionXML,True)
agent_host = MalmoPython.AgentHost()
try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print('ERROR:',e)
    print(agent_host.getUsage())
    exit(1)
if agent_host.receivedArgument("help"):
    print(agent_host.getUsage())
    exit(0)

my_mission_record = MalmoPython.MissionRecordSpec()
max_retries = 3
for retry in range(max_retries):
    try:
        agent_host.startMission( my_mission, my_mission_record )
        break
    except RuntimeError as e:
        if retry == max_retries - 1:
            print("Error starting mission",e)
            print("Is the game running?")
            exit(1)
        else:
            time.sleep(2)

world_state = agent_host.peekWorldState()
while not world_state.has_mission_begun:
    time.sleep(0.1)
    world_state = agent_host.peekWorldState()

# Just wander around randomly until we run out of time:
agent_host.sendCommand("move 1")
while world_state.is_mission_running:
    world_state = agent_host.peekWorldState()
    if random.random() > 0.96:
        agent_host.sendCommand("turn " + str(2 * random.random() - 1.0))
    if random.random() > 0.97:
        agent_host.sendCommand("jump 1")
    if random.random() > 0.85:
        agent_host.sendCommand("jump 0")
    time.sleep(0.1)
