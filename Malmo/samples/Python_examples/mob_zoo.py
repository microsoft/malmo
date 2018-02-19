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

# Test blaze drawing, entity tracking etc.

from builtins import range
from past.utils import old_div
import MalmoPython
import os
import random
import sys
import time
import json
import random
import re
import math
from collections import namedtuple
import xml.etree.ElementTree

EntityInfo = namedtuple('EntityInfo', 'x, y, z, name')

hostileMobs =  [("ElderGuardian", "", 2),
                ("WitherSkeleton", "", 1.95),
                ("Stray", "", 1.95),
                ("Husk", "", 1.95),
                ("ZombieVillager", "", 1.95),
                ("SkeletonHorse", "", 1.6),
                ("ZombieHorse", "", 1.6),
                ("EvocationIllager", "", 1.95),
                ("VindicationIllager", "", 1.95),
                ("", '<DrawEntity x="{inner_left}" y="{inner_bottom}" z="{inner_back}" type="Vex"/>', 0.4),  # Vexes won't stay put, so draw one, but don't include its name in the list.
                ("Giant", "", 11.7),

                ("Skeleton", "", 2.0),
                ("Spider", "", 0.9),
                ("Creeper", "", 1.7),
                ("Zombie", "", 1.9),
                ("Slime", "", 0.5),
                ("Ghast", "", 4),
                ("PigZombie", "", 1.95),
                ("Enderman", "", 2.9),
                ("CaveSpider", '<DrawCuboid x1="{inner_left}" y1="{inner_bottom}" z1="{inner_front}" x2="{inner_right}" y2="{inner_bottom}" z2="{inner_back}" type="water"/>', 0.5),
                #Cave spider will die from repeatedly climbing/falling, unless we give him a cushion of water to fall into.
                ("Silverfish", "", 0.2),
                ("",'<DrawCuboid x1="{inner_left}" y1="{outer_bottom}" z1="{inner_front}" x2="{inner_right}" y2="{outer_bottom}" z2="{inner_back}" type="air"/>', 0), #Padding cell for the giant's torso
                
                ("Blaze", "", 1.8),
                ("LavaSlime", "", 0.4),
                ("EnderDragon", "", 5),
                ("WitherBoss", '<DrawCuboid x1="{outer_left}" y1="{inner_bottom}" z1="{inner_front}" x2="{outer_right}" y2="{inner_top}" z2="{inner_back}" type="bedrock"/><DrawCuboid x1="{inner_left}" y1="{inner_bottom}" z1="{inner_front}" x2="{inner_right}" y2="{inner_top}" z2="{inner_back}" type="air"/>', 3.5),
                ("Witch", '<DrawCuboid x1="{outer_left}" y1="{inner_bottom}" z1="{inner_front}" x2="{outer_left}" y2="{inner_top}" z2="{inner_back}" type="bedrock"/>', 1.95),
                ("Bat", "", 0.5),
                ("Endermite", "", 0.3),
                ("Guardian", "", 0.85),
                ("Shulker", "", 1.0),
                ("","",0), #Padding cell
                ("",'<DrawCuboid x1="{inner_left}" y1="{outer_bottom}" z1="{inner_front}" x2="{inner_right}" y2="{outer_bottom}" z2="{inner_back}" type="air"/>',0)] #Padding cell for the giant's head

friendlyMobs = [("Donkey", "", 1.6),
                ("Mule", "", 1.6),
                ("Pig", "", 0.9),
                ("Sheep", "", 1.3),
                ("Cow", "", 1.4),
                ("Chicken", "", 0.7),
                ("Squid", '<DrawCuboid x1="{inner_left}" y1="{inner_bottom}" z1="{inner_front}" x2="{inner_right}" y2="{inner_top}" z2="{inner_back}" type="water"/>', 0.8),
                ("Wolf", "", 0.85),
                ("MushroomCow", "", 1.4),
                ("SnowMan", "", 1.9),
                ("Ozelot", "", 0.7),
                ("VillagerGolem", "", 2.7),
                ("Horse", "", 1.6),
                ("Rabbit", "", 0.4),
                ("PolarBear", "", 1.4),
                ("Llama", "", 1.9),
                ("Villager", "", 1.95)]

def checkEnts(present_entities, required_entities):
    missing = []
    for ent in required_entities:
        if not ent in present_entities:
            missing.append(ent)
    if len(missing) > 0:
        print("Can't find:", missing)
        if TESTING:
            exit(1)

