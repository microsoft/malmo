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

from builtins import range
from past.utils import old_div
import MalmoPython
import json
import math
import os
import random
import sys
import time
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)
recordingsDirectory = malmoutils.get_recordings_directory(agent_host)
video_requirements = '<VideoProducer><Width>860</Width><Height>480</Height></VideoProducer>' if agent_host.receivedArgument("record_video") else ''

#-------------------------------------------------------------------------------------------------------------------------------------
#Very simple script to test drawing code - starts a mission in order to draw, but quits after a second.
#-------------------------------------------------------------------------------------------------------------------------------------

def Menger(xorg, yorg, zorg, size, blocktype, holetype):
    #draw solid chunk
    genstring = GenCuboid(xorg,yorg,zorg,xorg+size-1,yorg+size-1,zorg+size-1,blocktype) + "\n"
    #now remove holes
    unit = size
    while (unit >= 3):
        w=old_div(unit,3)
        for i in range(0, size, unit):
            for j in range(0, size, unit):
                x=xorg+i
                y=yorg+j
                genstring += GenCuboid(x+w,y+w,zorg,(x+2*w)-1,(y+2*w)-1,zorg+size-1,holetype) + "\n"
                y=yorg+i
                z=zorg+j
                genstring += GenCuboid(xorg,y+w,z+w,xorg+size-1, (y+2*w)-1,(z+2*w)-1,holetype) + "\n"
                genstring += GenCuboid(x+w,yorg,z+w,(x+2*w)-1,yorg+size-1,(z+2*w)-1,holetype) + "\n"
                if (w == 1):
                    genstring += GenItem(x+w, yorg+size+100, z+w, "diamond") + "\n"
        unit = w
    return genstring

def GenCuboid(x1, y1, z1, x2, y2, z2, blocktype):
    return '<DrawCuboid x1="' + str(x1) + '" y1="' + str(y1) + '" z1="' + str(z1) + '" x2="' + str(x2) + '" y2="' + str(y2) + '" z2="' + str(z2) + '" type="' + blocktype + '"/>'

def GenItem(x, y, z, itemtype):
    return '<DrawItem x="' + str(x) + '" y="' + str(y) + '" z="' + str(z) + '" type="' + itemtype + '"/>'

#----------------------------------------------------------------------------------------------------------------------------------

