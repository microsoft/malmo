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

# Not-particularly-useful demonstration of some of the new VideoProducers introduced in Malmo 0.31.0
# Uses the 32bpp depth-map and the colour-map.
# Shows how to unpack values from the depth-map, and
# how to determine frame type when more than one video producer is used.
# The visualisation is constructed purely from the video streams - no other observations are required.

from future import standard_library
standard_library.install_aliases()
from builtins import bytes
from builtins import range
from builtins import object
from past.utils import old_div
import MalmoPython
import random
import time
import logging
import struct
import socket
import os
import sys
import errno
import json
import math
import malmoutils

malmoutils.fix_print()

agent_host = MalmoPython.AgentHost()
malmoutils.parse_command_line(agent_host)

if sys.version_info[0] == 2:
    # Workaround for https://github.com/PythonCharmers/python-future/issues/262
    from Tkinter import *
else:
    from tkinter import *

from PIL import ImageTk
from PIL import Image

video_width = 432
video_height = 240

WIDTH = 432
HEIGHT = 432 + video_height

root = Tk()
root.wm_title("Depth and ColourMap Example")
root_frame = Frame(root)
canvas = Canvas(root_frame, borderwidth=0, highlightthickness=0, width=WIDTH, height=HEIGHT, bg="black")
canvas.config( width=WIDTH, height=HEIGHT )
canvas.pack(padx=5, pady=5)
root_frame.pack()

class draw_helper(object):
    def __init__(self, canvas):
        self._canvas = canvas
        self.reset()
        self._line_fade = 9
        self._blip_fade = 100

    def reset(self):
        self._canvas.delete("all")
        self._dots = []
        self._segments = []
        self._panorama_image = Image.new('RGB', (WIDTH, video_height))
        self._panorama_photo = None
        self._image_handle = None
        self._current_frame = 0
        self._last_angle = 0

    def processFrame(self, frame):
        if frame.frametype == MalmoPython.FrameType.DEPTH_MAP:
            # Use the depth map to create a "radar" - take just the centre point of the depth image,
            # and use it to add a "blip" to the radar screen.

            # Set up some drawing params:
            size = min(WIDTH, HEIGHT)
            scale = old_div(size, 20.0)
            angle = frame.yaw * math.pi / 180.0
            cx = old_div(size, 2)
            cy = cx

            # Draw the sweeping line:
            points = [cx, cy, cx + 10 * scale * math.cos(angle), cy + 10 * scale * math.sin(angle), cx + 10 * scale * math.cos(self._last_angle), cy + 10 * scale * math.sin(self._last_angle)]
            self._last_angle = angle
            self._segments.append(self._canvas.create_polygon(points, width=0, fill="#004410"))

            # Get the depth value from the centre of the map:
            mid_pix = 2 * video_width * (video_height + 1)  # flattened index of middle pixel
            depth = scale * struct.unpack('f', bytes(frame.pixels[mid_pix:mid_pix + 4]))[0]   # unpack 32bit float

            # Draw the "blip":
            x = cx + depth * math.cos(angle)
            y = cy + depth * math.sin(angle)
            self._dots.append((self._canvas.create_oval(x - 3, y - 3, x + 3, y + 3, width=0, fill="#ffa930"), self._current_frame))

            # Fade the lines and the blips:
            for i, seg in enumerate(self._segments):
                fillstr = "#{0:02x}{1:02x}{2:02x}".format(0, int((self._line_fade - len(self._segments) + i) * (old_div(255.0, float(self._line_fade)))), 0)
                self._canvas.itemconfig(seg, fill=fillstr)
            if len(self._segments) >= self._line_fade:
                self._canvas.delete(self._segments.pop(0))

            for i, dot in enumerate(self._dots):
                brightness = self._blip_fade - (self._current_frame - dot[1])
                if brightness < 0:
                    self._canvas.delete(dot[0])
                else:
                    fillstr = "#{0:02x}{1:02x}{2:02x}".format(100, int(brightness * (old_div(255.0, float(self._blip_fade)))), 80)
                    self._canvas.itemconfig(dot[0], fill=fillstr)
                self._dots = [dot for dot in self._dots if self._current_frame - dot[1] <= self._blip_fade]
            self._current_frame += 1
        elif frame.frametype == MalmoPython.FrameType.COLOUR_MAP:
            # Use the centre slice of the colourmap to create a panaramic image
            # First create image from this frame:
            cmap = Image.frombytes('RGB', (video_width, video_height), bytes(frame.pixels))
            # Now crop just the centre slice:
            left = (old_div(video_width, 2)) - 4
            cmap = cmap.crop((left, 0, left + 8, video_height))
            cmap.load()
            # Where does this slice belong in the panorama?
            x = int((int(frame.yaw) % 360) * WIDTH / 360.0)
            # Paste it in:
            self._panorama_image.paste(cmap, (x, 0, x + 8, video_height))
            # Convert to a photo for canvas use:
            self._panorama_photo = ImageTk.PhotoImage(self._panorama_image)
            # And update/create the canvas image:
            if self._image_handle is None:
                self._image_handle = canvas.create_image(old_div(WIDTH, 2), HEIGHT - (old_div(video_height, 2)), image=self._panorama_photo)
            else:
                canvas.itemconfig(self._image_handle, image=self._panorama_photo)

