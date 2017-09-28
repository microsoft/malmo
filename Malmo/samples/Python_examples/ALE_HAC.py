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

from future import standard_library
standard_library.install_aliases()
from past.utils import old_div
import MalmoPython
import os
import random
import sys
import time
import errno
if sys.version_info[0] == 2:
    # Workaround for https://github.com/PythonCharmers/python-future/issues/262
    import Tkinter as tk
else:
    import tkinter as tk
from PIL import Image, ImageTk
from array import array
from struct import pack

gameNum = 0
iterations = 5

root = tk.Tk()
root.wm_title("Video Output")
canvas = tk.Canvas(root, width=160, height=210, borderwidth=0, highlightthickness=0, bg="white")
canvas.pack()                

def callback(event):
    sendCommand() # kick things off

canvas.bind("<Button-1>", callback)

os.system('xset r off') # Nasty, but we need to turn off keyboard auto-repeat to track key states.

# Track which keys are pressed
left = 0
right = 0
up = 0
down = 0
fire = 0

def keyUp(event):
    global left, right, up, down, fire
    if event.keysym == 'Escape':
        root.destroy()
    if event.keysym == 'Right':
        right = 0
    if event.keysym == 'Left':
        left = 0
    if event.keysym == 'Up':
        up = 0
    if event.keysym == 'Down':
        down = 0
    if event.keysym == 'space':
        fire = 0

def keyDown(event):
    global left, right, up, down, fire
    if event.keysym == 'Right':
        right = 1
        left = 0    # left and right are mutually exclusive
    if event.keysym == 'Left':
        left = 1
        right = 0
    if event.keysym == 'Up':
        up = 1
        down = 0    # up and down are mutally exclusive
    if event.keysym == 'Down':
        down = 1
        up = 0
    if event.keysym == 'space':
        fire = 1



# ALE op-codes:
#           PLAYER_A_NOOP           = 0,
#           PLAYER_A_FIRE           = 1,
#           PLAYER_A_UP             = 2,
#           PLAYER_A_RIGHT          = 3,
#           PLAYER_A_LEFT           = 4,
#           PLAYER_A_DOWN           = 5,
#           PLAYER_A_UPRIGHT        = 6,
#           PLAYER_A_UPLEFT         = 7,
#           PLAYER_A_DOWNRIGHT      = 8,
#           PLAYER_A_DOWNLEFT       = 9,
#           PLAYER_A_UPFIRE         = 10,
#           PLAYER_A_RIGHTFIRE      = 11,
#           PLAYER_A_LEFTFIRE       = 12,
#           PLAYER_A_DOWNFIRE       = 13,
#           PLAYER_A_UPRIGHTFIRE    = 14,
#           PLAYER_A_UPLEFTFIRE     = 15,
#           PLAYER_A_DOWNRIGHTFIRE  = 16,
#           PLAYER_A_DOWNLEFTFIRE   = 17

allops=[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17]    # all allowed op-codes
leftops=[4,7,9,12,15,17]    # op-codes with left pressed
rightops=[3,6,8,11,14,16]   # op-codes with right pressed
upops=[2,6,7,10,14,15]      # op-codes with up pressed
downops=[5,8,9,13,16,17]    # op-codes with down pressed
fireops=[1,10,11,12,13,14,15,16,17] # op-codes with fire pressed

