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

from __future__ import print_function
import MalmoPython
import os
import sys
import time
import json
import random
import math
import malmoutils
import xml.etree.ElementTree

if sys.version_info[0] == 2:
    # Workaround for https://github.com/PythonCharmers/python-future/issues/262
    from Tkinter import *
else:
    from tkinter import *

malmoutils.fix_print()

# Create main Malmo object:
agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)

# The four simple behaviours that Braitenberg proposed:
behaviours = ["love","fear","aggression","exploration"]
current_behaviour = random.choice(behaviours)   # Choose one at random.

if agent_host.receivedArgument("test"):
    current_behaviour = "love"  # Should eventually bang in to a pillar, or the wall.

# Method for changing the behaviour to the next in the list:
def change_mode():
    global current_behaviour, mode_text
    current_behaviour = behaviours[(behaviours.index(current_behaviour) + 1) % 4]
    agent_host.sendCommand("chat " + current_behaviour) # Chat this to Minecraft
    mode_text.config(text = current_behaviour)  # And update our own window

# Code for creating the UI window
root = Tk()
root.wm_title("Minecraft Braitenberg Vehicle Simulator")
sensor_title = Label(root, text="Sensor values")
sensor_title.grid(row=0, column=0)
sensor_canvas = Canvas(root, borderwidth=0, width=60, height=100, bg="black")
sensor_canvas.grid(padx=5, rowspan=2, row=1, column=0)

movement_title = Label(root, text="Yaw and speed")
movement_title.grid(row=0, column=1)
movement_canvas = Canvas(root, width=100, height=100, bg="black")
movement_canvas.grid(row=1, column=1, rowspan=2)

mode_button = Button(root, text="Change Mode", command=change_mode)
mode_button.grid(row=1, column=2)
mode_text = Label(root, text=current_behaviour)
mode_text.grid(row=2, column=2)

root_frame = Frame(root)
root_frame.grid(row=3, column=0, columnspan=3)

arena_canvas = Canvas(root_frame, borderwidth=0, highlightthickness=0, width=400, height=400, bg="black")
arena_canvas.config( width=400, height=400 )
arena_canvas.pack(padx=5, pady=5)

def draw_ui(left_signal, right_signal, yaw, speed, x, z):
    sensor_canvas.delete("all")
    sensor_canvas.create_rectangle(0, 101 * (1.0 - left_signal), 30, 101, fill="#008840")
    sensor_canvas.create_rectangle(31, 101 * (1.0 - right_signal), 62, 101, fill="#008840")
    movement_canvas.delete("all")
    # Draw a line to represent our direction and speed.
    # Yaw is in degrees - we need it in radians.
    yaw = math.pi * yaw / 180.0
    movement_canvas.create_line(50, 50, 50 + 50 * speed * math.cos(yaw), 50 + 50 * speed * math.sin(yaw), width="2", fill="#00ff00")
    # Scale our x and z for the arena canvas:
    x = (x + 50) * 4
    z = (z + 50) * 4
    arena_canvas.create_oval(x - 3, z - 3, x + 3, z + 3, width=0, fill="#ffa930")
    root.update()

def processFrame(frame):
    """Simulate a left and right sensor output (from 0 to 1) given the input image."""
    # The pixels are grey-scale values from 0-255 - white = 255, black = 0
    # We want to turn the image into two values, one for the left "sensor" and one for the right.
    # There are many possible ways to do this. The very simplest way would be
    # to examine the values of two pixels - one in the centre of the left half of the image,
    # and one in the centre of the right half. What are the obvious problems with this?

    # NB: The pixels are stored in a flat (1 dimensional) array, so to calculate the index of the pixels
    # at (x,y), we do:
    # index = x + (y * width)

    width = frame.width
    height = frame.height
    pixels = frame.pixels

    USE_SIMPLE_APPROACH = False  # Change this to False to use more sophisticated approach.

    if USE_SIMPLE_APPROACH:
        # Simplest approach.
        left_centre_x = int(width / 4)      # Centre of left half
        right_centre_x = int(3 * width / 4) # Centre of right half
        centre_y = int(height / 2)          # Middle row
        left_pixel_index = left_centre_x + (centre_y * width)
        right_pixel_index = right_centre_x + (centre_y * width)
        left_sensor = float(pixels[left_pixel_index]) / 255.0
        right_sensor = float(pixels[right_pixel_index]) / 255.0
    else:
        # Obviously, the simple approach misses a lot of data, and is very susceptible to noise.
        # A better approach would be to consider more pixels.
        # You could take the average of all the pixels - it's slower but better.
        # Or you could use the median value, which is less noisy than the mean.
        # Here we calculate both, and estimate the median using histograms:
        left_total = 0
        right_total = 0
        left_hist = [0 for i in range(256)]
        right_hist = [0 for i in range(256)]
        # Create a histogram for each half of the image:
        for y in range(height):
            for x in range(int(width/2)):
                i = pixels[x + y*width]
                left_hist[i] += 1
                left_total += float(i)/255.0
            for x in range(int(width/2), width):
                i = pixels[x + y*width]
                right_hist[i] += 1
                right_total += float(i)/255.0
        # Calculate the mean values:
        left_mean, right_mean = left_total / (width*height/2), right_total / (width*height/2)
        # Now use the histogram to estimate the median value
        left_total, right_total = 0, 0
        pixels_per_half = width * height / 2
        cut_off_value = pixels_per_half / 2
        left_cut_off_point, right_cut_off_point = 0, 0
        while (left_total < cut_off_value):
            left_total += left_hist[left_cut_off_point]
            left_cut_off_point += 1
        while(right_total < cut_off_value):
            right_total += right_hist[right_cut_off_point]
            right_cut_off_point += 1
        left_median, right_median = left_cut_off_point / 255.0, right_cut_off_point / 255.0
        
        # Use the median values:
        left_sensor, right_sensor = left_median, right_median
        # Or uncomment this line to use the mean values:
        # left_sensor, right_sensor = left_mean, right_mean
    
    # In our gloomy arena, we never get particularly bright, so the sensor values tend to be low.
    # To get more action from our vehicle, we can scale up (keeping 1.0 as the max).
    # What values work well for GAIN? What happens if it's too high or too low?
    GAIN = 1.8
    left_sensor = min(1, left_sensor * GAIN)
    right_sensor = min(1, right_sensor * GAIN)
    # Done - return the values:
    return left_sensor, right_sensor

