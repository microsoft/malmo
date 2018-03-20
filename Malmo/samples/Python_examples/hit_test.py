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

# Demo of reward for damaging mobs - create an arena filled with pigs and sheep,
# and reward the agent positively for attacking sheep, and negatively for attacking pigs.
# Using this reward signal to train the agent is left as an exercise for the reader...
# this demo just uses ObservationFromRay and ObservationFromNearbyEntities to determine
# when and where to attack.

from builtins import range
from past.utils import old_div
import MalmoPython
import os
import random
import sys
import time
import json
import random
import errno
import math
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)
recordingsDirectory = malmoutils.get_recordings_directory(agent_host)
video_requirements = '<VideoProducer><Width>860</Width><Height>480</Height></VideoProducer>' if agent_host.receivedArgument("record_video") else ''

# Task parameters:
ARENA_WIDTH = 20
ARENA_BREADTH = 20

def getCorner(index,top,left,expand=0,y=0):
    ''' Return part of the XML string that defines the requested corner'''
    x = str(-(expand+old_div(ARENA_WIDTH,2))) if left else str(expand+old_div(ARENA_WIDTH,2))
    z = str(-(expand+old_div(ARENA_BREADTH,2))) if top else str(expand+old_div(ARENA_BREADTH,2))
    return 'x'+index+'="'+x+'" y'+index+'="' +str(y)+'" z'+index+'="'+z+'"'

def getSpawnEndTag(i):
    return ' type="mob_spawner" variant="' + ["Sheep", "Pig"][i % 2] + '"/>'

def getMissionXML(summary):
    ''' Build an XML mission string.'''
    # We put the spawners inside an animation object, to move them out of range of the player after a short period of time.
    # Otherwise they will just keep spawning - as soon as the agent kills a sheep, it will be replaced.
    # (Could use DrawEntity to create the pigs/sheep, rather than using spawners... but this way is much more fun.)
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>''' + summary + '''</Summary>
        </About>

        <ModSettings>
            <MsPerTick>50</MsPerTick>
        </ModSettings>
        <ServerSection>
            <ServerInitialConditions>
                <Time>
                    <StartTime>1000</StartTime>
                    <AllowPassageOfTime>true</AllowPassageOfTime>
                </Time>
                <AllowSpawning>true</AllowSpawning>
                <AllowedMobs>Pig Sheep</AllowedMobs>
            </ServerInitialConditions>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,202*1,5*3,2;3;,biome_1" />
                <DrawingDecorator>
                    <DrawCuboid ''' + getCorner("1",True,True,expand=10,y=206) + " " + getCorner("2",False,False,y=246,expand=10) + ''' type="grass"/>
                    <DrawCuboid ''' + getCorner("1",True,True,y=207) + " " + getCorner("2",False,False,y=246) + ''' type="air"/>
                </DrawingDecorator>
                <AnimationDecorator ticksPerUpdate="10">
                <Linear>
                    <CanvasBounds>
                        <min x="''' + str(old_div(-ARENA_BREADTH,2)) + '''" y="205" z="''' + str(old_div(-ARENA_BREADTH,2)) + '''"/>
                        <max x="''' + str(old_div(ARENA_WIDTH,2)) + '''" y="217" z="''' + str(old_div(ARENA_WIDTH,2)) + '''"/>
                    </CanvasBounds>
                    <InitialPos x="0" y="207" z="0"/>
                    <InitialVelocity x="0" y="0.025" z="0"/>
                </Linear>
                <DrawingDecorator>
                    <DrawLine ''' + getCorner("1",True,True,expand=-2) + " " + getCorner("2",True,False,expand=-2) + getSpawnEndTag(1) + '''
                    <DrawLine ''' + getCorner("1",True,True,expand=-2) + " " + getCorner("2",False,True,expand=-2) + getSpawnEndTag(1) + '''
                    <DrawLine ''' + getCorner("1",False,False,expand=-2) + " " + getCorner("2",True,False,expand=-2) + getSpawnEndTag(1) + '''
                    <DrawLine ''' + getCorner("1",False,False,expand=-2) + " " + getCorner("2",False,True,expand=-2) + getSpawnEndTag(1) + '''
                    <DrawLine ''' + getCorner("1",True,True,expand=-3) + " " + getCorner("2",True,False,expand=-3) + getSpawnEndTag(2) + '''
                    <DrawLine ''' + getCorner("1",True,True,expand=-3) + " " + getCorner("2",False,True,expand=-3) + getSpawnEndTag(2) + '''
                    <DrawLine ''' + getCorner("1",False,False,expand=-3) + " " + getCorner("2",True,False,expand=-3) + getSpawnEndTag(2) + '''
                    <DrawLine ''' + getCorner("1",False,False,expand=-3) + " " + getCorner("2",False,True,expand=-3) + getSpawnEndTag(2) + '''
                </DrawingDecorator>
                </AnimationDecorator>
               <ServerQuitWhenAnyAgentFinishes />
               <ServerQuitFromTimeUp timeLimitMs="120000"/>
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>The Hunter</Name>
            <AgentStart>
                <Placement x="0.5" y="207.0" z="0.5" pitch="20"/>
                <Inventory>
                    <InventoryItem type="diamond_pickaxe" slot="0"/>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <ContinuousMovementCommands turnSpeedDegs="420"/>
                <ObservationFromRay/>
                <RewardForDamagingEntity>
                    <Mob type="Sheep" reward="1"/>
                    <Mob type="Pig" reward="-1"/>
                </RewardForDamagingEntity>
                <ObservationFromNearbyEntities>
                    <Range name="entities" xrange="'''+str(ARENA_WIDTH)+'''" yrange="2" zrange="'''+str(ARENA_BREADTH)+'''" />
                </ObservationFromNearbyEntities>
                <ObservationFromFullStats/>''' + video_requirements + '''
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

