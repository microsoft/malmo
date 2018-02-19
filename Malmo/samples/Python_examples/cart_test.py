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

# Silly test of:
#   Minecart track shaping
#   Entity drawing
#   Step shaping
#
# If everything is working correctly, the agent should ride around happily forever.

from builtins import range
import MalmoPython
import os
import random
import sys
import time
import json
import random
import re
import math

# For a bit more fun, set MOB_TYPE = "Creeper"...
MOB_TYPE = "Villager"

def drawrailline(x1, z1, x2, z2, y):
    ''' Draw a powered rail between the two points '''
    y1railsegment = '" y1="' + str(y + 1) + '" z1="'
    y2railsegment = '" y2="' + str(y + 1) + '" z2="'
    y1stonesegment = '" y1="' + str(y) + '" z1="'
    y2stonesegment = '" y2="' + str(y) + '" z2="'
    shape = "north_south"
    if z1 == z2:
        shape = "east_west"
    rail_line = '<DrawLine x1="' + str(x1) + y1railsegment + str(z1) + '" x2="' + str(x2) + y2railsegment + str(z2) + '" type="golden_rail" variant="' + shape + '"/>'
    redstone_line = '<DrawLine x1="' + str(x1) + y1stonesegment + str(z1) + '" x2="' + str(x2) + y2stonesegment + str(z2) + '" type="redstone_block" />'
    return redstone_line + rail_line

def drawstepline(x1, z1, x2, z2, y, type, facing, half):
    ''' Draw a line of steps '''
    y1 = '" y1="' + str(y) + '" z1="'
    y2 = '" y2="' + str(y) + '" z2="'
    return '<DrawLine x1="' + str(x1) + y1 + str(z1) + '" x2="' + str(x2) + y2 + str(z2) + '" type="' + type + '" face="' + facing + '" variant="' + half + '"/>'

def drawblock(x, y, z):
    ''' Draw a corner piece of rail '''
    shape = ""
    if z < 0:
        if x > 0:
            shape="south_west"
        else:
            shape="south_east"
    else:
        if x > 0:
            shape="north_west"
        else:
            shape="north_east"

    yrailsegment = '" y="' + str(y + 1) + '" z="'
    ystonesegment = '" y="' + str(y) + '" z="'

    return '<DrawBlock x="' + str(x) + ystonesegment + str(z) + '" type="redstone_block"/>' + \
           '<DrawBlock x="' + str(x) + yrailsegment + str(z) + '" type="rail" variant="' + shape + '"/>'
    
def drawloop(radius, y):
    ''' Create a loop of powered rail '''
    return drawrailline(-radius, 1-radius, -radius, radius-1, y) + \
           drawrailline(radius, 1-radius, radius, radius-1, y) + \
           drawrailline(1-radius, radius, radius-1, radius, y) + \
           drawrailline(1-radius, -radius, radius-1, -radius, y) + \
           drawblock(radius, y, radius) + drawblock(-radius, y, radius) + drawblock(-radius, y, -radius) + drawblock(radius, y, -radius)

def drawloops(radius, y):
    ''' Draw a set of concentric loops '''
    loops=""
    for i in range(radius, 1, -2):
        loops += drawloop(i, y)
        y += 1
    return loops

def drawstep(radius, y, type, half):
    ''' Draw a loop of steps '''
    e="EAST"
    w="WEST"
    n="NORTH"
    s="SOUTH"
    if half == "top":
        n,s = s,n
        e,w = w,e
    return drawstepline(-radius, -radius, -radius, radius, y, type, e, half) + \
           drawstepline(radius, -radius, radius, radius, y, type, w, half) + \
           drawstepline(1-radius, radius, radius-1, radius, y, type, n, half) + \
           drawstepline(1-radius, -radius, radius-1, -radius, y, type, s, half)

def drawsteps(radius, y):
    ''' Draw a pyramid of steps '''
    steps = ""
    for i in range(radius, 1, -2):
        steps += drawstep(i, y, "quartz_stairs", "bottom")
        steps += drawstep(i, y-1, "dark_oak_stairs", "top")
        y += 1
    return steps

