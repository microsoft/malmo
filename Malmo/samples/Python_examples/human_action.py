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

# Human Action Component - use this to let humans play through the same missions as you give to agents

import MalmoPython
import os
import sys
import time
from Tkinter import *
from PIL import Image
from PIL import ImageTk

prev_x = prev_y = 0

def onStartMission():
    print 'starting mission'
    my_mission_record_spec = MalmoPython.MissionRecordSpec()
    agent_host.startMission( my_mission, my_mission_record_spec )

    print "Waiting for the mission to start",
    world_state = agent_host.peekWorldState()
    while not world_state.is_mission_running:
        sys.stdout.write(".")
        time.sleep(0.1)
        world_state = agent_host.peekWorldState()
        for error in world_state.errors:
            print "Error:",error.text
    print

    while world_state.is_mission_running:
        if world_state.number_of_video_frames_since_last_state > 0:
            frame = world_state.video_frames[-1]
            buff = buffer(frame.pixels, 0, frame.width * frame.height * frame.channels)
            image = Image.frombytes('RGB', (frame.width,frame.height), buff)
            photo = ImageTk.PhotoImage(image)
            canvas.delete("all")
            canvas.create_image(frame.width/2, frame.height/2, image=photo)
            root.update()
        time.sleep(0.01)
        world_state = agent_host.getWorldState()
    print 'Mission has stopped.'
    
def onSendCommand():
    agent_host.sendCommand(command_entry.get())
    command_entry.delete(0,END)

def onMouseMoveInCanvas(event):
    canvas.focus_set()
    global prev_x,prev_y
    # TODO: still not quite right
    turn_speed = ( event.x - prev_x ) / 10.0
    agent_host.sendCommand( 'turn '+str(turn_speed) )
    pitch_speed = ( event.y - prev_y ) / 10.0
    agent_host.sendCommand( 'pitch '+str(pitch_speed) )
    prev_x = event.x
    prev_y = event.y
    # TODO: this doesn't feel right - want trackball not flight or something
    #agent_host.sendCommand( 'turn '+str( 2.0 * event.x / canvas.winfo_width() - 1 ) )
    #agent_host.sendCommand( 'pitch '+str( 2.0 * event.y / canvas.winfo_height() - 1 ) )
  
def onLeftMouseDownInCanvas(event):
    canvas.focus_set()
    agent_host.sendCommand( 'attack 1' )
  
def onLeftMouseUpInCanvas(event):
    canvas.focus_set()
    agent_host.sendCommand( 'attack 0' )
  
def onRightMouseDownInCanvas(event):
    canvas.focus_set()
    agent_host.sendCommand( 'use 1' )
  
def onRightMouseUpInCanvas(event):
    canvas.focus_set()
    agent_host.sendCommand( 'use 0' )
  
def onKeyInCommandEntry(event):
    if event.char == '\r':
        onSendCommand()
       
def onKeyPressInCanvas(event):
    print 'canvas key:',event.char
    if event.char == 'w':
        agent_host.sendCommand('move 1')
    elif event.char == 'a':
        agent_host.sendCommand('strafe -1')
    elif event.char == 's':
        agent_host.sendCommand('move -1')
    elif event.char == 'd':
        agent_host.sendCommand('strafe 1')
    elif event.char == ' ':
        agent_host.sendCommand('jump 1')

def onKeyReleaseInCanvas(event):
    print 'canvas key released:',event.char
    if event.char == 'w':
        agent_host.sendCommand('move 0')
    elif event.char == 'a':
        agent_host.sendCommand('strafe 0')
    elif event.char == 's':
        agent_host.sendCommand('move 0')
    elif event.char == 'd':
        agent_host.sendCommand('strafe 0')
    elif event.char == ' ':
        agent_host.sendCommand('jump 0')

root = Tk()
root.wm_title("Human Action Component")
start_button = Button(root, text='Start', command=onStartMission,font = "Helvetica 36 bold")
start_button.pack(padx=5, pady=5)
canvas = Canvas(root, width=320, height=200, borderwidth=0, highlightthickness=0, bg="white" )
canvas.focus_set()
canvas.bind('<Motion>',onMouseMoveInCanvas)
canvas.bind('<Button-1>',onLeftMouseDownInCanvas)
canvas.bind('<ButtonRelease-1>',onLeftMouseUpInCanvas)
canvas.bind('<Button-3>',onRightMouseDownInCanvas)
canvas.bind('<ButtonRelease-3>',onRightMouseUpInCanvas)
canvas.bind('<KeyPress>',onKeyPressInCanvas)
canvas.bind('<KeyRelease>',onKeyReleaseInCanvas)
#canvas.config(cursor='none')
canvas.pack(padx=5, pady=5)
Label(root, text="Command:",font = "Helvetica 16 bold").pack(padx=5, pady=5, side=LEFT)
command_entry = Entry(root,font = "Helvetica 16 bold")
command_entry.bind('<Key>',onKeyInCommandEntry)
command_entry.pack(padx=5, pady=5, side=LEFT)
Button(root, text='Send', command=onSendCommand,font = "Helvetica 16 bold").pack(padx=5, pady=5, side=LEFT)

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

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
    
my_mission = MalmoPython.MissionSpec()
my_mission.requestVideo(800,600)
my_mission.timeLimitInSeconds(20)
canvas.config( width=800, height=600 )

root.mainloop()
