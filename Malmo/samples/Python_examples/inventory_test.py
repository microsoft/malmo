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

# Test of Malmo's extended inventory feature:
# - DrawingDecorator allows you to "draw" containers with specific contents
# - ObservationFromFullInventory includes contents of the container under focus
# - InventoryCommands allows you to transfer items between the player's inventory and the container

# To test, create sixteen shulker boxes - one for each colour - arranged in a circle.
# Take four stained glass tiles of each colour - 64 tiles in total - and shuffle them, putting four in each box.
# Stand the agent in the middle, rotating so that it "sees" each container in turn.
# "Sort" the tiles by collecting all the wrong coloured tiles from the box,
# and placing all the right coloured tiles into the box.

# (In theory, after two complete rotations the boxes should all be sorted.
# In practice, it may take longer, since each inventory swap command takes a finite time to process,
# and the agent keeps rotating at a fixed speed, so may not have time to complete all the swaps before
# moving on to the next box.)

from builtins import range
from past.utils import old_div
import MalmoPython
import json
import math
import os
import random
import sys
import time
from collections import namedtuple
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)

# Create a named tuple type for the inventory contents.
InventoryObject = namedtuple('InventoryObject', 'type, colour, variation, quantity, inventory, index')
InventoryObject.__new__.__defaults__ = ("", "", "", 0, "", 0)

# The sixteen Minecraft colours - see Types.xsd
colours = ["WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "SILVER", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"]

def boxCompleted(inventoryFull, colour, merges_allowed):
    '''Check whether the box included in this inventory is complete.'''
    inventory = [i for i in inventoryFull if i.inventory != "inventory"]    # Filter out the box's items from the player's.
    if not merges_allowed:
        # In strict mode (where we're not combining item stacks), a box is considered
        # completed if it contains exactly four items of the correct colour,
        # occupying the first four slots.
        if len(inventory) != 4:
            return False    # Wrong number of items in inventory
        slotFilled = [False for x in range(4)]
        for i in inventory:
            if i.colour != colour:
                return False    # Item of wrong colour in inventory
            if i.index >= 4 or i.index < 0:
                return False    # Item in wrong slot
            slotFilled[i.index] = True
        if False in slotFilled:
            return False    # Not all of the first four slots are filled
        return True # All good
    else:
        # If we are combining item stacks, the box is considered complete if it contains exactly four items of
        # the correct colour, and no other items, but the items could be spread across < 4 slots.
        total_correct = sum([i.quantity for i in inventory if i.colour == colour])
        total_incorrect = sum([i.quantity for i in inventory if i.colour != colour])
        return total_correct == 4 and total_incorrect == 0

def getContainerXML():
    '''Place 16 shulker boxes in Minecraft's equivalent of a circle, and fill each one with four randomly chosen coloured tiles.'''
    xml = ""
    radius = 4.0
    # A greater radius gives a more "circular" circle, but need to keep the boxes within range of the agent.
    contents = 4 * ['<Object type="stained_glass" colour="{}" quantity="1"/>'.format(col) for col in colours] # Four tiles for each colour
    for i, col in enumerate(colours):
        ang = math.pi * float(i) / 8.0
        x = int(radius * math.cos(ang))
        z = int(radius * math.sin(ang))
        type = col.lower() + "_shulker_box"
        xml += '<DrawContainer x="{x}" y="56" z="{z}" type="{type}">'.format(x = x, z = z, type = type)
        # Fill with four random entries from the contents list.
        # To ensure we get no accidental duplicates, chose each item from the
        # items remaining, and swap it out after use.
        # (eg https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm)
        random.seed()   # Otherwise we always get the same arrangement
        for content_index in range(i * 4, (i + 1) * 4):
            rand = random.randint(content_index, len(contents) - 1)
            xml += contents[rand]
            contents[rand], contents[content_index] = contents[content_index], contents[rand]
        xml += '</DrawContainer>'
    return xml