def drawlinks(radius, y):
    ''' Link each circuit of powered rail into the adjacent inner loop '''
    links=""
    for i in range(radius, 1, -2):
        links += drawlink(i, y)
        y += 1
    return links

def drawlink(radius, y):
    ''' Create a link from one circuit to the inner circuit '''
    # first remove four rail blocks:
    x=str(radius-3)
    link = ""
    if radius > 3:  # Don't do all of this for the innermost loop
        link='<DrawCuboid x1="' + x + '" y1="' + str(y) + '" z1="' + str(radius) + '" x2="' + x + '" y2="' + str(y+1) + '" z2="' + str(radius-2) + '" type="air"/>'
        link='<DrawBlock x="' + str(radius-4) + '" y="' + str(y) + '" z="' + str(radius-2) + '" type="air"/>'
        # add the link blocks:
        link += '<DrawBlock x="' + x + '" y="' + str(y) + '" z="' + str(radius-1) + '" type="redstone_block"/>'
        link += '<DrawBlock x="' + x + '" y="' + str(y + 1) +'" z="' + str(radius-1) + '" type="golden_rail" variant="ascending_north"/>'
        link += '<DrawBlock x="' + x + '" y="' + str(y + 2) +'" z="' + str(radius-2) + '" type="rail" variant="south_east"/>'
    link += '<DrawBlock x="' + x + '" y="' + str(y + 1) +'" z="' + str(radius) + '" type="rail" variant="north_west"/>'
    return link

def drawHilbert(level, x, y, z):
    ''' Draw a Hilbert Curve out of Minecraft railtrack '''
    # L-System production rules:
    #   "L" = turn left 90 degrees
    #   "R" = turn right 90 degrees
    #   "F" = forward
    a = "LBFRAFARFBL"
    b = "RAFLBFBLFAR"
    
    rep_dict = {'A':a, 'B':b}
    expand_pattern = re.compile('A|B')
    expand_func = lambda match: rep_dict[match.group(0)]

    # Initialise curve:
    curve = a
    
    # Perform repeated substitutions:
    for i in range(level):
        curve = expand_pattern.sub(expand_func, curve)

    # Remove the fluff:
    curve = expand_pattern.sub("",curve)

    # Concatenate the turns (not really necessary, but saves time in the next step)
    reduce_pattern = re.compile(u"[LR]+")
    reduce_func = lambda match: "" if len(match.group(0)) % 2 == 0 else match.group(0)[0]
    curve = reduce_pattern.sub(reduce_func, curve)

    # Now convert into drawing XML:
    xml=""
    dir = 0
    prev_dir = dir
    deltax = [0,1,0,-1]
    deltaz = [-1,0,1,0]
    dir_vars = ["north_south", "south_east", "north_south", "south_west",
                "north_west", "east_west", "south_west", "east_west",
                "north_south", "north_east", "north_south", "north_west",
                "north_east", "east_west", "south_east", "east_west"]
    for c in curve:
        if c == 'L':
            dir = (dir - 1) % 4
        elif c == 'R':
            dir = (dir + 1) % 4
        elif c == 'F':
            # print x, z, dir_vars[dir + prev_dir * 4]
            rail_type = "rail" if dir != prev_dir else "golden_rail"
            xml += '<DrawBlock x="' + str(x) + '" y="' + str(y) + '" z="' + str(z) + '" type="' + rail_type + '" variant="' + dir_vars[dir + prev_dir * 4] + '"/>'
            x += deltax[dir]
            z += deltaz[dir]
            prev_dir = dir
    
    return xml

def calcYawToMob(entities, x, y, z):
    ''' Find the mob we are following, and calculate the yaw we need in order to face it '''
    for ent in entities:
        if ent['name'] == MOB_TYPE:
            dx = ent['x'] - x
            dz = ent['z'] - z
            yaw = -180 * math.atan2(dx, dz) / math.pi
            return yaw
    return 0

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

endCondition = ""
timeoutCondition = ""
if agent_host.receivedArgument("test"):
    # If the rail drawing has worked, the agent will eventually reach the diamond and quit.
    # (This takes around four minutes in Minecraft time)
    # If not, it will eventually time out, and the test will fail.
    timeoutCondition = '<ServerQuitFromTimeUp timeLimitMs="240000" description="FAILED"/>'
    endCondition = '''<AgentQuitFromCollectingItem>
                        <Item type="diamond" description="PASSED"/>
                      </AgentQuitFromCollectingItem>'''

