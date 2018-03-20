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

# Tests discrete movement by navigating through a 3D structure, using the following action sets:
# - Discrete_NSEW: movenorth, moveeast, movesouth, movewest, jumpnorth, jumpeast, jumpsouth, jumpwest
# - Discrete_NSEW_autojump: movenorth, moveeast, movesouth, movewest
# - Discrete_move_turn: move, turn, jumpmove
# - Discrete_move_turn_autojump: move, turn
# - Discrete_move_strafe: move, strafe, jumpmove, jumpstrafe
# - Discrete_move_strafe_autojump: move, strafe
# - Continuous (just for fun) - move, strafe, jump

# The structure is created using a cellular automata, and the path through it is generated using
# a simple graph search (either BFS or DFS depending on how silly you like your tests).
# ObservationFromSubgoalPositionList is used to store the generated path, and we annotate the subgoals
# with helpful clues that the agent can use to navigate to the next point in the path.
# (This is especially useful for the continuous case. The discrete cases could be solved much more simply
# by just storing a list of the commands required, but doing it this way tests more things.)

from builtins import range
from past.utils import old_div
import MalmoPython
import os
import random
import sys
import time
import json
import copy
import errno
import xml.etree.ElementTree
from collections import deque
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)
recordingsDirectory = malmoutils.get_recordings_directory(agent_host)

# Set up some pallettes:
colourful=["stained_glass", "diamond_block", "lapis_block", "gold_block", "redstone_block", "obsidian"]
fiery=["stained_glass WHITE", "stained_glass PINK", "stained_glass ORANGE", "stained_glass RED", "wool BLACK", "glowstone"]
oresome=["gold_ore", "lapis_ore", "iron_ore", "emerald_ore", "redstone_ore", "quartz_ore"]
frilly=["skull", "stained_glass WHITE", "wool PINK", "wool WHITE", "stained_hardened_clay PINK", "stained_hardened_clay WHITE"]
icepalace=["ice", "stained_glass", "stained_glass", "stained_glass", "stained_glass", "snow"]
volatile=["tnt", "stained_glass", "stained_glass", "redstone_block", "stained_glass", "stained_glass"]
oak=["planks", "planks", "planks", "planks", "lapis_block", "lapis_block"]
sponge=["sponge", "glass", "sponge", "glass", "sponge", "glass"]
palletes = [colourful, fiery, oresome, frilly, icepalace, volatile, oak, sponge]

# dimensions of the test structure:
SIZE_X = 21
SIZE_Y = 31
SIZE_Z = 21

SHOW_COMMANDS = False

class Enum(tuple): __getattr__ = tuple.index

# different movement modes (see comments above):
MovementModes = Enum(['Continuous', 'Discrete_NSEW', 'Discrete_NSEW_autojump', 'Discrete_move_turn', 'Discrete_move_turn_autojump', 'Discrete_move_strafe', 'Discrete_move_strafe_autojump'])

# different path generation options:
PathTypes = Enum(['Silly', 'Sensible'])

def createTestStructure(sx, sy, sz):
    # Adapted from chunk_test.py
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
    iterations = random.randint(20,25)

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
    # Give it a floor:
    for x in range(sx):
        for z in range(sz):
            s[x][0][z] = 26

    return s

