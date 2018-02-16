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

import MalmoPython
import os
import sys
import time
import json

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately


def processFrame(frame):
    width = frame.width
    height = frame.height
    pixels = frame.pixels
    y1 = height / 4
    y2 = 3 * height / 4
    x1 = height / 4
    x2 = 3 * height / 4
    left_top_intensity = get_intensity(x1, y1, width)
    left_bottom_intensity = get_intensity(x1, y2, width)
    right_top_intensity = get_intensity(x2, y1, width)
    right_bottom_intensity = get_intensity(x2, y2, width)
    return max(left_top_intensity, left_bottom_intensity), max(right_top_intensity, right_bottom_intensity)

def get_intensity(x, y, width):
    pixels = frame.pixels
    i = x + (y * width)
    r = pixels[i * 3]
    g = pixels[i * 3 + 1]
    b = pixels[i * 3 + 2]
    brightness = (r + g + b) / 3
    return brightness / 255.0

def calc_yaw_and_velocity(left, right):
    wheel_span = 0.5
    yaw = (right - left) / wheel_span
    # velocity = 0.7 # min(left, right) + abs(yaw / 2)
    velocity = 0.7 if (min(left, right) > 0.1) else -0.7
    return yaw, velocity

missionXML='''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
            <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            
              <About>
                <Summary>Hello world!</Summary>
              </About>
              
              <ServerSection>
                <ServerInitialConditions>
                    <Time>
                        <StartTime>1000</StartTime>
                        <AllowPassageOfTime>false</AllowPassageOfTime>
                    </Time>
                </ServerInitialConditions>
                <ServerHandlers>
                  <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1"/>
					<DrawingDecorator>
						<DrawCuboid x1="0" y1="46" z1="0" x2="36" y2="50" z2="36" type="air" />
						<DrawCuboid x1="0" y1="45" z1="0" x2="35" y2="45" z2="36" type="stone"/>
						<DrawBlock x="15"  y="45" z="7" type="glowstone" />
						<DrawBlock x="4"  y="46" z="1" type="glowstone" />
						<DrawBlock x="4"  y="47" z="1" type="glowstone" />
						<DrawBlock x="4"  y="48" z="1" type="glowstone" />
						<DrawBlock x="4"  y="49" z="1" type="glowstone" />
						<DrawBlock x="4"  y="50" z="1" type="glowstone" />
						<DrawBlock x="4"  y="46" z="1" type="glowstone" />
						<DrawBlock x="4"  y="47" z="1" type="glowstone" />
						<DrawBlock x="4"  y="48" z="1" type="glowstone" />
						<DrawBlock x="4"  y="49" z="1" type="glowstone" />
						<DrawBlock x="4"  y="50" z="1" type="glowstone" />
						<DrawBlock x="28"  y="46" z="8" type="glowstone" />
						<DrawBlock x="28"  y="47" z="8" type="glowstone" />
						<DrawBlock x="28"  y="48" z="8" type="glowstone" />
						<DrawBlock x="28"  y="49" z="8" type="glowstone" />
						<DrawBlock x="28"  y="50" z="8" type="glowstone" />
						<DrawBlock x="12"  y="45" z="15" type="glowstone" />
						<DrawBlock x="4"  y="45" z="26" type="glowstone" />
						<DrawBlock x="20"  y="46" z="32" type="glowstone" />
						<DrawBlock x="34"  y="46" z="15" type="glowstone" />
					</DrawingDecorator>
                  <ServerQuitFromTimeUp timeLimitMs="300000"/>
                  <ServerQuitWhenAnyAgentFinishes/>
                </ServerHandlers>
              </ServerSection>
              
              <AgentSection mode="Survival">
                <Name>Breitenburg#1</Name>
                <AgentStart>
                    <Placement x="4.5" y="50" z="0.5" yaw="-70" pitch="20"/>
					<Inventory>
						<InventoryItem type="diamond_pickaxe" slot="0"/>
					</Inventory>
                </AgentStart>
                <AgentHandlers>
                  <ObservationFromRay/>
				  <ObservationFromFullStats/>
                  <ContinuousMovementCommands turnSpeedDegs="180"/>
                  <VideoProducer>
                    <Width>640</Width>
                    <Height>480</Height>
                  </VideoProducer>
                </AgentHandlers>
              </AgentSection>
            </Mission>'''

# Create default Malmo objects:

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

my_mission = MalmoPython.MissionSpec(missionXML, True)
my_mission_record = MalmoPython.MissionRecordSpec()

# Attempt to start a mission:
max_retries = 3
for retry in range(max_retries):
    try:
        agent_host.startMission( my_mission, my_mission_record )
        break
    except RuntimeError as e:
        if retry == max_retries - 1:
            print "Error starting mission:",e
            exit(1)
        else:
            time.sleep(2)

# Loop until mission starts:
print "Waiting for the mission to start ",
world_state = agent_host.getWorldState()
while not world_state.has_mission_begun:
    sys.stdout.write(".")
    time.sleep(0.1)
    world_state = agent_host.getWorldState()
    for error in world_state.errors:
        print "Error:",error.text

print
print "Mission running ",

# Loop until mission ends:
while world_state.is_mission_running:
    world_state = agent_host.getWorldState()
    if world_state.number_of_video_frames_since_last_state > 0:
        frame = world_state.video_frames[-1]
        left_brightness, right_brightness = processFrame(frame)
        print left_brightness, right_brightness
        yaw, speed = calc_yaw_and_velocity(left_brightness, right_brightness)
        agent_host.sendCommand("turn " + str(yaw))
        agent_host.sendCommand("move " + str(speed * 3))
    if world_state.number_of_observations_since_last_state > 0:
        msg = world_state.observations[-1].text
        ob = json.loads(msg)
        if u'LineOfSight' in ob:
            block = ob[u'LineOfSight']
            type = block["type"]
            if type == "glowstone":
                agent_host.sendCommand("attack 1")
            else:
				agent_host.sendCommand("attack 0")
	for error in world_state.errors:
		print "Error:",error.text

print
print "Mission ended"
# Mission has ended.


agent_host.sendCommand("move 1")
