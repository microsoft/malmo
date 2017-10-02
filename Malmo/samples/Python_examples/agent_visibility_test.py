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

# Test multi-agent visibility issues (see https://github.com/Microsoft/malmo/issues/443)
# Launches a multi-agent mission where each agent is positioned in a circle, facing the centre.
# If the number of agents is small enough (eg 3 or 4), each agent should be able to see every other agent.
# Some basic image processing is applied in order to test this.

from future import standard_library
standard_library.install_aliases()
from builtins import range
from past.utils import old_div
WIDTH=860
HEIGHT=480

import MalmoPython
import logging
import math
import os
import random
import sys
import time
import uuid
import errno

if sys.version_info[0] == 2:
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
else:
    import functools
    print = functools.partial(print, flush=True)

# Create somewhere to store any failed frames:
FAILED_FRAME_DIR = "VisibilityTest_FailedFrames"
try:
    os.makedirs(FAILED_FRAME_DIR)
except OSError as exception:
    if exception.errno != errno.EEXIST: # ignore error if already existed
        raise

# Create the first agent host - needed to process command-line options:
agent_hosts = [MalmoPython.AgentHost()]

# Parse the command-line options:
agent_hosts[0].addOptionalFlag("debug,d", "Display mission start debug information.")
agent_hosts[0].addOptionalFlag("gui,g", "Display image processing steps in a gui window.")
agent_hosts[0].addOptionalIntArgument("agents,n", "Number of agents to use.", 4)
try:
    agent_hosts[0].parse( sys.argv )
except RuntimeError as e:
    print('ERROR:',e)
    print(agent_hosts[0].getUsage())
    exit(1)
if agent_hosts[0].receivedArgument("help"):
    print(agent_hosts[0].getUsage())
    exit(0)

DEBUG = agent_hosts[0].receivedArgument("debug")
SHOW_GUI = agent_hosts[0].receivedArgument("gui")
INTEGRATION_TEST_MODE = agent_hosts[0].receivedArgument("test")
agents_requested = agent_hosts[0].getIntArgument("agents")
NUM_AGENTS = max(2,min(agents_requested,4))
if NUM_AGENTS != agents_requested:
    print("WARNING: using", NUM_AGENTS, "agents, rather than", agents_requested)

# Create the rest of the agents:
agent_hosts += [MalmoPython.AgentHost() for x in range(1, NUM_AGENTS) ]
# Set debug flag:
for ah in agent_hosts:
    ah.setDebugOutput(DEBUG)    # Turn client-pool connection messages on/off.

failed_frame_count = 0

# Dependencies for gui:
if SHOW_GUI:
    if sys.version_info[0] == 2:
        # Workaround for https://github.com/PythonCharmers/python-future/issues/262
        from Tkinter import *
    else:
        from tkinter import *
    from PIL import Image
    from PIL import ImageTk

    root = Tk()
    root.wm_title("Visibility Test")
    root_frame = Frame(root)
    Label(root_frame, text="Original image; greyscale image; thresholded image").pack(padx=5, pady=5)
    canvas = Canvas(root_frame, borderwidth=0, highlightthickness=0, width=640, height=480, bg="#dd88aa")
    canvas.config( width=WIDTH, height=NUM_AGENTS * 4 * (5 + HEIGHT * 0.05) )
    canvas.pack(padx=5, pady=5)
    root_frame.pack()
else:
    from PIL import Image

# Used by the gui:
bmp_original = 0
bmp_luminance = 1
bmp_thresholded = 2
bitmaps = [[(-1, None) for bmp_type in [bmp_original, bmp_luminance, bmp_thresholded]] for x in range(NUM_AGENTS)]

def safeStartMission(agent_host, my_mission, my_client_pool, my_mission_record, role, expId):
    used_attempts = 0
    max_attempts = 5
    print("Calling startMission for role", role)
    while True:
        try:
            # Attempt start:
            agent_host.startMission(my_mission, my_client_pool, my_mission_record, role, expId)
            break
        except MalmoPython.MissionException as e:
            errorCode = e.details.errorCode
            if errorCode == MalmoPython.MissionErrorCode.MISSION_SERVER_WARMING_UP:
                print("Server not quite ready yet - waiting...")
                waitWhileUpdatingGui(2)
            elif errorCode == MalmoPython.MissionErrorCode.MISSION_INSUFFICIENT_CLIENTS_AVAILABLE:
                print("Not enough available Minecraft instances running.")
                used_attempts += 1
                if used_attempts < max_attempts:
                    print("Will wait in case they are starting up.", max_attempts - used_attempts, "attempts left.")
                    waitWhileUpdatingGui(2)
            elif errorCode == MalmoPython.MissionErrorCode.MISSION_SERVER_NOT_FOUND:
                print("Server not found - has the mission with role 0 been started yet?")
                used_attempts += 1
                if used_attempts < max_attempts:
                    print("Will wait and retry.", max_attempts - used_attempts, "attempts left.")
                    waitWhileUpdatingGui(2)
            else:
                print("Other error:", e.message)
                print("Waiting will not help here - bailing immediately.")
                exit(1)
        if used_attempts == max_attempts:
            print("All chances used up - bailing now.")
            exit(1)
    print("startMission called okay.")