def makePath(s, xorg, yorg, zorg, path_type):
    # Build a graph of the structure, and attempt to find a nice path through it, to the lowest reachable point.

    # First create some lambdas that will help assess the reachability of each block:
    standable = lambda x,y,z: True if (x < 0 or x >= SIZE_X or z < 0 or z >= SIZE_Z) else (s[x][y][z] > 0 and (y + 1 >= SIZE_Y or s[x][y+1][z] == 0) and (y + 2 >= SIZE_Y or s[x][y+2][z] == 0))
    hoverable = lambda x,y,z: True if (x < 0 or x >= SIZE_X or z < 0 or z >= SIZE_Z) else ((y + 1 >= SIZE_Y or s[x][y+1][z] == 0) and (y + 2 >= SIZE_Y or s[x][y+2][z] == 0))
    jumpable = lambda x,y,z: True if (x < 0 or x >= SIZE_X or z < 0 or z >= SIZE_Z) else (s[x][y][z] > 0 and (y + 1 >= SIZE_Y or s[x][y+1][z] == 0) and (y + 2 >= SIZE_Y or s[x][y+2][z] == 0) and (y + 3 >= SIZE_Y or s[x][y+3][z] == 0))
    safeblock = lambda x,y,z: (x,y,z) if s[x][y][z] > 0 else (x,y-1,z) if s[x][y-1][z] > 0 else (x,y-2,z) if s[x][y-2][z] > 0 else (x,y-3,z) if s[x][y-3][z] > 0 else None
    neighbour = lambda x,y,z,dx,dz: safeblock(x+dx,y,z+dz) if (x+dx < SIZE_X and x+dx >= 0 and z+dz < SIZE_Z and z+dz >= 0) else None
    index = lambda x,y,z: x + (y * SIZE_X * SIZE_Z) + (z * SIZE_X)

    links = [[[[] for x in range(SIZE_X)] for y in range(SIZE_Y)] for z in range(SIZE_Z)]
    startpos = (0,0,0)

    # Build the graph:
    for x in range(SIZE_X):
        for y in range(SIZE_Y):
            for z in range(SIZE_Z):
                # Can we stand on this block?
                if not standable(x,y,z):
                    continue
                if y > startpos[1]:
                    startpos = (x,y,z)
                jumpy = jumpable(x,y,z) and y < SIZE_Y - 1
                # Enumerate accessible neighbours:
                for dx in range(-1,2):
                    for dz in range(-1,2):
                        if dx==0 and dz==0:
                            continue
                        if dx != 0 and dz != 0:
                            continue
                        if hoverable(x+dx,y,z+dz):
                            n = neighbour(x,y,z,dx,dz)
                            if n != None and standable(n[0],n[1],n[2]):
                                links[x][y][z].append(n)
                        if jumpy:
                            nu = neighbour(x,y+1,z,dx,dz)
                            if nu != None and nu[1] > y and standable(nu[0],nu[1],nu[2]):
                                links[x][y][z].append(nu)

    # Now search to lowest reachable point:
    queue = deque([startpos])
    points = [(False,0) for i in range(SIZE_X * SIZE_Y * SIZE_Z)]
    start_ind = index(startpos[0], startpos[1], startpos[2])
    points[start_ind] = (True,0)
    node = startpos
    endpos = startpos
    while len(queue):
        node = queue.popleft()
        x = node[0]
        y = node[1]
        z = node[2]
        if y < endpos[1] or (y == endpos[1] and abs(endpos[0]-startpos[0]) + abs(endpos[2]-startpos[2]) < abs(x-startpos[0]) + abs(z-startpos[2])):
            endpos = (x,y,z)    # Either deeper, or same level but further away.
        if y == 0:
            break

        ind = index(x,y,z)
        if x >= 0 and x < SIZE_X and y >= 0 and y < SIZE_Y and z >= 0 and z < SIZE_Z:
            neighbours = links[x][y][z]
            for n in neighbours:
                n_ind = index(n[0],n[1],n[2])
                if not points[n_ind][0]:
                    if path_type == PathTypes.Sensible:
                        queue.append(n) # Breadth-first.
                    else:    # PathTypes.Silly
                        queue.appendleft(n) # Depth-first - much less efficient, much more fun.
                    points[n_ind]=(True,ind)

    # Work backwards from end point to build path:
    path = []
    ind = index(endpos[0], endpos[1], endpos[2])
    while ind != start_ind:
        i = ind
        y = old_div(i, (SIZE_X * SIZE_Z))
        i -= y * SIZE_X * SIZE_Z
        z = old_div(i, SIZE_X)
        i -= z * SIZE_X
        x = i
        path.append((x + xorg, y + yorg, z + zorg))
        ind = points[ind][1]
    path.append((startpos[0] + xorg, startpos[1] + yorg, startpos[2] + zorg))
    return path

def structureToXML(structure, xorg, yorg, zorg, pallette):
    # Take the structure and create a drawing decorator for it.
    drawing = ""
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
    drawingdecorator = "<DrawingDecorator>"
    # "Blank out" the volume, in case if overlaps with old structures and throws the test.
    drawingdecorator += '<DrawCuboid x1="' + str(xorg) + '" y1="' + str(yorg) + '" z1="' + str(zorg) + '" x2="' + str(xorg + SIZE_X) + '" y2="' + str(yorg + SIZE_Y) + '" z2="' + str(zorg + SIZE_Z) + '" type="air"/>'
    drawingdecorator += drawing + "</DrawingDecorator>"
    return drawingdecorator