def startGame():
    #Find filename for the recording:
    filenum = 0
    fileRecording = ''
    while fileRecording == '':
        fileRecording = recordingsDirectory+'/saved_data'+str(filenum)+'.tar.gz'
        if os.path.isfile(fileRecording):
            filenum = filenum + 1
            fileRecording = ''

    my_mission_record = MalmoPython.MissionRecordSpec(fileRecording)
    my_mission_record.recordCommands()
    my_mission_record.recordMP4(20, 400000)
    my_mission_record.recordRewards()
    my_mission_record.recordObservations()

    try:
        display_gui = 1
        if want_own_display:
            display_gui = 0
        agent_host.startMission( my_mission, MalmoPython.ClientPool(), my_mission_record, display_gui, rom_file )
    except RuntimeError as e:
        print("Error starting mission:",e)
        exit(1)

    print("Waiting for the mission to start", end=' ')
    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        print(".", end="")
        time.sleep(0.1)
        world_state = agent_host.getWorldState()
        for error in world_state.errors:
            print("Error:",error.text)
    print()

    gamestats = "Go " + str(gameNum+1) + " out of " + str(iterations) + "\n"
    canvas.delete("all")
    canvas.create_text(80, 105, text=gamestats + "Click to begin!\nEscape to end")  # The window needs keyboard focus or no way to control game.


def sendCommand():
    global gameNum

    ops = set(allops)
    # Narrow down the op-codes by the keys pressed:
    if left:
        ops = ops & set(leftops)
    else:
        ops = ops - set(leftops)

    if right:
        ops = ops & set(rightops)
    else:
        ops = ops - set(rightops)

    if up:
        ops = ops & set(upops)
    else:
        ops = ops - set(upops)

    if down:
        ops = ops & set(downops)
    else:
        ops = ops - set(downops)

    if fire:
        ops = ops & set(fireops)
    else:
        ops = ops - set(fireops)

    if len(ops) > 0:
        agent_host.sendCommand( str(list(ops)[0]) ) # If no keys pressed will send no-op
        # The ALE only updates in response to a command, so get the new world state now.
        world_state = agent_host.getWorldState()
        for reward in world_state.rewards:
            if reward.getValue() > 0:
                print("Summed reward:",reward.getValue())
        for error in world_state.errors:
            print("Error:",error.text)
        if world_state.number_of_video_frames_since_last_state > 0 and want_own_display:
            # Turn the frame into an image to display on our canvas.
            frame = world_state.video_frames[-1]
            buff = buffer(frame.pixels, 0, frame.width * frame.height * frame.channels)
            image = Image.frombytes('RGB', (frame.width,frame.height), buff)
            photo = ImageTk.PhotoImage(image)
            canvas.delete("all")
            canvas.create_image(old_div(frame.width,2), old_div(frame.height,2), image=photo)
            root.update()

    if world_state.is_mission_running:
        canvas.after(0, sendCommand)    # Call sendCommand again as soon as possible within tkinter's event loop.
    else:
        gameNum = gameNum + 1
        if gameNum < iterations:
            startGame()
        else:
            root.destroy() # We are done.

root.bind_all('<KeyPress>', keyDown)
root.bind_all('<KeyRelease>', keyUp)

if sys.version_info[0] == 2:
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
else:
    import functools
    print = functools.partial(print, flush=True)

agent_host = MalmoPython.ALEAgentHost()
# add some arguments:
agent_host.addOptionalStringArgument('rom_file', 'Path/to/ROM from which to load the game.', '../ALE_ROMS/montezuma_revenge')
agent_host.addOptionalFlag('own_display', 'Display frames direct from Malmo')
agent_host.addOptionalIntArgument('goes', 'Number of goes at the game.', 2)

try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print('ERROR:',e)
    print(agent_host.getUsage())
    exit(1)
if agent_host.receivedArgument("help"):
    print(agent_host.getUsage())
    exit(0)

rom_file = agent_host.getStringArgument('rom_file')
want_own_display = agent_host.receivedArgument('own_display')
iterations = agent_host.getIntArgument('goes')

my_mission = MalmoPython.MissionSpec()
my_mission.requestVideo( 160, 210 )

recordingsDirectory = rom_file.rpartition('/')[-1]+'_recordings'
try:
    os.makedirs(recordingsDirectory)
except OSError as exception:
    if exception.errno != errno.EEXIST: #ignore error if already existed
        raise

startGame() # Get things up and ready...
root.mainloop() # and enter the event loop
print("Mission has stopped.")
os.system('xset r on')  # set auto-repeat back

