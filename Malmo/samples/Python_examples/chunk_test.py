from __future__ import print_function
from __future__ import division
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

# Test force-loading of the initial chunk - see:
#       https://github.com/Microsoft/malmo/issues/338
#       https://github.com/Microsoft/malmo/issues/195
#       https://github.com/Microsoft/malmo/pull/327

# If the spawn point is a long way from the mission's start position, the Minecraft client can be slow to load the starting chunk.
# This led to problems with agents falling into the scenery, missing observations, etc.
# Force-loading the initial chunk should fix that.

# This tests the force-loading by running missions with random start points (x and z vary between +- 10000),
# using the drawing decorator to build a structure at the start point, and using the grid observer to test that the
# structure matches what should have been drawn.
# If the initial observation is incorrect, then the test fails.

from builtins import range
from past.utils import old_div
import MalmoPython
import os
import random
import sys
import time
import json
import copy
import malmoutils

malmoutils.fix_print()

# Create default Malmo objects:
agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)
recordingsDirectory = malmoutils.get_recordings_directory(agent_host)

# Set up some pallettes:
colourful=["stained_glass", "diamond_block", "emerald_block", "gold_block", "redstone_block", "obsidian"]
fiery=["stained_glass WHITE", "stained_glass PINK", "stained_glass ORANGE", "stained_glass RED", "wool BLACK", "glowstone"]
oresome=["gold_ore", "lapis_ore", "iron_ore", "emerald_ore", "redstone_ore", "quartz_ore"]
frilly=["web", "stained_glass WHITE", "wool PINK", "wool WHITE", "stained_hardened_clay PINK", "stained_hardened_clay WHITE"]
icepalace=["ice", "air", "air", "air", "air", "snow"]
volatile=["tnt", "air", "air", "redstone_block", "air", "air"]
oak=["planks", "planks", "planks", "planks", "lapis_block", "lapis_block"]
sponge=["sponge", "glass", "sponge", "glass", "sponge", "glass"]
palletes = [colourful, fiery, oresome, frilly, icepalace, volatile, oak, sponge]

# dimensions of the test structure:
SIZE_X = 21
SIZE_Y = 21
SIZE_Z = 21

def createTestStructure(sx, sy, sz):
    # Obviously we could just plonk random blocks around the place, but this is much prettier...
    r = 0.5
    # Initialise our structure matrix, and an empty copy:
    s = [[[(1 if random.random() >= r else 0) for x in range(sx)] for y in range(sy)] for z in range(sz)]
    t = [[[0 for x in range(sx)] for y in range(sy)] for z in range(sz)]
    # Some useful lambdas:
    xtrim = lambda x: x % sx if x > 0 else x
    ytrim = lambda y: y % sy if y > 0 else y
    ztrim = lambda z: z % sz if z > 0 else z
    neighbours = lambda x,y,z: [s[xtrim(a)][ytrim(b)][ztrim(c)] for a in range(x-1, x+2) for b in range(y-1, y+2) for c in range(z-1, z+2)]

    colour = False
    iterations = random.randint(10,25)

    # Run a cellular automata for a few iterations:
    for i in range(iterations):
        if i == iterations - 1:
            colour = True   # Final iteration: don't apply the CA, just colour the cell.
        for x in range(sx):
            for y in range(sy):
                for z in range(sz):
                    tot = sum(neighbours(x,y,z)) - s[x][y][z]
                    result = s[x][y][z]
                    if colour:
                        # 'colour' the cell according to how many neighbours it has.
                        result = tot
                    else:
                        # CA rules:
                        #   cell is born if it has 13,14,17,18 or 19 neighbours.
                        #   cell survives if it has 13 or more neighbours. 
                        if result == 0:
                            if tot == 13 or tot == 14 or (tot >= 17 and tot <= 19):
                                result = 1
                        else:
                            if tot < 13:
                                result = 0
                    t[x][y][z] = result
        # copy next generation into source matrix.
        s = copy.deepcopy(t)
    return s