def getAnnotationAndTolerance(current, next, mode):
    # Depending on the movement mode, return an annotation that will help the agent know how to reach the next subgoal.
    if mode == MovementModes.Continuous:
        # Ignore x and z, just provide information about whether we are jumping or falling:
        if next[1] > current[1]:
            return "jump", 0.5
        elif next[1] < current[1]:
            return "drop", 0.5
        else:
            return "flat", 0.5
    elif mode in [MovementModes.Discrete_NSEW, MovementModes.Discrete_NSEW_autojump]:
        # In NSEW case, we can provide the exact command the agent needs to execute in order to get from current to next.
        desc_a = "move" if (next[1] <= current[1] or mode == MovementModes.Discrete_NSEW_autojump) else "jump"
        dx = next[0] - current[0]
        dz = next[2] - current[2]
        desc_b = ("north" if dz < 0 else "south") if dz != 0 else ("west" if dx < 0 else "east")
        return desc_a + desc_b, 0.0001
    elif mode in [MovementModes.Discrete_move_turn, MovementModes.Discrete_move_turn_autojump]:
        # Turning complicates things - agent will have to use the yaw delta etc, but we can tell it when to jump.
        description = "jump" if (next[1] > current[1] and mode == MovementModes.Discrete_move_turn) else ""
        return description, 0.0001
    elif mode in [MovementModes.Discrete_move_strafe, MovementModes.Discrete_move_strafe_autojump]:
        # With moving and strafing, can again give the exact command required.
        desc_a = "jump" if (next[1] > current[1] and mode == MovementModes.Discrete_move_strafe) else ""
        dx = next[0] - current[0]
        dz = next[2] - current[2]
        desc_b = "move " + str(dz) if dz != 0 else "strafe " + str(-dx)
        return desc_a + desc_b, 0.0001