missionXML = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>It's Fun To Build Things!</Summary>
        </About>

        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,44*49,73,35:1,159:4,95:13,35:13,159:11,95:10,159:14,159:6,35:6,95:6;157;decoration,lake" />
                <DrawingDecorator>
                    '''+Menger(0,56,420,27,"wool","air")+'''
                    <DrawSphere x="10" y="70" z="420" radius="10" type="ice" />
                    <DrawCuboid x1="0" y1="56" z1="419" x2="27" y2="81" z2="419" type="ladder" face="NORTH"/>
                    <DrawLine x1="0" y1="56" z1="447" x2="27" y2="66" z2="447" type="quartz_block" steptype="quartz_stairs" face="EAST"/>
                    <DrawLine x1="27" y1="66" z1="447" x2="27" y2="76" z2="419" type="quartz_block" steptype="quartz_stairs" face="NORTH"/>
                    <DrawLine x1="27" y1="76" z1="419" x2="0" y2="82" z2="419" type="quartz_block" steptype="quartz_stairs" face="WEST"/>
                    <DrawLine x1="27" y1="82" z1="420" x2="127" y2="182" z2="420" type="redstone_block"/>
                    <DrawLine x1="27" y1="83" z1="420" x2="127" y2="183" z2="420" type="air"/>
                    <DrawLine x1="27" y1="83" z1="420" x2="127" y2="183" z2="420" type="golden_rail"/>

                    <DrawLine x1="127" y1="162" z1="420" x2="137" y2="152" z2="420" type="stone" variant="smooth_granite"/>
                    <DrawLine x1="127" y1="163" z1="420" x2="137" y2="153" z2="420" type="air"/>
                    <DrawLine x1="127" y1="163" z1="420" x2="137" y2="153" z2="420" type="rail"/>

                    <DrawLine x1="138" y1="152" z1="420" x2="228" y2="55" z2="520" type="stone" variant="andesite"/>
                    <DrawLine x1="138" y1="153" z1="420" x2="228" y2="56" z2="520" type="air"/>
                    <DrawLine x1="138" y1="153" z1="420" x2="228" y2="56" z2="520" type="rail"/>

                    <DrawCuboid x1="228" y1="10" z1="467" x2="278" y2="56" z2="567" type="air" />
                    <DrawItem x="0" y="86" z="420" type="minecart" />
                    <DrawItem x="0" y="86" z="420" type="minecart" />
                    <DrawItem x="0" y="86" z="420" type="minecart" />
                    <DrawItem x="0" y="86" z="420" type="minecart" />
                    <DrawCuboid x1="229" y1="5" z1="519" x2="236" y2="9" z2="526" type="water" />
                </DrawingDecorator>
                <ServerQuitFromTimeUp timeLimitMs="1000" description="out_of_time"/>
                <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers>
        </ServerSection>

        <AgentSection mode="Survival">
            <Name>The Explorer</Name>
            <AgentStart>
                <Placement x="0.5" y="83.0" z="420.5"/>
            </AgentStart>
            <AgentHandlers>
                <ObservationFromFullStats/>
                <ObservationFromGrid>
                    <Grid name="nearby">
                        <min x="-1" y="-1" z="-1"/>
                        <max x="1" y="-1" z="1"/>
                    </Grid>
                    <Grid name="far" absoluteCoords="true">
                        <min x="12" y="79" z="417"/>
                        <max x="14" y="79" z="419"/>
                    </Grid>
                    <Grid name="very_far" absoluteCoords="true">
                        <min x="-10711" y="55" z="347"/>
                        <max x="-10709" y="55" z="349"/>
                    </Grid>
                </ObservationFromGrid>''' + video_requirements + '''
            </AgentHandlers>
        </AgentSection>

    </Mission>'''

my_mission = MalmoPython.MissionSpec(missionXML,True)
my_mission_record = MalmoPython.MissionRecordSpec()
if recordingsDirectory:
    my_mission_record.recordRewards()
    my_mission_record.recordObservations()
    my_mission_record.recordCommands()
    my_mission_record.setDestination(recordingsDirectory + "//" + "Mission_1.tgz")
    if agent_host.receivedArgument("record_video"):
        my_mission_record.recordMP4(24,2000000)

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
    
while world_state.is_mission_running:
    world_state = agent_host.peekWorldState()

# We also use this sample as a test. In this section we verify that
# the expected things were received.
if agent_host.receivedArgument("test"):
    # check the height of the player in the last observation    
    assert len(world_state.observations) > 0, 'No observations received'
    obs = json.loads( world_state.observations[-1].text )
    player_y = obs[u'YPos']
    print('Player at y =',player_y)
    assert math.fabs( player_y - 83.0 ) < 0.01, 'Player not at expected height'
    
    # check the grid observations
    for obs in world_state.observations:
        assert '"nearby":["air","quartz_block","quartz_block","air","wool","wool","air","wool","air"]' in obs.text, 'Nearby observation incorrect:'+obs.text
        assert '"far":["ice","ice","air","ice","ice","air","quartz_block","quartz_block","quartz_block"]' in obs.text, 'Far observation incorrect:'+obs.text
        assert '"very_far":["stained_glass","stained_glass","stained_glass","stained_glass","stained_glass","stained_glass","stained_glass","stained_glass","stained_glass"]' in obs.text, 'Vey far observation incorrect:'+obs.text

# mission has ended.
print("Mission over - feel free to explore the world.")