def calc_velocities(left_wheel_speed, right_wheel_speed):
    # Malmo steers the agent by giving them a forward speed (0 to 1) and a turn speed (-1 to 1).
    # Calculate these from the "wheel" speeds we're given.
    # Many ways of doing this - but this is simple and seems to work well:
    forward_velocity = max(min(left_wheel_speed, right_wheel_speed), 0.04)   # Don't let it stop completely.
    angular_velocity = (left_wheel_speed - right_wheel_speed) * forward_velocity
    return angular_velocity, forward_velocity

def get_pillars():
    # Add some randomly positioned light "pillars" to excite the vehicles.
    xml = ""
    NUMBER_OF_PILLARS = 30  # Change this if you want more/fewer pillars
    for i in range(NUMBER_OF_PILLARS):
        x = random.randint(-50, 50)
        z = random.randint(-50, 50)
        xml += '<DrawCuboid x1="{}" y1="50" z1="{}" x2="{}" y2="54" z2="{}" type="beacon"/>'.format(x, z, x, z)
    return xml

def get_mission_xml():
    # This is what tells Malmo how to build the world we are experimenting in.
    return '''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
            <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            
              <About>
                <Summary>Braitenberg Vehicles!</Summary>
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
                        <DrawCuboid x1="-50" y1="40" z1="-50" x2="50" y2="80" z2="50" type="obsidian"/>
                        <DrawCuboid x1="-50" y1="51" z1="-50" x2="50" y2="51" z2="-50" type="glowstone"/>
                        <DrawCuboid x1="-50" y1="51" z1="50" x2="-50" y2="51" z2="-50" type="glowstone"/>
                        <DrawCuboid x1="50" y1="51" z1="-50" x2="50" y2="51" z2="50" type="glowstone"/>
                        <DrawCuboid x1="-50" y1="51" z1="50" x2="50" y2="51" z2="50" type="glowstone"/>
                        <DrawCuboid x1="-49" y1="41" z1="-49" x2="49" y2="79" z2="49" type="air"/>
                        <DrawCuboid x1="-49" y1="49" z1="-49" x2="49" y2="49" z2="49" type="wool" colour="PINK"/>''' + get_pillars() + '''
                    </DrawingDecorator>''' + get_moving_target() + '''
                  <ServerQuitFromTimeUp timeLimitMs="300000" description="time_up"/>
                  <ServerQuitWhenAnyAgentFinishes/>
                </ServerHandlers>
              </ServerSection>

              <AgentSection mode="Survival">
                <Name>Ivy</Name>
                <AgentStart>
                    <Placement x="4.5" y="50" z="0.5" yaw="-70" pitch="20"/>
					<Inventory>
						<InventoryItem type="diamond_pickaxe" slot="0"/>
					</Inventory>
                </AgentStart>
                <AgentHandlers>
				  <ObservationFromFullStats/>
                  <ContinuousMovementCommands turnSpeedDegs="360"/>
                  <LuminanceProducer>
                    <Width>860</Width>
                    <Height>480</Height>
                  </LuminanceProducer>
                  <ChatCommands/>''' + get_end_criteria() + '''
                </AgentHandlers>
              </AgentSection>
            </Mission>'''