def draw_environment():
    xml = '<DrawCuboid x1="-12" y1="227" z1="-12" x2="12" y2="237" z2="12" type="air"/>'
    for i in range(20):
        x, z = (0, 0)
        while (x, z) == (0, 0):
            x = random.randint(-6, 6)
            z = random.randint(-6, 6)
        block_type = random.choice(["obsidian", "gold_block", "bookshelf", "log", "lapis_block", "stone"])
        cuboid = '<DrawCuboid x1="{}" y1="227" z1="{}" x2="{}" y2="{}" z2="{}" type="{}"/>'.format(x, z, x, 227 + random.randint(1,4), z, block_type)
        xml += cuboid
    return xml

def get_mission_xml():
    return '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    
      <About>
        <Summary>Run the maze!</Summary>
      </About>
      
     <ServerSection>
        <ServerHandlers>
            <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
            <DrawingDecorator>''' + draw_environment() + '''</DrawingDecorator>
            <ServerQuitFromTimeUp timeLimitMs="30000"/>
            <ServerQuitWhenAnyAgentFinishes />
        </ServerHandlers>
    </ServerSection>

    <AgentSection>
        <Name>Spinner</Name>
        <AgentStart>
            <Placement x="0.5" y="227.0" z="0.5"/>
        </AgentStart>
        <AgentHandlers>
            <DepthProducer>
                <Width>''' + str(video_width) + '''</Width>
                <Height>''' + str(video_height) + '''</Height>
            </DepthProducer>
            <ColourMapProducer>
                <Width>''' + str(video_width) + '''</Width>
                <Height>''' + str(video_height) + '''</Height>
            </ColourMapProducer>
            <ContinuousMovementCommands turnSpeedDegs="180" />
        </AgentHandlers>
    </AgentSection>
  </Mission>'''

agent_host.setVideoPolicy(MalmoPython.VideoPolicy.LATEST_FRAME_ONLY)

if agent_host.receivedArgument("test"):
    num_reps = 2
else:
    num_reps = 30000

drawer = draw_helper(canvas)

for iRepeat in range(num_reps):
    my_mission = MalmoPython.MissionSpec( get_mission_xml(), True )
    my_mission_record = malmoutils.get_default_recording_object(agent_host, "colourmap_test_{}".format(iRepeat + 1))
    max_retries = 3
    for retry in range(max_retries):
        try:
            agent_host.startMission( my_mission, my_mission_record )
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print("Error starting mission: %s" % e)
                exit(1)
            else:
                time.sleep(2)

    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        print(".", end="")
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
    print()

    agent_host.sendCommand( "turn 1" )

    # main loop:
    current_yaw_delta = 0
    while world_state.is_mission_running:
        world_state = agent_host.getWorldState()
        if world_state.number_of_video_frames_since_last_state > 0:
            drawer.processFrame(world_state.video_frames[-1])
            root.update()

    time.sleep(1) # let the Mod recover
    drawer.reset()