def angvel(target, current, scale):
    '''Use sigmoid function to choose a delta that will help smoothly steer from current angle to target angle.'''
    delta = target - current
    while delta < -180:
        delta += 360;
    while delta > 180:
        delta -= 360;
    return (old_div(2.0, (1.0 + math.exp(old_div(-delta,scale))))) - 1.0

def pointTo(agent_host, ob, target_pitch, target_yaw, threshold):
    '''Steer towards the target pitch/yaw, return True when within the given tolerance threshold.'''
    pitch = ob.get(u'Pitch', 0)
    yaw = ob.get(u'Yaw', 0)
    delta_yaw = angvel(target_yaw, yaw, 50.0)
    delta_pitch = angvel(target_pitch, pitch, 50.0)
    agent_host.sendCommand("turn " + str(delta_yaw))    
    agent_host.sendCommand("pitch " + str(delta_pitch))
    if abs(pitch-target_pitch) + abs(yaw-target_yaw) < threshold:
        agent_host.sendCommand("turn 0")
        agent_host.sendCommand("pitch 0")
        return True
    return False

def calcYawAndPitchToMob(target, x, y, z, target_height):
    dx = target.x - x
    dz = target.z - z
    yaw = -180 * math.atan2(dx, dz) / math.pi
    distance = math.sqrt(dx * dx + dz * dz)
    pitch = math.atan2(((y + 1.625) - (target.y + target_height * 0.9)), distance) * 180.0 / math.pi
    return yaw, pitch

agent_host = MalmoPython.AgentHost()
agent_host.addOptionalFlag("mayhem,m", "Remove the safety glass from the cages...")
try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print('ERROR:',e)
    print(agent_host.getUsage())
    exit(1)
if agent_host.receivedArgument("help"):
    print(agent_host.getUsage())
    exit(0)

rail_endpoints = []
cell_midpoints = []
endCondition = ""
timeoutCondition = ""
TESTING = agent_host.receivedArgument("test")
if TESTING:
    timeoutCondition = '<ServerQuitFromTimeUp timeLimitMs="240000" description="FAILED"/>'
    endCondition = '''<AgentQuitFromCollectingItem>
                        <Item type="diamond" description="PASSED"/>
                      </AgentQuitFromCollectingItem>'''
# If we're not testing, and the user is after a little mayhem, swap the safety glass for air and make it night time.
barrier_type = "air" if agent_host.receivedArgument("mayhem") and not TESTING else "barrier"
start_time = "13000" if agent_host.receivedArgument("mayhem") and not TESTING else "10000"

