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
import tkMessageBox
from PIL import Image
from PIL import ImageTk

last_mouse_event = None
is_mission_running = False

def onStartMission():

    my_mission_record_spec = MalmoPython.MissionRecordSpec()
    try:
        agent_host.startMission( my_mission, my_mission_record_spec )
    except RuntimeError as e:
        tkMessageBox.showerror("Error","Error starting mission: "+str(e))
        return

    global is_mission_running
    global start_button

    print "Waiting for the mission to start",
    world_state = agent_host.peekWorldState()
    while not world_state.is_mission_running:
        sys.stdout.write(".")
        time.sleep(0.1)
        world_state = agent_host.peekWorldState()
        for error in world_state.errors:
            print "Error:",error.text
    print
    canvas.config(cursor='none') # hide the mouse cursor while over the canvas
    canvas.event_generate('<Motion>', warp=True, x=canvas.winfo_width()/2, y=canvas.winfo_height()/2) # put cursor at center
    is_mission_running = True
    start_button.config(state='disabled')
    start_button.config(text='Running...')
    canvas.focus_set()

    while world_state.is_mission_running:
        if world_state.number_of_observations_since_last_state > 0:
            global observation
            observation.config(text = world_state.observations[0].text )
        if world_state.number_of_rewards_since_last_state > 0:
            global reward
            rewards.config(text = str(world_state.rewards[0].getValue()) )
        if world_state.number_of_video_frames_since_last_state > 0:
            frame = world_state.video_frames[-1]
            buff = buffer(frame.pixels, 0, frame.width * frame.height * frame.channels)
            image = Image.frombytes('RGB', (frame.width,frame.height), buff)
            photo = ImageTk.PhotoImage(image)
            canvas.delete("all")
            canvas.create_image(frame.width/2, frame.height/2, image=photo)
            canvas.create_line( frame.width/2 - 5, frame.height/2, frame.width/2 + 6, frame.height/2, fill='white' )
            canvas.create_line( frame.width/2, frame.height/2 - 5, frame.width/2, frame.height/2 + 6, fill='white' )
            root.update()
        time.sleep(0.01)
        world_state = agent_host.getWorldState()
    is_mission_running = False
    canvas.config(cursor='arrow') # restore the mouse cursor
    start_button.config(state='normal')
    start_button.config(text='Start')
    print 'Mission stopped'
    
def onSendCommand():
    agent_host.sendCommand(command_entry.get())
    command_entry.delete(0,END)

def update():
    if is_mission_running:
        global last_mouse_event
        if last_mouse_event:
            rotation_speed = 0.1
            turn_speed = ( last_mouse_event.x - canvas.winfo_width()/2 ) * rotation_speed
            pitch_speed = ( last_mouse_event.y - canvas.winfo_height()/2 ) * rotation_speed
            agent_host.sendCommand( 'turn '+str(turn_speed) )
            agent_host.sendCommand( 'pitch '+str(pitch_speed) )
        canvas.event_generate('<Motion>', warp=True, x=canvas.winfo_width()/2, y=canvas.winfo_height()/2) # put cursor at center
        last_mouse_event.x = canvas.winfo_width()/2
        last_mouse_event.y = canvas.winfo_height()/2
    root.after(50, update)

def onMouseMoveInCanvas(event):
    global last_mouse_event
    last_mouse_event = event
  
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
        canvas.focus_set() # move focus back to the canvas to continue moving
       
def onKeyPressInCanvas(event):
    commands_map = { 'w':'move 1', 'a':'strafe -1', 's':'move -1', 'd':'strafe 1', ' ':'jump 1' }
    if event.char == '/':
        command_entry.focus_set() # interlude to allow user to type command
    elif event.char in commands_map:
        agent_host.sendCommand( commands_map[ event.char ] )

def onKeyReleaseInCanvas(event):
    commands_map = { 'w':'move 0', 'a':'strafe 0', 's':'move 0', 'd':'strafe 0', ' ':'jump 0' }
    if event.char in commands_map:
        agent_host.sendCommand( commands_map[ event.char ] )

our_font = "Helvetica 16 bold"
small_font = "Helvetica 9 bold"
root = Tk()
root.wm_title("Human Action Component")
start_button = Button(root, text='Start', command=onStartMission,font = our_font)
start_button.pack(padx=5, pady=5)
canvas = Canvas(root, borderwidth=0, highlightthickness=0, bg="white" )
canvas.bind('<Motion>',onMouseMoveInCanvas)
canvas.bind('<Button-1>',onLeftMouseDownInCanvas)
canvas.bind('<ButtonRelease-1>',onLeftMouseUpInCanvas)
canvas.bind('<Button-3>',onRightMouseDownInCanvas)
canvas.bind('<ButtonRelease-3>',onRightMouseUpInCanvas)
canvas.bind('<KeyPress>',onKeyPressInCanvas)
canvas.bind('<KeyRelease>',onKeyReleaseInCanvas)
canvas.pack(padx=5, pady=5)
entry_frame = Frame(root)
Label(entry_frame, text="Command:",font = our_font).pack(padx=5, pady=5, side=LEFT)
command_entry = Entry(entry_frame,font = our_font)
command_entry.bind('<Key>',onKeyInCommandEntry)
command_entry.pack(padx=5, pady=5, side=LEFT)
Button(entry_frame, text='Send', command=onSendCommand,font = our_font).pack(padx=5, pady=5, side=LEFT)
entry_frame.pack()
observation = Label(root, text='observations will appear here', wraplength=640, font = small_font)
observation.pack()
reward = Label(root, text='rewards will appear here', wraplength=640, font = small_font)
reward.pack()

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
my_mission.requestVideo(640,480)
my_mission.timeLimitInSeconds(40)
my_mission.allowAllChatCommands()
#my_mission.createDefaultTerrain()
#my_mission.startArt(19,0,19)
my_mission.setTimeOfDay(1000,False)
my_mission.observeChat()
canvas.config( width=640, height=480 )

root.after(50, update)
root.mainloop()