validate = True
my_client_pool = MalmoPython.ClientPool()
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10000))

if agent_host.receivedArgument("test"):
    num_reps = 1
else:
    num_reps = 30000

for iRepeat in range(num_reps):
    mission_xml = getMissionXML("Go hunting! #" + str(iRepeat))
    my_mission = MalmoPython.MissionSpec(mission_xml,validate)
    # Set up a recording
    my_mission_record = MalmoPython.MissionRecordSpec()
    if recordingsDirectory:
        my_mission_record.setDestination(recordingsDirectory + "//" + "Mission_" + str(iRepeat + 1) + ".tgz")
        my_mission_record.recordRewards()
        my_mission_record.recordObservations()
        my_mission_record.recordCommands()
        if agent_host.receivedArgument("record_video"):
            my_mission_record.recordMP4(24,2000000)

    max_retries = 3
    for retry in range(max_retries):
        try:
            # Attempt to start the mission:
            agent_host.startMission( my_mission, my_client_pool, my_mission_record, 0, "hunterExperiment" )
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

    # main loop:
    total_reward = 0
    pig_population = 0
    sheep_population = 0
    self_x = 0
    self_z = 0
    current_yaw = 0

    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_observations_since_last_state > 0:
            msg = world_state.observations[-1].text
            ob = json.loads(msg)
            # Use the line-of-sight observation to determine when to hit and when not to hit:
            if u'LineOfSight' in ob:
                los = ob[u'LineOfSight']
                type=los["type"]
                if type == "Sheep":
                    agent_host.sendCommand("attack 1")
                    agent_host.sendCommand("attack 0")
            # Get our position/orientation:
            if u'Yaw' in ob:
                current_yaw = ob[u'Yaw']
            if u'XPos' in ob:
                self_x = ob[u'XPos']
            if u'ZPos' in ob:
                self_z = ob[u'ZPos']
            # Use the nearby-entities observation to decide which way to move, and to keep track
            # of population sizes - allows us some measure of "progress".
            if u'entities' in ob:
                entities = ob["entities"]
                num_pigs = 0
                num_sheep = 0
                x_pull = 0
                z_pull = 0
                for e in entities:
                    if e["name"] == "Sheep":
                        num_sheep += 1
                        # Each sheep contributes to the direction we should head in...
                        dist = max(0.0001, (e["x"] - self_x) * (e["x"] - self_x) + (e["z"] - self_z) * (e["z"] - self_z))
                        # Prioritise going after wounded sheep. Max sheep health is 8, according to Minecraft wiki...
                        weight = 9.0 - e["life"]
                        x_pull += weight * (e["x"] - self_x) / dist
                        z_pull += weight * (e["z"] - self_z) / dist
                    elif e["name"] == "Pig":
                        num_pigs += 1
                # Determine the direction we need to turn in order to head towards the "sheepiest" point:
                yaw = -180 * math.atan2(x_pull, z_pull) / math.pi
                difference = yaw - current_yaw;
                while difference < -180:
                    difference += 360;
                while difference > 180:
                    difference -= 360;
                difference /= 180.0;
                agent_host.sendCommand("turn " + str(difference))
                move_speed = 1.0 if abs(difference) < 0.5 else 0  # move slower when turning faster - helps with "orbiting" problem
                agent_host.sendCommand("move " + str(move_speed))
                if num_sheep != sheep_population or num_pigs != pig_population:
                    # Print an update of our "progress":
                    sheep_population = num_sheep
                    pig_population = num_pigs
                    tot = sheep_population + pig_population
                    if tot:
                        print("PIGS:SHEEP", end=' ')
                        r = old_div(40.0, tot)
                        p = int(num_pigs * r)
                        print("P" * p, "|", "S" * (40 - p), "(", num_pigs, num_sheep, ")")
                        
        if world_state.number_of_rewards_since_last_state > 0:
            # Keep track of our total reward:
            total_reward += world_state.rewards[-1].getValue()

    # mission has ended.
    for error in world_state.errors:
        print("Error:",error.text)
    if world_state.number_of_rewards_since_last_state > 0:
        # A reward signal has come in - see what it is:
        total_reward += world_state.rewards[-1].getValue()

    print()
    print("=" * 41)
    print("Total score this round:", total_reward)
    print("=" * 41)
    print()
    time.sleep(1) # Give the mod a little time to prepare for the next mission.
