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

from builtins import range
import MalmoPython
import json
import math
import os
import random
import sys
import time
import shutil
import uuid
import errno
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
agent_host.addOptionalStringArgument( "savesDir,s", "Location of Minecraft saves folder", "" )
malmoutils.parse_command_line(agent_host)
recordingsDirectory = malmoutils.get_recordings_directory(agent_host)
video_requirements = '<VideoProducer><Width>860</Width><Height>480</Height></VideoProducer>' if agent_host.receivedArgument("record_video") else ''

# Test that FileWorldGenerator works.
# First create a world using the FlatWorldGenerator, then attempt to copy the world file.
# Then start a new mission using that file.

localSavesDirectory = os.path.join(os.getcwd(), "saved_world_tests")

try:
    os.makedirs(localSavesDirectory)
except OSError as exception:
    if exception.errno != errno.EEXIST: # ignore error if already existed
        raise

saved_filename = os.path.join(localSavesDirectory, str(uuid.uuid1()))

def genItems():
    items = ""
    for x in range(4):
        for z in range(4):
            items += '<DrawBlock x="' + str(x * 2000) + '" y="3" z="' + str(z * 2000) + '" type="redstone_block"/>'
            items += '<DrawItem x="' + str(x * 2000) + '" y="10" z="' + str(z * 2000) + '" type="emerald"/>'
    return items

def cleanup():
    print("Cleaning up - deleting " + saved_filename)
    shutil.rmtree(saved_filename, ignore_errors=True)
    
def startMission(agent_host, xml, record_description):
    my_mission = MalmoPython.MissionSpec(xml, True)
    my_mission_record = MalmoPython.MissionRecordSpec()
    if recordingsDirectory:
        my_mission_record.recordRewards()
        my_mission_record.recordObservations()
        my_mission_record.recordCommands()
        if agent_host.receivedArgument("record_video"):
            my_mission_record.recordMP4(24,2000000)
        if recordingsDirectory:
            my_mission_record.setDestination(recordingsDirectory + "//" + record_description + ".tgz")

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
        for error in world_state.errors:
            print("Error:",error.text)
        if len(world_state.errors) > 0:
            exit(1)

createWorldXML = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Create a world</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;;1;" forceReset="true" destroyAfterUse="false"/>
                <DrawingDecorator>''' + genItems() + '''
                    <DrawBlock x="-100" y="10" z="400" type="diamond_block"/>
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="1000" description="time_up"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Fred</Name>
            <AgentStart>
                <Placement x="-100.5" y="4" z="400.5"/>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullStats/>
                <ContinuousMovementCommands/>
                <RewardForMissionEnd>
                    <Reward description="time_up" reward="100.0"/>
                </RewardForMissionEnd>''' + video_requirements + '''
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

cleanserWorldXML = '''<?xml version="1.0" encoding="UTF-8" ?>
<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <About>
        <Summary>Pallette Cleanser</Summary>
    </About>

    <ServerSection>
        <ServerHandlers>
            <FlatWorldGenerator generatorString="3;7,5*1,5*3,5*12,90*9;1;"/>
            <ServerQuitFromTimeUp timeLimitMs="100000" description="time_up"/>
            <ServerQuitWhenAnyAgentFinishes />
        </ServerHandlers>
    </ServerSection>

    <AgentSection mode="Survival">
        <Name>Esther Williams</Name>
        <AgentStart>
            <Placement x="-1000.5" y="200" z="-1000.5"/>
        </AgentStart>
        <AgentHandlers>
            <ObservationFromFullStats/>
            <RewardForMissionEnd rewardForDeath="100.0">
                <Reward description="time_up" reward="-900.0"/>
            </RewardForMissionEnd>''' + video_requirements + '''
        </AgentHandlers>
    </AgentSection>

</Mission>'''