missionXML = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Goin' Cartin'</Summary>
        </About>
        
        <ModSettings>
            <MsPerTick>25</MsPerTick>
        </ModSettings>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,56*35:9,36;" forceReset="true" />
                <DrawingDecorator>
                    <DrawCuboid x1="-20" y1="34" z1="-20" x2="20" y2="57" z2="20" type="air"/>
                    <!-- <DrawCuboid x1="-20" y1="55" z1="-20" x2="20" y2="55" z2="20" type="obsidian"/> -->

                    <!-- Draw the torches along the side walls -->
                    <DrawLine x1="-20" y1="37" z1="-20" x2="-20" y2="37" z2="20" type="torch" face="EAST"/>
                    <DrawLine x1="20" y1="37" z1="-20" x2="20" y2="37" z2="20" type="torch" face="WEST"/>
                    <DrawLine x1="-20" y1="37" z1="-20" x2="20" y2="37" z2="-20" type="torch" face="SOUTH"/>
                    <DrawLine x1="-20" y1="37" z1="20" x2="20" y2="37" z2="20" type="torch" face="NORTH"/>

                    <!-- Rail from the centre of the ground-floor to the start of the Hilbert Curve -->
                    <DrawLine x1="-1" y1="64" z1="2" x2="-1" y2="43" z2="-19" type="redstone_block"/>
                    <DrawLine x1="-1" y1="65" z1="2" x2="-1" y2="44" z2="-19" type="golden_rail"/>
                    <DrawBlock x="-1" y="43" z="-20" type="redstone_block"/>
                    <DrawBlock x="-1" y="44" z="-20" type="rail"/>
                    <DrawLine x1="0" y1="43" z1="-20" x2="19" y2="34" z2="-20" type="redstone_block"/>
                    <DrawLine x1="0" y1="44" z1="-20" x2="19" y2="35" z2="-20" type="golden_rail"/>
                    <DrawBlock x="20" y="34" z="-20" type="redstone_block"/>
                    <DrawBlock x="20" y="35" z="-20" type="rail"/>
                    <DrawLine x1="20" y1="34" z1="-19" x2="20" y2="34" z2="-16" type="golden_rail"/>
                    <DrawBlock x="20" y="34" z="-15" type="rail"/>
                    <DrawLine x1="19" y1="34" z1="-15" x2="16" y2="34" z2="-15" type="golden_rail"/>

                    <!-- Rail from end of Hilbert Curve back to surface level -->
                    <DrawLine x1="16" y1="33" z1="17" x2="16" y2="36" z2="20" type="redstone_block"/>
                    <DrawLine x1="16" y1="34" z1="17" x2="16" y2="37" z2="20" type="golden_rail"/>
                    <DrawBlock x="16" y="37" z="20" type="rail"/>
                    <DrawLine x1="15" y1="37" z1="20" x2="-35" y2="58" z2="20" type="air"/>
                    <DrawLine x1="15" y1="38" z1="20" x2="-35" y2="59" z2="20" type="air"/>
                    <DrawLine x1="15" y1="39" z1="20" x2="-35" y2="60" z2="20" type="air"/>
                    <DrawLine x1="15" y1="36" z1="20" x2="-35" y2="57" z2="20" type="redstone_block"/>
                    <DrawLine x1="15" y1="37" z1="20" x2="-35" y2="58" z2="20" type="golden_rail"/>

                    <!-- Rail from surface level back to spiral -->
                    <DrawBlock x="-36" y="57" z="20" type="redstone_block"/>
                    <DrawBlock x="-36" y="58" z="20" type="rail"/>
                    <DrawBlock x="-36" y="57" z="19" type="redstone_block"/>
                    <DrawBlock x="-36" y="58" z="19" type="rail"/>
                    <DrawLine x1="-35" y1="57" z1="19" x2="-23" y2="57" z2="19" type="redstone_block"/>
                    <DrawLine x1="-23" y1="57" z1="20" x2="-21" y2="57" z2="20" type="redstone_block"/>
                    <DrawLine x1="-35" y1="58" z1="19" x2="-24" y2="58" z2="19" type="golden_rail"/>
                    <DrawBlock x="-23" y="58" z="19" type="rail"/>
                    <DrawBlock x="-23" y="58" z="20" type="rail"/>
                    <DrawLine x1="-22" y1="58" z1="20" x2="-21" y2="58" z2="20" type="golden_rail"/>
                    <DrawItem x="-21" y="60" z="20" type="diamond"/>

                    <!-- Draw spiral -->
                    ''' + drawloops(20,55) + drawsteps(19,56) + drawlinks(20,55) + '''

                    <!-- Hole in the middle -->
                    <DrawCuboid x1="-1" y1="55" z1="1" x2="0" y2="54" z2="3" type="air"/>

                    <!-- And the Hilbert Curve in the basement -->
                    <DrawCuboid x1="-20" y1="33" z1="-20" x2="20" y2="33" z2="20" type="redstone_block"/>
                    ''' + drawHilbert(4,16,34,16) + '''
                    
                    <!-- Give the player a Minecart -->
                    <DrawEntity x="18.5" y="56" z="20.5" type="MinecartRideable"/>
                    
                    <!-- And something amusing to follow -->
                    <DrawEntity x="20.5" y="56" z="16.5" xVel="0" yVel="0" zVel="-1" type="MinecartRideable"/>
                    <DrawEntity x="20.5" y="56" z="16.5" type="''' + MOB_TYPE + '''"/>
                </DrawingDecorator>''' + timeoutCondition + '''
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>The Explorer</Name>
            <AgentStart>
                <Placement x="18.5" y="56.0" z="20.5" yaw="270" pitch="60"/>
            </AgentStart>
            <AgentHandlers>
                <ContinuousMovementCommands turnSpeedDegs="420"/>
                <ObservationFromNearbyEntities>
                    <Range name="entities" xrange="40" yrange="40" zrange="40"/>
                </ObservationFromNearbyEntities>
                <ObservationFromFullStats/>''' + endCondition + '''
                <RewardForMissionEnd rewardForDeath="-100.0">
                    <Reward description="PASSED" reward="100.0"/>
                    <Reward description="FAILED" reward="-100.0"/>
                </RewardForMissionEnd>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