missionXML = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Colourful Sorting</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,44*49,73,35:1,159:4,95:13,35:13,159:11,95:10,159:14,159:6,35:6,95:6;157;" />
                <DrawingDecorator>''' + getContainerXML() + '''
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="120000" description="out_of_time"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Mondrian</Name>
            <AgentStart>
                <Placement x="0.5" y="56.0" z="0.5" pitch="18"/>
                <Inventory/>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullInventory flat="false"/>
                <ObservationFromRay/>
                <InventoryCommands/>
                <ContinuousMovementCommands />
                <MissionQuitCommands />
                <RewardForCollectingItem>
                    <Item reward="-1" type="stained_glass"/>
                </RewardForCollectingItem>
                <RewardForDiscardingItem>
                    <Item reward="1" type="stained_glass"/>
                </RewardForDiscardingItem>''' + malmoutils.get_video_xml(agent_host) + '''
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

my_mission = MalmoPython.MissionSpec(missionXML,True)
num_missions = 10 if agent_host.receivedArgument("test") else 30000
for mission_no in range(num_missions):
    merges_allowed = mission_no % 2
    my_mission_record = malmoutils.get_default_recording_object(agent_host, "Mission_{}".format(mission_no + 1))
    max_retries = 3
    for retry in range(max_retries):
        try:
            agent_host.startMission( my_mission, my_mission_record )
            break
        except RuntimeError as e:
            print(e)
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

    last_inventory = None
    last_box_colour = None
    boxes_traversed = 0
    num_swaps = 0
    num_merges = 0
    # Track which boxes have been sorted:
    completed_boxes = { col:False for col in colours }

    total_reward = 0
    turn_speed = 0.2
    # Start the agent turning:
    agent_host.sendCommand("turn " + str(turn_speed))
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_rewards_since_last_state > 0:
            total_reward += world_state.rewards[-1].getValue()
        if world_state.number_of_observations_since_last_state > 0:
            obs = json.loads(world_state.observations[-1].text)

            # Are we looking at a shulker box? If so, we want to know its colour.
            # We use the ObservationFromRay to find this out.
            box_colour = None
            if u'LineOfSight' in obs:
                los = obs[u'LineOfSight']
                box_colour = los[u'type'].split("_shulker")[0].upper() # eg "brown_shulker_box" -> "BROWN"
                if not box_colour in colours:
                    continue
                if box_colour != last_box_colour:
                    last_box_colour = box_colour
                    boxes_traversed += 1
                    if boxes_traversed % 16 == 0:
                        turn_speed *= 0.9   # Get slower with each lap.
                        agent_host.sendCommand("turn " + str(turn_speed))

            # In our setup, we know that the "foreign" inventories will all be named "shulkerBox",
            # but here is how we could check for the presence of an inventory by using the "inventoriesAvailable"
            # data that ObservationFromFullInventory returns. We also use this to get the size of the
            # inventories.
            # (Note that we can also get the inventory names from the list of inventory contents - but if there
            # is nothing in the inventory this method will fail.)
            invSizeLocal, invSizeForeign = 0, 0
            foreignInventory = ""
            if u'inventoriesAvailable' in obs:
                for inv in obs[u'inventoriesAvailable']:
                    if inv[u'name'] == "inventory": # Player's inventory is always called "inventory".
                        invSizeLocal = inv[u'size']
                    else:
                        foreignInventory = str(inv[u'name'])
                        invSizeForeign = inv[u'size']

            # Now look for potential swaps. We need this check against last_inventory because
            # the swap command, and the observation generation, take place on the Minecraft server. This adds
            # some lag, meaning that we won't instantly see the result of the swap - and so the code which looks
            # for useful swaps will keep generating the same swap command (thus undoing itself).
            if u'inventory' in obs and last_inventory != obs[u'inventory']:
                # The inventory has changed, so proceed.
                last_inventory = obs[u'inventory']           

                # Create a list of the available contents.
                # This will create an InventoryObject for every Minecraft ItemStack in both the player's
                # inventory, and the "foreign" inventory (our shulker box).
                inv = [InventoryObject(**k) for k in obs[u'inventory']]
                if boxCompleted(inv, box_colour, merges_allowed):
                    if not completed_boxes[box_colour]:
                        completed_boxes[box_colour] = True
                        print("Completed " + box_colour + " box.")
                        if not False in list(completed_boxes.values()):
                            print("ALL BOXES COMPLETED!")
                            agent_host.sendCommand("turn 0")
                            time.sleep(1)   # Short pause to allow final rewards to get processed
                            agent_host.sendCommand("quit")

                # We want to track empty slots in both inventories. To do this, we create a list with a
                # numbered entry for each slot, and whenever we encounter an item we mark its slot as used
                # by setting that entry to a (high) sentinel value. We can then find the lowest available
                # slot by finding the min value in the list.
                ourUsedSlots = list(range(invSizeLocal))
                theirUsedSlots = list(range(invSizeForeign))

                # Now look for a good swap.
                # The best-case scenario is swapping a wrong coloured item for a right coloured item.
                # If we have no right-coloured items to swap in, then swap the wrong coloured items with an
                # empty slot in our inventory - unless we already own an item of the (same) wrong colour, in which
                # case combine them (if merges_allowed == True).
                # If the box has no wrong-coloured item to swap out, then swap with an empty slot in the box's inventory.
                # If there is nothing to swap in, and nothing to swap out... do nothing.
                wrongColouredItem, rightColouredItem = None, None
                for i in inv:
                    if i.inventory == "inventory":
                        ourUsedSlots[i.index] = invSizeLocal
                        if i.colour == box_colour and rightColouredItem == None:
                            rightColouredItem = i
                    else:
                        theirUsedSlots[i.index] = invSizeForeign
                        if i.colour != box_colour and wrongColouredItem == None:
                            wrongColouredItem = i

                # The swap command takes two inventory locations as parameters, and swaps the ItemStacks
                # in those locations. These locations are of the form [inventory_name]:slot_index.
                # If inventory_name is unspecified, the player's inventory (called "inventory") is assumed.
                # Eg:
                # swapInventoryItems 0 4                        // swap the items in slots 0 and 4 of the player's inventory
                # swapInventoryItems inventory:0 inventory:4    // same thing
                # swapInventoryItems shulkerBox:0 4             // swap player's item 4 with the item in slot 0 of the box
                # (The combineInventoryItems command works the same way.)
                merging = False
                ourFirstSlot = min(ourUsedSlots)
                theirFirstSlot = min(theirUsedSlots)
                source = str(rightColouredItem.index) if rightColouredItem else str(ourFirstSlot)
                dest = str(foreignInventory) + ":" + (str(wrongColouredItem.index) if wrongColouredItem else str(theirFirstSlot))
                if merges_allowed and wrongColouredItem and not rightColouredItem:
                    # We don't have anything of the right colour to swap in, but we want to get the wrong coloured item out.
                    # Do we have to take the wrong item into an empty slot in our inventory, or can we actually combine it with
                    # an item stack we already own?
                    matching_slots = [i.index for i in inv if i.inventory == "inventory" and i.colour == wrongColouredItem.colour]
                    if len(matching_slots):
                        source = str(matching_slots[0])
                        merging = True

                sourceCol = rightColouredItem.colour if rightColouredItem else "empty"
                destCol = wrongColouredItem.colour if wrongColouredItem else "empty"
                if rightColouredItem or wrongColouredItem:
                    if merging:
                        print(" " * int(-total_reward), "Merging " + dest + "(" + destCol + ") into our inventory")
                        num_merges += 1
                        agent_host.sendCommand("combineInventoryItems " + source + " " + dest)
                    else:
                        print(" " * int(-total_reward), "Swapping " + source + "(" + sourceCol + ") and " + dest + "(" + destCol + ")")
                        num_swaps += 1
                        agent_host.sendCommand("swapInventoryItems " + source + " " + dest)

    # Mission has ended.
    # Get final reward:
    if world_state.number_of_rewards_since_last_state > 0:
        total_reward += world_state.rewards[-1].getValue()

    test_passed = True
    if False in list(completed_boxes.values()):
        test_passed = False
        print("FAILED TO SORT BOXES: ")
        print([k for k in completed_boxes if not completed_boxes[k]])
    else:
        print("Mission over - sorted boxes with " + str(num_swaps) + " swap commands and " + str(num_merges) + " merge commands (" + str(old_div(float(boxes_traversed),16.0)) + " laps).")
        print("Final reward: ", total_reward)
        if total_reward != 0:
            print("Final reward should have been zero.")
            test_passed = False

    if not test_passed and agent_host.receivedArgument("test"):
        print("TEST FAILED")
        exit(1)
