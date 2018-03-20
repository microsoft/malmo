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

# Tests the following:
# - Build battle decorator / reward producer
# - Drawing decorator
# - Inventory initialisation
# - Discrete use command
# - Continuous turn/pitch/strafe commands
# - Observation from ray
# - Inventory observations / hotkey commands
# - Parsing the MissionEnded XML message

# If all these things are working correctly, this test should create a series of random build challenges,
# using the DrawingDecorator / BuildBattleDecorator / RewardForStructureCopying, placing the exact number of required
# blocks into the agent's inventory.
# The agent will then use the continuous turn/pitch commands and the ObservationFromRay to determine the state
# of each block, before strafing to the destination grid and using the discrete use command / inventory hotbar commands
# to reproduce the source grid.
# The expected total reward is calculated and compared to the actual reward received.

from builtins import range
from builtins import object
from past.utils import old_div
import MalmoPython
import os
import random
import sys
import time
import json
import copy
import errno
import math
import xml.etree.ElementTree
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)
TESTING = agent_host.receivedArgument("test")

recordingsDirectory = malmoutils.get_recordings_directory(agent_host)

video_requirements = '<VideoProducer><Width>860</Width><Height>480</Height></VideoProducer>' if agent_host.receivedArgument("record_video") else ''

# dimensions of the test structure (works best with 3x3)
SIZE_X = 3
SIZE_Z = 3

# reward scheme
REWARD_PER_BLOCK = 1
REWARD_FOR_COMPLETION = 1000

# pallettes for the structure etc
pallette = ["air", "air", "mycelium","glowstone","netherrack","slime"]
recolour_pallettes = ['<BlockTypeOnCorrectPlacement type="fire" /> <BlockTypeOnIncorrectPlacement type="redstone_block" />',
                      '<BlockTypeOnCorrectPlacement type="emerald_block" /> <BlockTypeOnIncorrectPlacement type="redstone_block" />',
                      '',
                      '<BlockTypeOnCorrectPlacement type="sea_lantern" />']

def createTestStructure(sx, sz):
    while True:
        s = [[(random.randint(0,len(pallette))) for z in range(sz)] for x in range(sx)]
        # Check we didn't create a block entirely made of air:
        if sum(s[x][z] > 1 for x in range(sx) for z in range(sz)):
            break
    return s

def structureToXML(structure, xorg, yorg, zorg):
    # Take the structure and create a drawing decorator and inventory spec from it.
    drawing = ""
    inventory = {}
    expected_reward = 0
    for z in range(SIZE_Z):
        for x in range(SIZE_X):
            value = structure[x][z]
            type = pallette[value % len(pallette)]
            type_string = ' type="' + type + '"'
            drawing += '<DrawBlock x="' + str(x + xorg) + '" y="' + str(yorg) + '" z="' + str(z + zorg) + '" ' + type_string + '/>'
            inventory[type] = inventory.get(type, 0) + 1
            if type != "air":
                expected_reward += REWARD_PER_BLOCK
    drawingdecorator = "<DrawingDecorator>"
    # "Blank out" the volume, in case if overlaps with old structures and throws the test.
    drawingdecorator += '<DrawCuboid x1="' + str(xorg) + '" y1="' + str(yorg) + '" z1="' + str(zorg) + '" x2="' + str(xorg + SIZE_X + 1 + SIZE_X) + '" y2="' + str(yorg) + '" z2="' + str(zorg + SIZE_Z - 1) + '" type="air"/>'
    drawingdecorator += '<DrawCuboid x1="' + str(xorg) + '" y1="' + str(yorg-1) + '" z1="' + str(zorg) + '" x2="' + str(xorg + SIZE_X - 1) + '" y2="' + str(yorg-1) + '" z2="' + str(zorg + SIZE_Z - 1) + '" type="red_sandstone"/>'
    drawingdecorator += '<DrawCuboid x1="' + str(xorg + SIZE_X + 1) + '" y1="' + str(yorg-1) + '" z1="' + str(zorg) + '" x2="' + str(xorg + 2*SIZE_X) + '" y2="' + str(yorg-1) + '" z2="' + str(zorg + SIZE_Z - 1) + '" type="red_sandstone"/>'
    drawingdecorator += drawing + "</DrawingDecorator>"
    inventoryxml = '<Inventory>'
    slot = 0
    for i in range(0, len(inventory)):
        if list(inventory.keys())[i] != "air":
            inventoryxml += '<InventoryBlock slot="'+str(slot)+'" type="'+list(inventory.keys())[i] + '" quantity="' + str(list(inventory.values())[i]) + '"/>'
            slot += 1
    inventoryxml += '</Inventory>'
    expected_reward += REWARD_FOR_COMPLETION
    return drawingdecorator, inventoryxml, expected_reward