def waitWhileUpdatingGui(pause):
    while pause > 0:
        time.sleep(0.1)
        pause -= 0.1
        if SHOW_GUI:
            root.update()

def safeWaitForStart(agent_hosts):
    print("Waiting for the mission to start", end=' ')
    start_flags = [False for a in agent_hosts]
    start_time = time.time()
    time_out = 120  # Allow two minutes for mission to begin.
    while not all(start_flags) and time.time() - start_time < time_out:
        states = [a.peekWorldState() for a in agent_hosts]
        start_flags = [w.has_mission_begun for w in states]
        errors = [e for w in states for e in w.errors]
        if len(errors) > 0:
            print("Errors waiting for mission start:")
            for e in errors:
                print(e.text)
            print("Bailing now.")
            exit(1)
        time.sleep(0.1)
        if SHOW_GUI:
            root.update()
        print(".", end=' ')
    if time.time() - start_time >= time_out:
        print("Timed out while waiting for mission to begin running - bailing.")
        exit(1)
    print()
    print("Mission has started.")

def getPlacementString(i):
    # Place agents at equal points around a circle, facing inwards.
    radius = 5
    accuracy = 1000.
    angle = 2*i*math.pi/NUM_AGENTS
    x = old_div(int(accuracy * radius * math.cos(angle)), accuracy)
    z = old_div(int(accuracy * radius * math.sin(angle)), accuracy)
    yaw = 90 + angle*180.0/math.pi
    return 'x="' + str(x) + '" y="227" z="' + str(z) + '" pitch="0" yaw="' + str(yaw) + '"'