def getMissionXML(endCondition, timeoutCondition):
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Zoo!</Summary>
        </About>
        
        <ModSettings>
            <MsPerTick>50</MsPerTick>
        </ModSettings>

        <ServerSection>
            <ServerInitialConditions>
                <AllowSpawning>false</AllowSpawning>
                <Time>
                    <StartTime>''' + start_time + '''</StartTime>
                    <AllowPassageOfTime>false</AllowPassageOfTime>
                </Time>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,56*35:13;9,36;" forceReset="true" />''' \
                    + getZooXML(friendlyMobs, cells_across=6, cell_depth=6, cell_height=6, cell_width=7, orgx=18, orgy=56, orgz=5, left_padding = 18, right_padding = 17) \
                    + getZooXML(hostileMobs, cells_across=11, cell_depth=-6, cell_height=6, cell_width=7, orgx=0, orgy=60, orgz=-5) \
                    + getRailXML() \
                    + timeoutCondition + '''
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Gerald</Name>
            <AgentStart>
                <Placement x="0.5" y="58.0" z="4.5" yaw="-90" pitch="64"/>
            </AgentStart>
            <AgentHandlers>
                <ContinuousMovementCommands turnSpeedDegs="420"/>
                <ChatCommands/>
                <ObservationFromNearbyEntities>
                    <Range name="entities" xrange="300" yrange="60" zrange="60"/>
                </ObservationFromNearbyEntities>
                <ObservationFromFullStats/>''' + endCondition + '''
                <RewardForMissionEnd rewardForDeath="-100.0">
                    <Reward description="PASSED" reward="100.0"/>
                    <Reward description="FAILED" reward="-100.0"/>
                </RewardForMissionEnd>
            </AgentHandlers>
        </AgentSection>
    </Mission>'''

def getRailXML():
    railXML = "<DrawingDecorator>"
    if len(rail_endpoints) != 12:
        return ""   # Works on the assumption that both zoos have three levels
    for i in range(1, 6):
        a = rail_endpoints[i]
        b = rail_endpoints[i + 6] if i % 2 == 1 else rail_endpoints[i + 4]
        railXML += '<DrawLine x1="{}" y1="{}" z1="{}" x2="{}" y2="{}" z2="{}" type="redstone_block"/>'.format(a[0], a[1], a[2], b[0], b[1], b[2])
        railXML += '<DrawLine x1="{}" y1="{}" z1="{}" x2="{}" y2="{}" z2="{}" type="rail"/>'.format(a[0], a[1] + 1, a[2], b[0], b[1] + 1, b[2])

    # Add a diamond and a barrier at the ends:
    end = rail_endpoints[-2]
    start = rail_endpoints[0]
    railXML += '<DrawItem x="{}" y="{}" z="{}" type="diamond"/>'.format(end[0] + 1, end[1] + 2, end[2] - 1)
    railXML += '<DrawBlock x="{}" y="{}" z="{}" type="planks"/>'.format(end[0], end[1] + 1, end[2] - 1)
    railXML += '<DrawBlock x="{}" y="{}" z="{}" type="planks"/>'.format(start[0], start[1] + 1, start[2] + 1)
    # And add the minecart:
    railXML += '<DrawEntity x="1" y="57" z="4" yaw="-90" type="MinecartRideable"/>'
    return railXML + '</DrawingDecorator>'

def getZooXML(zooMobs, cells_across, cell_width, cell_height, cell_depth, orgx, orgy, orgz, left_padding = 0, right_padding = 0):
    global rail_endpoints, cell_midpoints
    missionXML = "<DrawingDecorator>"

    # Get some dimensions:
    # We allow cell_depth to be positive or negative, to specify front facing or back facing cells.
    z_offset = 1 if cell_depth > 0 else -1
    outer_left = orgx
    outer_right = outer_left + cell_width * cells_across
    inner_left, inner_right = outer_left + 1, outer_right - 1
    outer_front = orgz
    outer_back = orgz + cell_depth
    inner_front, inner_back = outer_front + z_offset, outer_back - z_offset
    outer_bottom, outer_top, inner_bottom, inner_top = 0, 0, 0, 0   # Filled in later
    plug_in_dimensions = lambda s : s.format(outer_left = outer_left,
                                           inner_left = inner_left,
                                           outer_right = outer_right,
                                           inner_right = inner_right,
                                           outer_top = outer_top,
                                           inner_top = inner_top,
                                           outer_bottom = outer_bottom,
                                           inner_bottom = inner_bottom,
                                           outer_front = outer_front,
                                           inner_front = inner_front,
                                           outer_back = outer_back,
                                           inner_back = inner_back)
    # Draw the levels:
    num_levels = int(math.ceil(old_div(float(len(zooMobs)), float(cells_across))))
    for i in range(num_levels):
        # Dimensions for this level:
        outer_bottom = orgy + i * cell_height
        outer_top = orgy + (i + 1) * cell_height
        inner_bottom, inner_top = outer_bottom + 1, outer_top - 1

        drawLevelXML = '''<DrawCuboid x1="{outer_left}" y1="{outer_bottom}" z1="{outer_front}"
                                      x2="{outer_right}" y2="{outer_top}" z2="{outer_back}"
                                      type="bedrock"/>
                          <DrawCuboid x1="{inner_left}" y1="{inner_bottom}" z1="{inner_front}"
                                      x2="{inner_right}" y2="{inner_top}" z2="{inner_back}"
                                      type="air"/>
                          <DrawCuboid x1="{outer_left}" y1="{outer_bottom}" z1="{outer_front}"
                                      x2="{outer_right}" y2="{outer_top}" z2="{outer_front}"
                                      type="'''+ barrier_type + '''"/>'''
        missionXML += plug_in_dimensions(drawLevelXML)
        # Add a platform and golden rail for this level:
        missionXML += '''<DrawLine x1="{outer_left}" y1="{outer_bottom}" z1="{front_lip}"
                                   x2="{outer_right}" y2="{outer_bottom}" z2="{front_lip}"
                                   type="redstone_block"/>
                         <DrawLine x1="{outer_left}" y1="{inner_bottom}" z1="{front_lip}"
                                   x2="{outer_right}" y2="{inner_bottom}" z2="{front_lip}"
                                   type="golden_rail"/>
                         <DrawBlock x="{outer_left}" y="{inner_bottom}" z="{front_lip}" type="rail"/>
                         <DrawBlock x="{outer_right}" y="{inner_bottom}" z="{front_lip}" type="rail"/>'''.format(
                                                                  outer_left = outer_left - left_padding,
                                                                  outer_right = outer_right + right_padding,
                                                                  inner_bottom = inner_bottom,
                                                                  outer_bottom = outer_bottom,
                                                                  front_lip = outer_front - z_offset)
        rail_endpoints.append((outer_left - left_padding, outer_bottom, outer_front - 2 * z_offset))
        rail_endpoints.append((outer_right + right_padding, outer_bottom, outer_front - 2 * z_offset))

    # Draw the dividing walls, cell contents, and the entities. We do this in two passes,
    # since drawing the walls after drawing the entities can result in killing them.
    for draw_pass in ["cell", "entity"]:
        for i, mob in enumerate(zooMobs):
            x = i % cells_across
            y = old_div(i, cells_across)
            # Dimensions for this cell:
            outer_left = orgx + x * cell_width
            outer_right = outer_left + cell_width
            inner_left, inner_right = outer_left + 1, outer_right - 1
            outer_bottom = orgy + y * cell_height
            outer_top = outer_bottom + cell_height
            inner_bottom, inner_top = outer_bottom + 1, outer_top - 1

            # Should we draw the entity?
            if draw_pass == "entity" and len(mob[0]) > 0:
                mobx, moby, mobz = outer_left + old_div(cell_width, 2), inner_bottom, orgz + 3 * z_offset
                missionXML += '<DrawEntity x="{}" y="{}" z="{}" type="{}" yaw="0"/>'.format(mobx, moby, mobz, mob[0])
                cell_midpoints.append((mobx, moby, mobz, mob[0]))

            if draw_pass == "cell":
                cellXML = '''<DrawCuboid x1="{outer_left}" y1="{inner_bottom}" z1="{inner_front}"
                                         x2="{outer_left}" y2="{inner_top}" z2="{inner_back}"
                                         type="iron_bars"/>'''
                missionXML += plug_in_dimensions(cellXML)
                # Draw any extra cell furniture:
                if len(mob[1]) > 0:
                    missionXML += plug_in_dimensions(mob[1])

    missionXML += '''</DrawingDecorator>'''
    return missionXML

my_client_pool = MalmoPython.ClientPool()
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10000))

if sys.version_info[0] == 2:
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
else:
    import functools
    print = functools.partial(print, flush=True)
my_mission = MalmoPython.MissionSpec(getMissionXML(endCondition, timeoutCondition), True)

my_mission_record = MalmoPython.MissionRecordSpec()
max_retries = 3
for retry in range(max_retries):
    try:
        agent_host.startMission( my_mission, my_client_pool, my_mission_record, 0, "blahblah" )
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

agent_host.sendCommand("use 1")
agent_host.sendCommand("move 0.1")

mob_list = friendlyMobs[0:6] + hostileMobs[10::-1] + friendlyMobs[6:12] + hostileMobs[21:10:-1] + friendlyMobs[12:] + hostileMobs[:21:-1]
mobs_to_view = [mob[0] for mob in mob_list if mob[0] != ""]
mob_index = 0

while world_state.is_mission_running:
    world_state = agent_host.getWorldState()
    if world_state.number_of_observations_since_last_state > 0:
        obvsText = world_state.observations[-1].text
        data = json.loads(obvsText) # observation comes in as a JSON string...
        current_x = data.get(u'XPos', 0)
        current_z = data.get(u'ZPos', 0)
        current_y = data.get(u'YPos', 0)
        if "entities" in data:
            entities = [EntityInfo(k["x"], k["y"], k["z"], k["name"]) for k in data["entities"]]
            checkEnts([ent.name for ent in entities], mobs_to_view)
            # Find our closest cell, and work out what should be in it:
            dist_to_cells = [abs(c[0] - current_x) + abs(c[1] - current_y) + abs(c[2] - current_z) for c in cell_midpoints]
            target_ent_name = cell_midpoints[dist_to_cells.index(min(dist_to_cells))][3]
            if mob_index < len(mobs_to_view) and target_ent_name == mobs_to_view[mob_index]:
                agent_host.sendCommand("chat hello " + target_ent_name)
                mob_index += 1
            # Attempt to find that entity in our entities list:
            target_ents = [e for e in entities if e.name == target_ent_name]
            if len(target_ents) != 1:
                pass
            else:
                target_ent = target_ents[0]
                # Look up height of entity from our table:
                target_height = [e for e in mob_list if e[0] == target_ent_name][0][2]
                # Calculate where to look in order to see it:
                target_yaw, target_pitch = calcYawAndPitchToMob(target_ent, current_x, current_y, current_z, target_height)
                # And point ourselves there:
                pointing_at_target = pointTo(agent_host, data, target_pitch, target_yaw, 0.5)

# End of mission:
reward = world_state.rewards[-1].getValue()
print("Result: " + str(reward))
if reward < 0:
    exit(1)
else:
    exit(0)