def getMissionXML(forceReset, structure):
    # Draw a structure directly in front of the player.
    xpos = 0
    zpos = 0
    xorg = xpos - int(old_div(SIZE_X, 2))
    yorg = 1
    zorg = zpos + 1
    structureXML, inventoryXML, expected_reward = structureToXML(structure, xorg, yorg, zorg)
    startpos = ()
    recolourXML = random.choice(recolour_pallettes)

    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <About>
        <Summary>Test build battles</Summary>
    </About>
     
    <ServerSection>
        <ServerHandlers>
            <FlatWorldGenerator generatorString="3;168:1;8;" forceReset=''' + forceReset + '''/>''' + structureXML + '''
            <BuildBattleDecorator>
                <GoalStructureBounds>
                    <min x="''' + str(xorg) + '''" y="''' + str(yorg) + '''" z="''' + str(zorg) + '''"/>
                    <max x="''' + str(xorg + SIZE_X - 1) + '''" y="''' + str(yorg) + '''" z="''' + str(zorg + SIZE_Z - 1) + '''"/>
                </GoalStructureBounds>
                <PlayerStructureBounds>
                    <min x="''' + str(1 + xorg + SIZE_X) + '''" y="''' + str(yorg) + '''" z="''' + str(zorg) + '''"/>
                    <max x="''' + str(xorg + 2*SIZE_X) + '''" y="''' + str(yorg) + '''" z="''' + str(zorg + SIZE_Z - 1) + '''"/>
                </PlayerStructureBounds>''' + recolourXML + '''
            </BuildBattleDecorator>
            <ServerQuitWhenAnyAgentFinishes />
            <ServerQuitFromTimeUp timeLimitMs="25000" description="Ran out of time."/>
        </ServerHandlers>
    </ServerSection>

    <AgentSection mode="Survival">
        <Name>Han van Meegeren</Name>
        <AgentStart>
            <Placement x="''' + str(xpos + 0.5) + '''" y="1.0" z="''' + str(zpos + 0.5) + '''"/>''' + inventoryXML + '''
        </AgentStart>
        <AgentHandlers>
            <ContinuousMovementCommands />
            <DiscreteMovementCommands />
            <InventoryCommands />
            <ObservationFromFullStats/>
            <RewardForStructureCopying rewardScale="''' + str(REWARD_PER_BLOCK) + '''" rewardForCompletion="''' + str(REWARD_FOR_COMPLETION) + '''">
                <RewardDensity>PER_BLOCK</RewardDensity>
                <AddQuitProducer description="Build successful!"/>
            </RewardForStructureCopying>
            <ObservationFromRay/>
            <ObservationFromHotBar/>''' + video_requirements + '''
            </AgentHandlers>
    </AgentSection>

  </Mission>''', expected_reward

class CopyAgent(object):
    ''' An agent that can sweep an area, build up a model of what blocks exist, and then copy that
    model to a new location.'''
    sentinel=(-1,-1)
    class Modes(object):
        InitSweep, Sweep, InitMove, Move, InitCopy, Copy, Wait = list(range(7))

    def findHotKeyForBlockType(self, ob, type):
        '''Hunt in the inventory hotbar observations for the slot which contains the requested type.'''
        for i in range(0, 9):
            slot_name = u'Hotbar_' + str(i) + '_item'
            slot_contents = ob.get(slot_name, "")
            if slot_contents == type:
                return i+1  # +1 to convert from 0-based inventory slot to 1-based hotbar key.
        return -1

    def angvel(self, target, current, scale):
        '''Use sigmoid function to choose a delta that will help smoothly steer from current angle to target angle.'''
        delta = target - current
        while delta < -180:
            delta += 360;
        while delta > 180:
            delta -= 360;
        return (old_div(2.0, (1.0 + math.exp(old_div(-delta,scale))))) - 1.0

    def pointTo(self, ah, ob, target_pitch, target_yaw, threshold):
        '''Steer towards the target pitch/yaw, return True when within the given tolerance threshold.'''
        pitch = ob.get(u'Pitch', 0)
        yaw = ob.get(u'Yaw', 0)
        delta_yaw = self.angvel(target_yaw, yaw, 20.0)
        delta_pitch = self.angvel(target_pitch, pitch, 30.0)
        agent_host.sendCommand("turn " + str(delta_yaw))    
        agent_host.sendCommand("pitch " + str(delta_pitch))
        if abs(pitch-target_pitch) + abs(yaw-target_yaw) < threshold:
            agent_host.sendCommand("turn 0")
            agent_host.sendCommand("pitch 0")
            return True
        return False

    def __init__(self, size_x, size_z):
        self.size_x = size_x
        self.size_z = size_z
        self.mode = CopyAgent.Modes.InitSweep
        self.createTargets()

    def reset(self):
        self.mode = CopyAgent.Modes.InitSweep

    def createTargets(self):
        ''' Calculate yaw and pitch pairs for each block in the source and dest grids.'''
        self.targets = []
        # Source grid:
        height = 0.625  # Height from top of block (player's eyes are positioned at height of 1.625 blocks from the ground.)
        direction = 1.0
        for z in range(self.size_z, 0, -1):
            for x in range(-(old_div(self.size_x,2)),(old_div(self.size_x,2))+1):
                yaw = direction * x * math.atan(old_div(1.0,z)) * 180.0/math.pi
                distance = math.sqrt(x*x + z*z)
                pitch = math.atan(old_div(height,distance)) * 180.0/math.pi
                self.targets.append((pitch,yaw))
            direction *= -1.0

        if TESTING:
            # For added security in test scenario, loop backwards through targets.
            tmp = list(self.targets)
            tmp.reverse()
            self.targets += tmp

        self.targets.append((0,0))
        self.targets.append(CopyAgent.sentinel)

        # Dest grid:
        height = 1.625  # Height from ground.
        direction = 1.0
        for z in range(self.size_z, 0, -1):
            for x in range(-(old_div(self.size_x,2)),(old_div(self.size_x,2))+1):
                yaw = direction * x * math.atan(old_div(1.0,z)) * 180.0/math.pi
                distance = math.sqrt(x*x + z*z)
                pitch = math.atan(old_div(height,distance)) * 180.0/math.pi
                self.targets.append((pitch,yaw))
            direction *= -1.0

        self.targets.append((0,0))

    def init_scan(self, ah, ob):
        self.current_target = 0
        self.world = {}
        self.replay_mask = []
        return True

    def scan(self, ah, ob):
        '''Sweep the cells in the source grid, use ObservationFromRay to build up a model of the blocks.'''
        target_pitch, target_yaw = self.targets[self.current_target]
        # If this is the first target point, or the reset target [(0,0)], then insist on accuracy.
        # Otherwise we can be fairly lax.
        if self.pointTo(ah, ob, target_pitch, target_yaw, 0.5 if (self.current_target == 0 or (target_pitch == 0 and target_yaw == 0)) else 5.0):
            hotkey_value = -1
            if u'LineOfSight' in ob:
                los = ob[u'LineOfSight']
                x = int(math.floor(los["x"]))
                y = int(math.floor(los["y"]-0.0001))
                z = int(math.floor(los["z"]))
                key = str(x+self.size_x+1)+":"+str(y)+":"+str(z)
                type=los["type"]
                if y == 1 and not key in self.world:
                    self.world[key] = type
                    hotkey_value = self.findHotKeyForBlockType(ob, type)
            self.current_target += 1
            self.replay_mask.append(hotkey_value)
            if self.targets[self.current_target] == CopyAgent.sentinel:
                self.current_target += 1
                return True
        return False

    def init_move(self, ah, ob):
        return True

    def move(self, ah, ob):
        '''Side step to the left, to face the destination grid.'''
        target_xpos = self.size_x + 1.5
        xpos = ob.get(u'XPos', 0)
        strafe = xpos - target_xpos
        strafe = (old_div(2.0, (1.0 + math.exp(-strafe)))) - 1.0
        ah.sendCommand("strafe " + str(strafe))
        if abs(target_xpos - xpos) < 0.1:
            agent_host.sendCommand("strafe 0")
            return True
        return False

    def init_copy(self, ah, ob):
        self.current_replay_target = 0
        self.replay_accuracy = 0.5
        self.current_hotbar_key = -1
        return True

    def copy(self, ah, ob):
        '''Sweep the cells in the destination grid, and place blocks at the relevant positions,
        using the data we gathered in the scan phase.'''
        target_pitch, target_yaw = self.targets[self.current_target]
        hotkey = self.replay_mask[self.current_replay_target]
        if hotkey >= 0 and hotkey != self.current_hotbar_key:
            # Hotbar slot has changed - select the correct slot:
            self.current_hotbar_key = hotkey
            agent_host.sendCommand("hotbar." + str(hotkey) + " 1")  # press
            agent_host.sendCommand("hotbar." + str(hotkey) + " 0")  # release

        proceed = True if hotkey < 0 else False # Skip this position if there's nothing to place there.
        if not proceed and self.pointTo(ah, ob, target_pitch, target_yaw, self.replay_accuracy):
            if TESTING:
                self.replay_accuracy = 1
            else:
                self.replay_accuracy = 5    # Once we've honed in on the first point, we can be less accurate with the rest.
            if u'LineOfSight' in ob:
                los = ob[u'LineOfSight']
                x = int(math.floor(los["x"]))
                y = int(math.floor(los["y"]-0.0001))
                z = int(math.floor(los["z"]))
                key=str(x)+":"+str(y+1)+":"+str(z)
                type = self.world.pop(key, None)
                if type:
                    ah.sendCommand("use")
                    proceed = True
        
        if proceed:
            self.current_target += 1
            self.current_replay_target += 1
            if self.current_target >= len(self.targets):
                self.current_target = 0
                return True
        return False

    def wait(self, ah, ob):
        return False    # Do nothing - only get here if we failed to complete the build.

    behaviour = {Modes.InitSweep:init_scan, Modes.Sweep:scan,
                 Modes.InitMove:init_move, Modes.Move:move,
                 Modes.InitCopy:init_copy, Modes.Copy:copy,
                 Modes.Wait:wait}

    def act(self, ah, ob):
        if CopyAgent.behaviour[self.mode](self, ah, ob):
            self.mode += 1

# Create a bunch of build battle missions and run an agent on them.
num_iterations = 30000
if TESTING:
    num_iterations = 5

# Set up a recording
recording = False
my_mission_record = MalmoPython.MissionRecordSpec()
if recordingsDirectory:
    recording = True
    my_mission_record.recordRewards()
    my_mission_record.recordObservations()
    my_mission_record.recordCommands()
    if video_requirements:
        my_mission_record.recordMP4(24,2000000)

# Create agent to run all the missions:
agent = CopyAgent(SIZE_X, SIZE_Z)

for i in range(num_iterations):
    structure = createTestStructure(SIZE_X, SIZE_Z)
    missionXML, expected_reward = getMissionXML('"false"', structure)
    if recording:
        my_mission_record.setDestination(recordingsDirectory + "//" + "Mission_" + str(i + 1) + ".tgz")
    my_mission = MalmoPython.MissionSpec(missionXML, True)
    agent.reset()

    # Start the mission:
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

    total_reward = 0
    # Mission loop:
    while world_state.is_mission_running:
        if world_state.number_of_rewards_since_last_state > 0:
            for r in world_state.rewards:
                print("Got reward: ", r.getValue())
                total_reward += r.getValue()
        if world_state.number_of_observations_since_last_state > 0:
            msg = world_state.observations[-1].text
            ob = json.loads(msg)
            agent.act(agent_host, ob)
        world_state = agent_host.getWorldState()

    for r in world_state.rewards:
        total_reward += r.getValue()

    # End of mission - parse the MissionEnded XML messasge:
    mission_end_tree = xml.etree.ElementTree.fromstring(world_state.mission_control_messages[-1].text)
    ns_dict = {"malmo":"http://ProjectMalmo.microsoft.com"}
    stat = mission_end_tree.find("malmo:Status", ns_dict).text
    hr_stat = mission_end_tree.find("malmo:HumanReadableStatus", ns_dict).text
    print("Mission over. Status: ", stat, end=' ')
    if len(hr_stat):
        print(" - " + hr_stat)
    if total_reward != expected_reward:
        print("Mission failed - total reward was " + str(total_reward) + ", expected reward was " + str(expected_reward))
        if TESTING:
            exit(1)
    print()