def processFrame(width, height, pixels, agent, mission_count):
    # Attempt some fairly simple image processing in order to determine how many agents the current agent can "see".
    # We rely on the fact that the other agents jut above the horizon line, which is at the mid-point of the image.
    # With the time set to morning, and the weather set to clear, there should always be a good distinction between
    # background (sky) pixels, and foreground (agent) pixels. A bit of thresholding should suffice to provide a
    # fairly reliable way of counting the visible agents.
    global root, bitmaps, failed_frame_count
    channels = 3    # Following code assumes this to be true.
    
    # 1. Extract a narrow strip of the middle from the middle up:
    y1 = int(height * 0.45)
    y2 = int(height * 0.5)
    if y2 == y1:
        y1 -= 1
    num_rows = y2 - y1
    middle_strip = pixels[y1*width*channels:y2*width*channels]

    if SHOW_GUI:
        image_original = Image.frombytes('RGB', (width, num_rows), str(middle_strip))
        photo_original = ImageTk.PhotoImage(image_original)
        if bitmaps[agent][bmp_original][1] != None:
            canvas.delete(bitmaps[agent][bmp_original][0])
        handle = canvas.create_image(old_div(width,2), ((4*agent)+0.5)*(num_rows+5), image=photo_original)
        bitmaps[agent][bmp_original] = (handle, photo_original)

    # 2. Convert RGB to luminance. Build up a histogram as we go - this will be useful for finding a threshold point.
    hist = [0 for x in range(256)]
    for col in range(0, width*channels, channels):
        for row in range(0, num_rows):
            pix = col + row * width * channels
            lum = int(0.2126 * middle_strip[pix] + 0.7152 * middle_strip[pix + 1] + 0.0722 * middle_strip[pix + 2])
            hist[lum] += 1
            middle_strip[pix] = middle_strip[pix+1] = middle_strip[pix+2] = lum # assuming channels == 3

    if SHOW_GUI:
        image_greyscale = Image.frombytes('RGB', (width, num_rows), str(middle_strip))
        photo_greyscale = ImageTk.PhotoImage(image_greyscale)
        if bitmaps[agent][bmp_luminance][1] != None:
            canvas.delete(bitmaps[agent][bmp_luminance][0])
        handle = canvas.create_image(old_div(width,2), ((4*agent+1)+0.5)*(num_rows+5), image=photo_greyscale)
        bitmaps[agent][bmp_luminance] = (handle, photo_greyscale)

    # 3. Calculate a suitable threshold, using the Otsu method
    total_pixels = width * num_rows
    total_sum = 0.
    for t in range(256):
        total_sum += t * hist[t]
    sum_background = 0.
    weight_background = 0.
    weight_foreground = 0.
    max_variation = 0.
    threshold = 0
    for t in range(256):
        weight_background += hist[t]
        if weight_background == 0:
            continue
        weight_foreground = total_pixels - weight_background
        if weight_foreground == 0:
            break
        sum_background += t * hist[t]
        mean_background = old_div(sum_background, weight_background)
        mean_foreground = old_div((total_sum - sum_background), weight_foreground)
        # Between class variance:
        var = weight_background * weight_foreground * (mean_background - mean_foreground) * (mean_background - mean_foreground)
        if var > max_variation:
            max_variation = var
            threshold = t

    # 4. Apply this threshold
    for pix in range(len(middle_strip)):
        if middle_strip[pix] <= threshold:
            middle_strip[pix] = 255
        else:
            middle_strip[pix] = 0

    # 5. OR together all the rows. This helps to de-noise the image.
    # At the same time, we count the number of changes (from foreground to background, or background to foreground)
    # that occur across the scanline. Assuming that there are no partial agents at the sides of the view - ie the scanline
    # starts and ends with background - this count should result in two changes per visible agent.
    pixelvalue = lambda col: sum(middle_strip[x] for x in range(col, len(middle_strip), width * channels))
    lastval = 255
    changes = 0
    for col in range(0, width * channels, channels):
        val = 0 if pixelvalue(col) > 0 else 255
        if lastval != val:
            changes += 1
        lastval = val
        if SHOW_GUI:
            # Update the bitmap so the user can see what we see.
            for row in range(num_rows):
                middle_strip[col + row*width*channels] = val
                middle_strip[1 + col + row*width*channels] = val
                middle_strip[2 + col + row*width*channels] = 0  # blue channel always 0 (will simplify recolouring later)

    # 6. Perform the actual test.
    agents_detected = old_div(changes, 2)
    test_passed = agents_detected == NUM_AGENTS - 1

    # 7. If we're displaying the gui, recolour the final image - turn the background red for error or green for success.
    # (At the moment all background pixels have both red and green values set to 255, so all we need to do is remove
    # the relevant channel.)
    if SHOW_GUI:
        channel_mask = 0 if test_passed else 1  # Remove red channel for success, remove green for failure
        for pixel in range(channel_mask, len(middle_strip), channels):
            middle_strip[pixel] = 0
        # And add this to the GUI:
        image_threshold = Image.frombytes('RGB', (width, num_rows), str(middle_strip))
        photo_threshold = ImageTk.PhotoImage(image_threshold)
        if bitmaps[agent][bmp_thresholded][1] != None:
            canvas.delete(bitmaps[agent][bmp_thresholded][0])
        handle = canvas.create_image(old_div(width,2), ((4*agent+2)+0.5)*(num_rows+5), image=photo_threshold)
        bitmaps[agent][bmp_thresholded] = (handle, photo_threshold)
        # Update the canvas:
        root.update()
    
    if not test_passed:
        # The threshold is not entirely bullet-proof - sometimes there are drawing artifacts that can result in
        # false negatives.
        # So we save a copy of the failing frames for manual inspection:
        image_failed = Image.frombytes('RGB', (width, height), str(pixels))
        image_failed.save(FAILED_FRAME_DIR + "/failed_frame_agent_" + str(agent) + "_mission_" + str(mission_count) + "_" + str(failed_frame_count) + ".png")
        failed_frame_count += 1
    return test_passed