def get_end_criteria():
    if agent_host.receivedArgument("test"):
        return '''<AgentQuitFromTouchingBlockType>
                    <Block type="beacon" description="hit_pillar"/>
                    <Block type="glowstone" description="hit_edge"/>
                  </AgentQuitFromTouchingBlockType>'''
    else:
        return ""
        
def get_moving_target():
    # Change this following value to True if you want some more movement in the arena.
    # (You might want fewer pillars in this case.)
    ADD_MOVING_OBJECT = False
    if ADD_MOVING_OBJECT:
        return '''
            <MovingTargetDecorator>
                <ArenaBounds>
                   <min x="-50" y="40" z="-50"/>
                   <max x="50" y="60" z="50"/>
                </ArenaBounds>
                <StartPos x="-3" y="50" z="0"/>
                <Seed>random</Seed>
                <UpdateSpeed>3</UpdateSpeed>
                <PermeableBlocks type="air obsidian"/>
                <BlockType type="beacon"/>
            </MovingTargetDecorator>'''
    else:
        return ""

# Code for telling Malmo what to do:
my_mission = MalmoPython.MissionSpec(get_mission_xml(), True)
my_mission_record = malmoutils.get_default_recording_object(agent_host, "braitenberg_test")

# Attempt to start a mission:
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

# Loop until mission starts:
print("Waiting for the mission to start - be patient!")
world_state = agent_host.getWorldState()
while not world_state.has_mission_begun:
    sys.stdout.write(".")
    time.sleep(0.1)
    world_state = agent_host.getWorldState()
    for error in world_state.errors:
        print("Error:",error.text)

print()
print("Mission running!")
print("To take manual control of the vehicle, click on the Minecraft window and press 'Enter'.")
print("You can then play Minecraft normally.")
print("To return control to the vehicle, press 'Enter' again.")
print("Press Control-C to stop the simulation.")

agent_host.sendCommand("chat " + current_behaviour)
# Loop until mission ends.
# This is the main loop where we get values for the sensors and turn it into movement.
current_yaw = 0 # Direction we are facing.
current_x, current_z = 0, 0 # Current position.
while world_state.is_mission_running:
    # Get the current state of the world:
    world_state = agent_host.getWorldState()
    if world_state.number_of_observations_since_last_state > 0:
        # Parse the observation to find out our current yaw (used for the UI)
        msg = world_state.observations[-1].text
        ob = json.loads(msg)
        current_yaw = ob.get(u'Yaw', 0)
        current_x = ob.get(u'XPos', 0)
        current_z = ob.get(u'ZPos', 0)
    # Have any frames come in?
    if world_state.number_of_video_frames_since_last_state > 0:
        # Yes, so process it to get values from our sensors:
        frame = world_state.video_frames[-1]
        left_signal, right_signal = processFrame(frame)
        # Depending on how we wire the signals to the "wheels", Braitenberg proposed we could
        # get four different behaviours.
        if current_behaviour == "love":
            # Stronger the signal, the slower that wheel moves.
            # Vehicle should approach the light source and stop.
            left_wheel, right_wheel = 1.0 - left_signal, 1.0 - right_signal
        elif current_behaviour == "exploration":
            left_wheel, right_wheel = 1.0 - right_signal, 1.0 - left_signal
            # Stronger the signal, the slower the *other* wheel moves.
            # Vehicle should approach the light source and then veer away.
        elif current_behaviour == "fear":
            left_wheel, right_wheel = left_signal, right_signal
            # Stronger the signal, faster the wheel.
            # Vehicle favours dark areas, speeds up to get out of the light.
        elif current_behaviour == "aggression":
            left_wheel, right_wheel = right_signal, left_signal
            # Stronger the signal, faster the *other* wheel moves.
            # Vehicle steers aggressively towards the light.

        turn, move = calc_velocities(left_wheel, right_wheel)
        # Send the actual commands to Minecraft:
        agent_host.sendCommand("turn " + str(turn))
        agent_host.sendCommand("move " + str(move))
        # And draw our UI:
        draw_ui(left_signal=left_signal, right_signal=right_signal, yaw=current_yaw, speed=move, x=current_x, z=current_z)

    for error in world_state.errors:
        print("Error:",error.text)

print()
print("Mission ended!")

# Parse the MissionEnded XML messasge:
mission_end_tree = xml.etree.ElementTree.fromstring(world_state.mission_control_messages[-1].text)
ns_dict = {"malmo":"http://ProjectMalmo.microsoft.com"}
stat = mission_end_tree.find("malmo:Status", ns_dict).text
hr_stat = mission_end_tree.find("malmo:HumanReadableStatus", ns_dict).text
print("Mission over. Status: ", stat, end=' ')
if len(hr_stat):
    print(" - " + hr_stat)
if agent_host.receivedArgument("test") and hr_stat == "time_up":
    print("FAILED - The vehicle should have steered into a pillar or the illuminated edge by now.")
    exit(1)