def getMissionXML(forceReset, structure, mode, pathtype, mission_description):
    xpos = int((random.random() - 0.5) * 20000)
    zpos = int((random.random() - 0.5) * 20000)
    xorg = xpos - int(old_div(SIZE_X, 2))
    yorg = 1
    zorg = zpos + 1
    pallette = random.choice(palletes)
    quitxml = ""
    # In discrete mode, straying from the path represents an error.
    # Set up a quit handler to catch this error case.
    # (Continuous movement is a bit more flexible...)
    if mode != MovementModes.Continuous:
        quitxml = '<AgentQuitFromTouchingBlockType>'
        for block_type in set(pallette):
            parts = block_type.split()
            type_string = ' type="' + parts[0] + '"'
            if len(parts) > 1:
                type_string += ' colour="' + parts[1] + '"'
            quitxml += '<Block ' + type_string + ' description="ERROR: Strayed off path onto ' + block_type + '"/>'
        quitxml += '</AgentQuitFromTouchingBlockType>'
    structureXML = structureToXML(structure, xorg, yorg, zorg, pallette)
    path = makePath(structure, xorg, yorg, zorg, pathtype)
    startpos = path[-1]
    pathxml = '<DrawingDecorator>'
    subgoalxml = ""
    current_x = path[-1][0]
    current_y = path[-1][1]
    current_z = path[-1][2]
    for p in reversed(path):
        # We annotate each subgoal as a cheeky way to provide clues to the agent.
        # Do this differently, depending on the action space.
        description, tol = getAnnotationAndTolerance((current_x, current_y, current_z), p, mode)
        current_x = p[0]
        current_y = p[1]
        current_z = p[2]

        pathxml += '<DrawBlock x="' + str(p[0]) + '" y="' + str(p[1]) + '" z="' + str(p[2]) + '" type="emerald_block"/>'
        subgoalxml += '<Point x="' + str(p[0]+0.5) + '" y="' + str(p[1]) + '" z="' + str(p[2]+0.5) + '" tolerance="' + str(tol) + '" description="' + description + '" />'
    reward_item = "golden_apple" if path[0][1] == yorg else "apple"
    pathxml += '<DrawItem x="' + str(path[0][0]) + '" y="' + str(path[0][1]+1) + '" z="' + str(path[0][2]) + '" type="' + reward_item + '"/>'
    pathxml += '</DrawingDecorator>'
    commandxml = '<ContinuousMovementCommands/>'
    if mode != MovementModes.Continuous:
        commandxml = '<DiscreteMovementCommands autoFall="true" '
        if mode in [MovementModes.Discrete_NSEW_autojump, MovementModes.Discrete_move_turn_autojump, MovementModes.Discrete_move_strafe_autojump]:
            commandxml += 'autoJump="true" />'
        else:
            commandxml += '/>'

    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <About>
        <Summary>''' + mission_description + '''</Summary>
    </About>

    <ServerSection>
        <ServerHandlers>
            <FlatWorldGenerator generatorString="3;7;8;" forceReset=''' + forceReset + '''/>''' + structureXML + pathxml + '''
            <ServerQuitWhenAnyAgentFinishes />
            <ServerQuitFromTimeUp timeLimitMs="25000" description="Ran out of time."/>
        </ServerHandlers>
    </ServerSection>

    <AgentSection mode="Survival">
        <Name>Clive</Name>
        <AgentStart>
            <Placement x="''' + str(startpos[0] + 0.5) + '''" y="''' + str(startpos[1] + 1) + '''" z="''' + str(startpos[2] + 0.5) + '''" pitch="90"/>
        </AgentStart>
        <AgentHandlers>
            ''' + commandxml + '''
            <MissionQuitCommands />
            <ObservationFromFullStats/>
            <VideoProducer viewpoint="1">
                <Width>860</Width>
                <Height>480</Height>
            </VideoProducer>
            <ObservationFromSubgoalPositionList>''' + subgoalxml + '''</ObservationFromSubgoalPositionList>
            <AgentQuitFromCollectingItem>
                <Item type="golden_apple" description="Reached the ground!!"/>
                <Item type="apple" description="Went as far as we could."/>
            </AgentQuitFromCollectingItem>''' + quitxml + '''
            <RewardForCollectingItem>
                <Item type="golden_apple" reward="1000"/>
                <Item type="apple" reward="10"/>
            </RewardForCollectingItem>
        </AgentHandlers>
    </AgentSection>

  </Mission>'''

def sendCommand(ah, command):
    if SHOW_COMMANDS:
        print(command)
    ah.sendCommand(command)

def steerAgent(ah, description, dir, mode):
    # Send the relevant commands to steer the agent.
    # Return true if all the commands necessary to reach the next subgoal have been sent.
    move_values=[-1,0,1,0,-1]
    strafe_values=[0,-1,0,1,0]
    if mode == MovementModes.Continuous:
        speed = 0.8
        if description == "drop":
            speed = 0.4 # go slow for drops
        if description == "jump":
            sendCommand(ah, "jump 1")
        else:
            sendCommand(ah, "jump 0")
        sendCommand( ah, "move " + str(move_values[dir] * speed) )
        sendCommand( ah, "strafe " + str(strafe_values[dir] * speed) )
        return False
    elif mode in [MovementModes.Discrete_NSEW, MovementModes.Discrete_NSEW_autojump, MovementModes.Discrete_move_strafe, MovementModes.Discrete_move_strafe_autojump]:
        sendCommand(ah, str(description))
        return True
    elif mode in [MovementModes.Discrete_move_turn, MovementModes.Discrete_move_turn_autojump]:
        if move_values[dir] == 0:
            sendCommand(ah, "turn " + str(strafe_values[dir]))
            time.sleep(0.2)
            return False
        else:
            sendCommand(ah, str(description) + "move " + str(move_values[dir]))
            return True

num_iterations = 30000
if agent_host.receivedArgument("test"):
    num_iterations = len(MovementModes) * len(PathTypes) # Once through all combinations of path type and action space

recording = False
my_mission_record = MalmoPython.MissionRecordSpec()
if recordingsDirectory:
    recording = True
    my_mission_record.recordRewards()
    my_mission_record.recordObservations()
    my_mission_record.recordCommands()
    if agent_host.receivedArgument("record_video"):
        my_mission_record.recordMP4(24,2000000)

for i in range(num_iterations):
    structure = createTestStructure(SIZE_X, SIZE_Y, SIZE_Z)
    mode = i % len(MovementModes)
    pathtype = PathTypes.Silly if (old_div(i, len(MovementModes))) % 2 == 0 else PathTypes.Sensible
    description = "3D navtest #" + str(i+1) + "; Actions=" + MovementModes[mode] + "; Path=" + PathTypes[pathtype]
    missionXML = getMissionXML('"false"', structure, mode, pathtype, description)
    if recording:
        my_mission_record.setDestination(recordingsDirectory + "//" + "Mission_" + str(i+1) + ".tgz")
    my_mission = MalmoPython.MissionSpec(missionXML, True)

    print("")
    print("")
    print(description)
    print('=' * len(description))

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

    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        print(".", end="")
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
    print()

    moving = False
    last_subgoal = None
    # main loop:
    while world_state.is_mission_running:
        if world_state.number_of_observations_since_last_state > 0:
            msg = world_state.observations[-1].text
            ob = json.loads(msg)
            if 'nextSubgoal' in ob:
                moving = True
                subgoal = ob.get(u'nextSubgoal')
                if subgoal != last_subgoal:
                    description = subgoal[u'description']
                    dir = int(0.5 + (ob.get(u'yawDelta', 0) + 1) * 2)
                    doneWithSubgoal = steerAgent(agent_host, description, dir, mode)
                    if doneWithSubgoal:
                        last_subgoal = subgoal
            elif moving:
                agent_host.sendCommand("move 0")
                agent_host.sendCommand("strafe 0")
                agent_host.sendCommand("jump 0")
                moving = False
        world_state = agent_host.getWorldState()

    # Parse the MissionEnded XML messasge:
    mission_end_tree = xml.etree.ElementTree.fromstring(world_state.mission_control_messages[-1].text)
    ns_dict = {"malmo":"http://ProjectMalmo.microsoft.com"}
    stat = mission_end_tree.find("malmo:Status", ns_dict).text
    hr_stat = mission_end_tree.find("malmo:HumanReadableStatus", ns_dict).text
    print("Mission over. Status: ", stat, end=' ')
    if len(hr_stat):
        print(" - " + hr_stat)
    if agent_host.receivedArgument("test") and "ERROR" in hr_stat:
        exit(1)