def createMissionXML(num_agents, width, height, reset):
    # Set up the Mission XML.
    # First, the server section.
    # Weather MUST be set to clear, since the dark thundery sky plays havoc with the image thresholding.
    xml = '''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <About>
        <Summary/>
      </About>
      <ServerSection>
        <ServerInitialConditions>
          <Time>
            <StartTime>1000</StartTime>
          </Time>
          <Weather>clear</Weather>
        </ServerInitialConditions>
        <ServerHandlers>
          <FlatWorldGenerator forceReset="'''+reset+'''" generatorString="3;2*4,225*22;1;" seed=""/>
          <ServerQuitFromTimeUp description="" timeLimitMs="10000"/>
        </ServerHandlers>
      </ServerSection>
    '''

    # Add an agent section for each watcher.
    # We put them in a leather helmet because it makes the image processing slightly easier.
    for i in range(num_agents):
        placement = getPlacementString(i)
        xml += '''<AgentSection mode="Survival">
        <Name>Watcher#''' + str(i) + '''</Name>
        <AgentStart>
          <Placement ''' + placement + '''/>
          <Inventory>
            <InventoryObject type="leather_helmet" slot="39" quantity="1"/>
          </Inventory>
        </AgentStart>
        <AgentHandlers>
          <VideoProducer>
            <Width>''' + str(width) + '''</Width>
            <Height>''' + str(height) + '''</Height>
          </VideoProducer>
        </AgentHandlers>
      </AgentSection>'''

    xml += '</Mission>'
    return xml

# Set up a client pool.
# IMPORTANT: If ANY of the clients will be on a different machine, then you MUST
# make sure that any client which can be the server has an IP address that is
# reachable from other machines - ie DO NOT SIMPLY USE 127.0.0.1!!!!
# The IP address used in the client pool will be broadcast to other agents who
# are attempting to find the server - so this will fail for any agents on a
# different machine.
client_pool = MalmoPython.ClientPool()
for x in range(10000, 10000 + NUM_AGENTS):
    client_pool.add( MalmoPython.ClientInfo('127.0.0.1', x) )

failed_frames = [0 for x in range(NUM_AGENTS)] # keep a count of the frames that failed for each agent.

# If we're running as part of the integration tests, just do ten iterations. Otherwise keep going.
missions_to_run = 10 if INTEGRATION_TEST_MODE else 30000

for mission_no in range(1,missions_to_run+1):
    # Create the mission. Force reset for the first mission, to ensure a clean world. No need for subsequent missions.
    my_mission = MalmoPython.MissionSpec(createMissionXML(NUM_AGENTS, WIDTH, HEIGHT, "true" if mission_no == 1 else "false"), True)
    print("Running mission #" + str(mission_no))
    # Generate an experiment ID for this mission.
    # This is used to make sure the right clients join the right servers -
    # if the experiment IDs don't match, the startMission request will be rejected.
    # In practice, if the client pool is only being used by one researcher, there
    # should be little danger of clients joining the wrong experiments, so a static
    # ID would probably suffice, though changing the ID on each mission also catches
    # potential problems with clients and servers getting out of step.

    # Note that, in this sample, the same process is responsible for all calls to startMission,
    # so passing the experiment ID like this is a simple matter. If the agentHosts are distributed
    # across different threads, processes, or machines, a different approach will be required.
    # (Eg generate the IDs procedurally, in a way that is guaranteed to produce the same results
    # for each agentHost independently.)
    experimentID = str(uuid.uuid4())

    for i in range(len(agent_hosts)):
        safeStartMission(agent_hosts[i], my_mission, client_pool, MalmoPython.MissionRecordSpec(), i, experimentID)

    safeWaitForStart(agent_hosts)
    time.sleep(2)	# Wait a short while for things to stabilise

    running = True
    timed_out = False
    # Main mission loop.
    # In this test, all we do is stand still and process our frames.
    while not timed_out:
        for i in range(NUM_AGENTS):
            ah = agent_hosts[i]
            world_state = ah.getWorldState()
            if world_state.is_mission_running == False:
                timed_out = True
            if world_state.is_mission_running and world_state.number_of_video_frames_since_last_state > 0:
                frame = world_state.video_frames[-1]
                can_see_agent = processFrame(frame.width, frame.height, frame.pixels, i, mission_no)
                if not can_see_agent:
                    failed_frames[i] += 1
    print()

    if SHOW_GUI:
        canvas.delete("all")    # Clear the gui window in between each mission.

    print("Waiting for mission to end ", end=' ')
    hasEnded = False
    while not hasEnded:
        print(".", end="")
        time.sleep(0.1)
        for ah in agent_hosts:
            world_state = ah.getWorldState()
            if not world_state.is_mission_running:
                hasEnded = True
    print()
    print("Failed frames: ", failed_frames)
    if INTEGRATION_TEST_MODE and sum(failed_frames):
        exit(1) # Test failed - quit.
    time.sleep(2)