if sys.version_info[0] == 2:
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
else:
    import functools
    print = functools.partial(print, flush=True)
my_mission = MalmoPython.MissionSpec(missionXML,True)

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

world_state = agent_host.getWorldState()
while not world_state.has_mission_begun:
    time.sleep(0.1)
    world_state = agent_host.getWorldState()

agent_host.sendCommand("use 1")     # Click on the minecart...
agent_host.sendCommand("use 0")
agent_host.sendCommand("move 1")    # And start moving forward...
agent_host.sendCommand("pitch -0.1")    # Look up
yaw_to_mob = 0

while world_state.is_mission_running:
    world_state = agent_host.getWorldState()
    if world_state.number_of_observations_since_last_state > 0:
        obvsText = world_state.observations[-1].text
        data = json.loads(obvsText) # observation comes in as a JSON string...
        current_x = data.get(u'XPos', 0)
        current_z = data.get(u'ZPos', 0)
        current_y = data.get(u'YPos', 0)
        yaw = data.get(u'Yaw', 0)
        pitch = data.get(u'Pitch', 0)
        # Try to look somewhere interesting:
        if "entities" in data:
            yaw_to_mob = calcYawToMob(data['entities'], current_x, current_y, current_z)
        if pitch < 0:
            agent_host.sendCommand("pitch 0") # stop looking up
        # Find shortest angular distance between the two yaws, preserving sign:
        deltaYaw = yaw_to_mob - yaw
        while deltaYaw < -180:
            deltaYaw += 360;
        while deltaYaw > 180:
            deltaYaw -= 360;
        deltaYaw /= 180.0;
        # And turn:
        agent_host.sendCommand("turn " + str(deltaYaw))

# mission has ended.
print("Mission over")
reward = world_state.rewards[-1].getValue()
print("Result: " + str(reward))
if reward < 0:
    exit(1)
else:
    exit(0)