loadWorldXML = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>Load a world</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FileWorldGenerator src="''' + saved_filename + '''"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>Buffy</Name>
            <AgentStart>
                <Placement x="-100.5" y="4" z="400.5"/>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullInventory/>
                <AbsoluteMovementCommands/>
                <MissionQuitCommands/>
                <RewardForCollectingItem>
                    <Item type="emerald" reward="1"/>
                </RewardForCollectingItem>''' + video_requirements + '''
                </AgentHandlers>
        </AgentSection>

    </Mission>'''

saveFolders = []
savesDir = agent_host.getStringArgument("savesDir")
if len(savesDir):
    if not os.path.isdir(savesDir):
        print("Error - supplied saves folder not found. Can not proceed with test.")
        exit(1)
    else:
        saveFolders.append(savesDir)

if len(saveFolders) == 0:
    # Try to find any Minecraft save folders.
    # Begin from our current location.
    currentFolder = os.getcwd()
    lastFolder = ""

    while currentFolder != lastFolder:
        minecraftDir = os.path.join(currentFolder, "Minecraft")
        if os.path.isdir(minecraftDir):
            # Is there a saves folder, or a run/saves folder?
            savesDir = os.path.join(minecraftDir, "saves")
            if os.path.isdir(savesDir):
                saveFolders.append(savesDir)
            runDir = os.path.join(minecraftDir, "run")
            runSavesDir = os.path.join(runDir, "saves")
            if os.path.isdir(runSavesDir):
                saveFolders.append(runSavesDir)
        lastFolder = currentFolder
        currentFolder = os.path.normpath(os.path.join(currentFolder, ".."))

    if len(saveFolders) == 0:
        print("Couldn't find the Minecraft saves folder - this test needs to be run somewhere that has access to the Minecraft client.")
        print("Please re-run from a different location, or explicitly pass in the saves folder location.")
        exit(1)

# Now we have a list of save folders, build a list of all the saved worlds contained therein.
initialSavedWorlds = []
for dir in saveFolders:
    for item in os.listdir(dir):
        initialSavedWorlds.append(os.path.join(dir, item))

# First mission: create a flatworld, add emeralds, run around until time runs out.
print("Start creation mission.")
startMission(agent_host, createWorldXML, "createWorld")
agent_host.sendCommand("move 1")
agent_host.sendCommand("turn 0.2") 
world_state = agent_host.peekWorldState()
while world_state.is_mission_running:
    world_state = agent_host.peekWorldState()
print("Creation mission over.")
world_state = agent_host.getWorldState()
if world_state.rewards[-1].getValue() != 100:
    print("Got incorrect reward - should have received 100 for time-up.")
    exit(1)

# Wait a bit...
time.sleep(4)

# List all the saved worlds again:
currentSavedWorlds = []
for dir in saveFolders:
    for item in os.listdir(dir):
        currentSavedWorlds.append(os.path.join(dir, item))

# Find the new worlds:
newWorlds = []
for world in currentSavedWorlds:
    if not world in initialSavedWorlds:
        print("Found new saved world: " + world)
        newWorlds.append(world)

if len(newWorlds) != 1:
    print("Couldn't find the new world file - cannot proceed with test.")
    exit(1)

# We don't try to copy this yet - Minecraft won't have saved our DrawingDecorator changes to it.
# Instead, start the next mission - this will force the saving.
# Second mission: to ensure a decent test, create a totally different world before reloading the first world.
print("Start pallette cleanser.")
startMission(agent_host, cleanserWorldXML, "palletteCleanser")
world_state = agent_host.peekWorldState()
# We should just drown and die.
current_air = 0
while world_state.is_mission_running:
    world_state = agent_host.peekWorldState()
    if world_state.number_of_observations_since_last_state > 0:
        obs = json.loads( world_state.observations[-1].text )
        if "Air" in obs:
            air = obs[u"Air"]
            if air != current_air:
                print("Air remaining: " + str(air))
                current_air = air
print("Cleanser mission over.")
world_state = agent_host.getWorldState()
if world_state.rewards[-1].getValue() != 100:
    print("Got incorrect reward - should have received 100 for drowning.")
    exit(1)

# Now, try to copy the previous world files:
print("Making copy of saved world " + newWorlds[0] + "...")
try:
    shutil.copytree(newWorlds[0], saved_filename)
except OSError as exc:
    print("Failed to copy saved world - " + exc)
    print("Can not proceed with test.")
    exit(1)

# Third mission: attempt to load the world we just copied.
print("Start loading mission.")
startMission(agent_host, loadWorldXML, "loadWorld")
world_state = agent_host.peekWorldState()
# Try to collect all the emeralds:
total_reward = 0
for x in range(4):
    for z in range(4):
        tp_command = "tp " + str(x * 2000) + " 4 " + str(z * 2000)
        print("Sending command: " + tp_command)
        agent_host.sendCommand(tp_command)
        world_state = agent_host.peekWorldState()
        if not world_state.is_mission_running:
            print("Mission ended prematurely - error.")
            exit(1)
        time.sleep(5) # Wait to make sure we collected the emerald.
        world_state = agent_host.getWorldState()
        if world_state.number_of_rewards_since_last_state > 0:
            total_reward += world_state.rewards[-1].getValue()
            print("Total reward: " + str(total_reward))
agent_host.sendCommand("quit")
while world_state.is_mission_running:
    world_state = agent_host.peekWorldState()

print("Loaded mission over.")
world_state = agent_host.getWorldState()
if world_state.number_of_rewards_since_last_state > 0:
    total_reward += world_state.rewards[-1].getValue()
if total_reward != 16:
    print("Got incorrect reward - should have received 16 for collecting 16 emeralds.")
    cleanup()
    exit(1)

print("Test successful")
cleanup()
exit(0)
