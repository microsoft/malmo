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

# Sample to demonstrate use of basic crafting.
# 4 bowls = 3 Oak Wood Planks
# 1 item.rabbitStew = 1 x item.rabbitCooked(Cooked Rabbit), 1 x item.carrots(Carrot), 1 x item.potatoBaked(Baked Potato), 1 x tile.mushroom(Mushroom), 1 x item.bowl(Bowl)

import MalmoPython
import os
import random
import sys
import time
import json
import random
import errno

items=["planks", "planks", "planks", "cooked_rabbit", "carrot", "baked_potato", "brown_mushroom"]

def buildPositionList(items):
    positions=[]
    for item in items:
        positions.append((random.randint(-10,10), random.randint(-10,10)))
    return positions

def getItemDrawing(positions):
    drawing=""
    index=0
    for p in positions:
        drawing += '<DrawItem x="' + str(p[0]) + '" y="228" z="' + str(p[1]) + '" type="' + items[index] + '" />'
        index += 1
    return drawing

def getSubgoalPositions(positions):
    goals=""
    for p in positions:
        goals += '<Point x="' + str(p[0]) + '" y="227" z="' + str(p[1]) + '" tolerance="1" description="ingredient" />'
    return goals

def checkInventoryForBowlIngredients(obs):
    # Need three planks
    plank_count = 0
    for i in xrange(0,10):
        key = 'InventorySlot_'+str(i)+'_item'
        if key in obs:
            item = obs[key]
            if item == 'tile.wood':
                plank_count += int(obs[u'InventorySlot_'+str(i)+'_size'])
            if item == 'item.bowl':
                return False    # Already have a bowl, so don't want another one!
    return plank_count >= 3

def checkInventoryForStewIngredients(obs):
    # Need a bowl, a cooked rabbit, a carrot, a mushroom and a baked potato.
    required=["item.rabbitCooked", "item.potatoBaked", "item.bowl", "item.carrots", "tile.mushroom"]
    for i in xrange(0,10):
        key = 'InventorySlot_'+str(i)+'_item'
        if key in obs:
            item = obs[key]
            if item in required:
                required.remove(item)
            if item == 'item.rabbitStew':
                return False    # Already have the stew.
    return len(required) == 0
    
def GetMissionXML(summary):
    ''' Build an XML mission string that uses the RewardForCollectingItem mission handler.'''
    
    positions = buildPositionList(items)
    
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>''' + summary + '''</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
                <DrawingDecorator>
                    <DrawCuboid x1="-50" y1="226" z1="-50" x2="50" y2="226" z2="50" type="carpet" colour="RED" face="UP"/>
                    ''' + getItemDrawing(positions) + '''
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="150000"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Delia</Name>
            <AgentStart>
                <Placement x="0.5" y="227.0" z="0.5"/>
                <Inventory>
                </Inventory>
            </AgentStart>
            <AgentHandlers>
                <RewardForCollectingItem>
                    <Item reward="10" type="planks"/>
                    <Item reward="100" type="cooked_rabbit carrot baked_potato brown_mushroom"/>
                    <Item reward="500" type="bowl"/>
                    <Item reward="1000" type="rabbit_stew"/>
                </RewardForCollectingItem>
                <RewardForDiscardingItem>
                    <Item reward="-2" type="planks"/>
                    <Item reward="-6" type="cooked_rabbit carrot baked_potato brown_mushroom"/>
                </RewardForDiscardingItem>
                <ContinuousMovementCommands turnSpeedDegs="480"/>
                <SimpleCraftCommands/>
                <ObservationFromSubgoalPositionList>''' + getSubgoalPositions(positions) + '''
                </ObservationFromSubgoalPositionList>
                <ObservationFromFullInventory/>
            </AgentHandlers>
        </AgentSection>

    </Mission>'''
  
sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

validate = True

# Expected reward made up as follows (see RewardForCollectingItem, above)
# POSITIVE REWARDS:
#   3 * 10 for collecting planks = 30
#   100 each for collecting rabbit, carrot, potato and mushrooms = 400
#   4 * 500 for crafting wooden bowls = 2000
#   Crafting the rabbit stew = 1000
# NEGATIVE REWARDS:
#   3 * -2 for losing the planks (during crafting) = -6
#   -6 each for losing the rabbit, carrot, potato and mushrooms (during crafting) = -24
# TOTAL: 3400
expected_reward = 3400

# Create a pool of Minecraft Mod clients.
# By default, mods will choose consecutive mission control ports, starting at 10000,
# so running four mods locally should produce the following pool by default (assuming nothing else
# is using these ports):
my_client_pool = MalmoPython.ClientPool()
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10000))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10001))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10002))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10003))

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

if agent_host.receivedArgument("test"):
    num_reps = 1
else:
    num_reps = 30000

for iRepeat in range(num_reps):
    my_mission = MalmoPython.MissionSpec(GetMissionXML("Crafty #" + str(iRepeat)),validate)
    # Set up a recording - MUST be done once for each mission - don't do this outside the loop!
    my_mission_record = MalmoPython.MissionRecordSpec()
    max_retries = 3
    for retry in range(max_retries):
        try:
            # Attempt to start the mission:
            agent_host.startMission( my_mission, my_client_pool, my_mission_record, 0, "craftTestExperiment" )
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print "Error starting mission",e
                print "Is the game running?"
                exit(1)
            else:
                time.sleep(2)

    world_state = agent_host.getWorldState()
    while not world_state.is_mission_running:
        time.sleep(0.1)
        world_state = agent_host.getWorldState()

    total_reward = 0
    # main loop:
    agent_host.sendCommand( "move 1" )
    print "Collecting ingredients..."
    while world_state.is_mission_running:
        if world_state.number_of_observations_since_last_state > 0:
            msg = world_state.observations[-1].text
            ob = json.loads(msg)
            if u'yawDelta' in ob:
                current_yaw_delta = ob.get(u'yawDelta', 0)
                agent_host.sendCommand( "turn " + str(current_yaw_delta) )
                agent_host.sendCommand( "move " + str(1.0 - abs(current_yaw_delta)) )
            else:
                agent_host.sendCommand("move 0")
                agent_host.sendCommand("turn 0")
                if checkInventoryForBowlIngredients(ob):
                    print "Crafting a bowl..."
                    agent_host.sendCommand("craft bowl")
                    time.sleep(1)
                elif checkInventoryForStewIngredients(ob):
                    print "Crafting a stew..."
                    agent_host.sendCommand("craft rabbit_stew")
                    time.sleep(1)
        if world_state.number_of_rewards_since_last_state > 0:
            reward = world_state.rewards[-1].getValue()
            print "Reward: " + str(reward)
            total_reward += reward
        world_state = agent_host.getWorldState()
        
    # mission has ended.
    for error in world_state.errors:
        print "Error:",error.text
    print "Total Reward: " + str(total_reward)
    if total_reward != expected_reward:
        print "Total reward did not match expected reward - did the crafting work?"
    time.sleep(0.5) # Give the mod a little time to prepare for the next mission.