def structureToXMLAndJson(structure, xorg, yorg, zorg):
    # Take the structure and create both a drawing decorator, and a grid json array from it.
    pallette = random.choice(palletes)
    drawing = ""
    json = []
    for y in range(SIZE_Y):
        for z in range(SIZE_Z):
            for x in range(SIZE_X):
                value = structure[x][y][z]
                if value > 0:
                    type = pallette[old_div(value,5)]
                    parts = type.split()
                    type_string = ' type="' + parts[0] + '"'
                    if len(parts) > 1:
                        type_string += ' colour="' + parts[1] + '"'
                    drawing += '<DrawBlock x="' + str(x + xorg) + '" y="' + str(y + yorg) + '" z="' + str(z + zorg) + '" ' + type_string + '/>'
                    json.append(u"" + parts[0])
                else:
                    json.append(u"air")
    drawingdecorator = "<DrawingDecorator>"
    # "Blank out" the volume, in case if overlaps with old structures and throws the test.
    drawingdecorator += '<DrawCuboid x1="' + str(xorg) + '" y1="' + str(yorg) + '" z1="' + str(zorg) + '" x2="' + str(xorg + SIZE_X) + '" y2="' + str(yorg + SIZE_Y) + '" z2="' + str(zorg + SIZE_Z) + '" type="air"/>'
    drawingdecorator += drawing + "</DrawingDecorator>"
    return drawingdecorator, json

def getMissionXMLAndJson(forceReset, structure):
    # Choose a random starting position.
    # Draw a structure directly in front of the player.
    # Add a grid observation handler that will return the volume of the structure.
    xpos = int((random.random() - 0.5) * 20000)
    zpos = int((random.random() - 0.5) * 20000)
    structureXML, gridJson = structureToXMLAndJson(structure, xpos - int(old_div(SIZE_X, 2)), 1, zpos + 1)
    
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <About>
        <Summary>Test initial chunk</Summary>
    </About>
     
    <ServerSection>
        <ServerHandlers>
            <FlatWorldGenerator generatorString="3;7;8;" forceReset=''' + forceReset + '''/>''' + structureXML + '''
            <ServerQuitWhenAnyAgentFinishes />
        </ServerHandlers>
    </ServerSection>

    <AgentSection mode="Creative">
        <Name>Clive</Name>
        <AgentStart>
            <Placement x="''' + str(xpos + 0.5) + '''" y="1.0" z="''' + str(zpos + 0.5) + '''"/>
        </AgentStart>
        <AgentHandlers>
            <ContinuousMovementCommands />
            <MissionQuitCommands />
            <ObservationFromGrid>
                <Grid name="structure">
                    <min x="''' + str(-(old_div(SIZE_X, 2))) + '''" y="0" z="1"/>
                    <max x="''' + str(old_div(SIZE_X, 2)) + '''" y="''' + str(SIZE_Y-1) + '''" z="''' + str(SIZE_Z) + '''"/>
                </Grid>
            </ObservationFromGrid>
            <VideoProducer viewpoint="1">
                <Width>860</Width>
                <Height>480</Height>
            </VideoProducer>
        </AgentHandlers>
    </AgentSection>

  </Mission>''', gridJson

num_iterations = 30000
if agent_host.receivedArgument("test"):
    num_iterations = 10 # Haven't got all day

my_mission_record = MalmoPython.MissionRecordSpec()
if recordingsDirectory:
    my_mission_record.recordRewards()
    my_mission_record.recordObservations()
    my_mission_record.recordCommands()
    if agent_host.receivedArgument("record_video"):
        my_mission_record.recordMP4(24,2000000)

structure = createTestStructure(SIZE_X, SIZE_Y, SIZE_Z) # Create the first one outside the loop.

for i in range(num_iterations):
    if recordingsDirectory:
        my_mission_record.setDestination(recordingsDirectory + "//" + "Mission_" + str(i + 1) + ".tgz")

    missionXML, gridJson = getMissionXMLAndJson('"false"', structure)
    my_mission = MalmoPython.MissionSpec(missionXML, True)

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

    print("Beginning test " + str(i) + ".")
    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        print(".", end="")
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
    print()

    agent_host.sendCommand("pitch -0.1")    # Start looking up, because it's pretty.
    # main loop:
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_observations_since_last_state > 0:
            obs = json.loads( world_state.observations[-1].text )
            if "structure" in obs:
                struct = obs[u"structure"]
                if struct == gridJson:
                    print()
                    print("MATCHING - moving to next mission.")
                    print()
                    structure = createTestStructure(SIZE_X, SIZE_Y, SIZE_Z) # Create the next one while we enjoy this one.
                    agent_host.sendCommand("quit")
                    break
                else:
                    print()
                    print("No match - test failed on iteration " + str(i))
                    # Find the discrepancies:
                    index = 0
                    for y in range(SIZE_Y):
                        for z in range(SIZE_Z):
                            for x in range(SIZE_X):
                                expected = gridJson[index]
                                actual = struct[index]
                                if expected != actual:
                                    print("(" + str(x) + "," + str(y) + "," + str(z) + "), -" + actual + " +" + expected)
                                index += 1
                    agent_host.sendCommand("quit")
                    if agent_host.receivedArgument("test"):
                        exit(1) # Fail the test.
                    break
