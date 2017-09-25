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

# Work in progress: this will eventually be a test of Malmo's support for all block types.

from builtins import range
from past.utils import old_div
import MalmoPython
import os
import sys
import time
import json
import copy
import errno
import xml.etree.ElementTree as ET

# Parse schema to collect all block types.
# First find the schema file:
schema_dir = None
try:
    schema_dir = os.environ['MALMO_XSD_PATH']
except KeyError:
    print("MALMO_XSD_PATH not set? Check environment.")
    exit(1)
types_xsd = schema_dir + os.sep + "Types.xsd"

# Now try to parse it:
types_tree = None
try:
    types_tree = ET.parse(types_xsd)
except (ET.ParseError, IOError):
    print("Could not find or parse Types.xsd - check Malmo installation.")
    exit(1)

# Find the BlockType element:
root = types_tree.getroot()
block_types_element = root.find("*[@name='BlockType']")
if block_types_element == None:
    print("Could not find block types in Types.xsd - file corruption?")
    exit(1)

# Find the enum inside the BlockType element:
block_types_enum = block_types_element.find("*[@base]")
if block_types_enum == None:
    print("Unexpected schema format. Did the format get changed without this test getting updated?")
    exit(1)

# Now make a list of block types:
block_types = [block.get("value") for block in block_types_enum]

def getMissionXML(block_types):
    forceReset = '"true"'
    structureXML = "<DrawingDecorator>" 
    for i, b in enumerate(block_types):
        structureXML += '<DrawBlock x="{}" y="{}" z="{}" type="{}" />'.format(i % 10, 3, old_div(i, 10), b)
    structureXML += "</DrawingDecorator>"
    startpos=(-2, 10, -2)
    
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <About>
        <Summary>Block Test</Summary>
    </About>

    <ServerSection>
        <ServerHandlers>
            <FlatWorldGenerator generatorString="3;7,2*3,2;12;" forceReset=''' + forceReset + '''/>''' + structureXML + '''
            <ServerQuitWhenAnyAgentFinishes />
        </ServerHandlers>
    </ServerSection>

    <AgentSection mode="Survival">
        <Name>Blocky</Name>
        <AgentStart>
            <Placement x="''' + str(startpos[0] + 0.5) + '''" y="''' + str(startpos[1] + 1) + '''" z="''' + str(startpos[2] + 0.5) + '''" pitch="90"/>
        </AgentStart>
        <AgentHandlers>
            <MissionQuitCommands />
            <ObservationFromFullStats/>
            <ObservationFromGrid>
                <Grid name="all_the_blocks" absoluteCoords="true">
                    <min x="0" y="3" z="0"/>
                    <max x="9" y="3" z="30"/>
                </Grid>
            </ObservationFromGrid>
            <VideoProducer viewpoint="1">
                <Width>860</Width>
                <Height>480</Height>
            </VideoProducer>
        </AgentHandlers>
    </AgentSection>
  </Mission>'''

if sys.version_info[0] == 2:
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
else:
    import functools
    print = functools.partial(print, flush=True)

agent_host = MalmoPython.AgentHost()
agent_host.addOptionalStringArgument( "recordingDir,r", "Path to location for saving mission recordings", "" )
try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print('ERROR:',e)
    print(agent_host.getUsage())
    exit(1)
if agent_host.receivedArgument("help"):
    print(agent_host.getUsage())
    exit(0)

num_iterations = 30000
if agent_host.receivedArgument("test"):
    num_iterations = 10

recording = False
my_mission_record = MalmoPython.MissionRecordSpec()
recordingsDirectory = agent_host.getStringArgument("recordingDir")
if len(recordingsDirectory) > 0:
    recording = True
    try:
        os.makedirs(recordingsDirectory)
    except OSError as exception:
        if exception.errno != errno.EEXIST: # ignore error if already existed
            raise
    my_mission_record.recordRewards()
    my_mission_record.recordObservations()
    my_mission_record.recordCommands()
    my_mission_record.recordMP4(24,2000000)

for i in range(num_iterations):
    missionXML = getMissionXML(block_types)
    if recording:
        my_mission_record.setDestination(recordingsDirectory + "//" + "Mission_" + str(i+1) + ".tgz")
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

    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        print(".", end="")
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
    print()

    # main loop:
    while world_state.is_mission_running:
        if world_state.number_of_observations_since_last_state > 0:
            msg = world_state.observations[-1].text
            ob = json.loads(msg)
            if "all_the_blocks" in ob:
                blocks = ob["all_the_blocks"]
                missing_blocks = [b for b in block_types if not b in blocks]
                if len(missing_blocks) > 0:
                    print("MISSING:")
                    for b in missing_blocks:
                        print(b, end=' ')
                    print()
        world_state = agent_host.getWorldState()
