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

class HumanAgentHost:

    def __init__( self, args ):
    
        self.createGUI()
        self.root.after(50, self.update)

        self.agent_host = MalmoPython.AgentHost()
        try:
            self.agent_host.parse( args )
        except RuntimeError as e:
            print 'ERROR:',e
            print self.agent_host.getUsage()
            raise RuntimeError(e)
            
    def createGUI( self ):
        our_font = "Helvetica 16 bold"
        small_font = "Helvetica 9 bold"
        self.root = Tk()
        self.root.wm_title("Human Action Component")
        #self.start_button = Button(root, text='Start', command=self.onStartMission,font = our_font)
        #self.start_button.pack(padx=5, pady=5)
        self.canvas = Canvas(self.root, borderwidth=0, highlightthickness=0, bg="white" )
        self.canvas.bind('<Motion>',self.onMouseMoveInCanvas)
        self.canvas.bind('<Button-1>',self.onLeftMouseDownInCanvas)
        self.canvas.bind('<ButtonRelease-1>',self.onLeftMouseUpInCanvas)
        self.canvas.bind('<Button-3>',self.onRightMouseDownInCanvas)
        self.canvas.bind('<ButtonRelease-3>',self.onRightMouseUpInCanvas)
        self.canvas.bind('<KeyPress>',self.onKeyPressInCanvas)
        self.canvas.bind('<KeyRelease>',self.onKeyReleaseInCanvas)
        self.canvas.pack(padx=5, pady=5)
        self.entry_frame = Frame(self.root)
        Label(self.entry_frame, text="Command:",font = our_font).pack(padx=5, pady=5, side=LEFT)
        self.command_entry = Entry(self.entry_frame,font = our_font)
        self.command_entry.bind('<Key>',self.onKeyInCommandEntry)
        self.command_entry.pack(padx=5, pady=5, side=LEFT)
        Button(self.entry_frame, text='Send', command=self.onSendCommand,font = our_font).pack(padx=5, pady=5, side=LEFT)
        self.entry_frame.pack()
        self.observation = Label(self.root, text='observations will appear here', wraplength=640, font = small_font)
        self.observation.pack()
        self.reward = Label(self.root, text='rewards will appear here', wraplength=640, font = small_font)
        self.reward.pack()
        self.mouse_event = self.prev_mouse_event = None
        
    def runMission( self, mission_spec, mission_record_spec ):

        sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
        self.world_state = None

        if mission_spec.isVideoRequested(0):
            self.canvas.config( width=mission_spec.getVideoWidth(0), height=mission_spec.getVideoHeight(0) )

        try:
            self.agent_host.startMission( mission_spec, mission_record_spec )
        except RuntimeError as e:
            tkMessageBox.showerror("Error","Error starting mission: "+str(e))
            return

        print "Waiting for the mission to start",
        self.world_state = self.agent_host.peekWorldState()
        while not self.world_state.is_mission_running:
            sys.stdout.write(".")
            time.sleep(0.1)
            self.world_state = self.agent_host.peekWorldState()
            for error in self.world_state.errors:
                print "Error:",error.text
        print
        if mission_spec.isVideoRequested(0):
            self.canvas.config(cursor='none') # hide the mouse cursor while over the canvas
            self.canvas.event_generate('<Motion>', warp=True, x=self.canvas.winfo_width()/2, y=self.canvas.winfo_height()/2) # put cursor at center
            self.canvas.focus_set()
        #self.start_button.config(state='disabled')
        #self.start_button.config(text='Running...')

        while self.world_state.is_mission_running:
            if self.world_state.number_of_observations_since_last_state > 0:
                self.observation.config(text = self.world_state.observations[0].text )
            if self.world_state.number_of_rewards_since_last_state > 0:
                self.rewards.config(text = str(self.world_state.rewards[0].getValue()) )
            if mission_spec.isVideoRequested(0) and self.world_state.number_of_video_frames_since_last_state > 0:
                frame = self.world_state.video_frames[-1]
                image = Image.frombytes('RGB', (frame.width,frame.height), str(frame.pixels) )
                photo = ImageTk.PhotoImage(image)
                self.canvas.delete("all")
                self.canvas.create_image(frame.width/2, frame.height/2, image=photo)
                self.canvas.create_line( frame.width/2 - 5, frame.height/2, frame.width/2 + 6, frame.height/2, fill='white' )
                self.canvas.create_line( frame.width/2, frame.height/2 - 5, frame.width/2, frame.height/2 + 6, fill='white' )
                self.root.update()
            time.sleep(0.01)
            self.world_state = self.agent_host.getWorldState()
        if mission_spec.isVideoRequested(0):
            self.canvas.config(cursor='arrow') # restore the mouse cursor
        #self.start_button.config(state='normal')
        #self.start_button.config(text='Start')
        print 'Mission stopped'
        
    def onSendCommand(self):
        self.agent_host.sendCommand(self.command_entry.get())
        self.command_entry.delete(0,END)

    def update(self):
        if self.world_state and self.world_state.is_mission_running:
            if self.mouse_event and self.prev_mouse_event:
                rotation_speed = 0.1
                turn_speed = ( self.mouse_event.x - self.prev_mouse_event.x ) * rotation_speed
                pitch_speed = ( self.mouse_event.y - self.prev_mouse_event.y ) * rotation_speed
                self.agent_host.sendCommand( 'turn '+str(turn_speed) )
                self.agent_host.sendCommand( 'pitch '+str(pitch_speed) )
            if self.mouse_event:
                if os.name == 'nt': # (moving the mouse cursor only seems to work on Windows)
                    self.canvas.event_generate('<Motion>', warp=True, x=self.canvas.winfo_width()/2, y=self.canvas.winfo_height()/2) # put cursor at center
                    self.mouse_event.x = self.canvas.winfo_width()/2
                    self.mouse_event.y = self.canvas.winfo_height()/2
                self.prev_mouse_event = self.mouse_event
        self.root.after(50, self.update)

    def onMouseMoveInCanvas(self, event):
        self.mouse_event = event
      
    def onLeftMouseDownInCanvas(self, event):
        self.canvas.focus_set()
        self.agent_host.sendCommand( 'attack 1' )
      
    def onLeftMouseUpInCanvas(self, event):
        self.canvas.focus_set()
        self.agent_host.sendCommand( 'attack 0' )
      
    def onRightMouseDownInCanvas(self, event):
        self.canvas.focus_set()
        self.agent_host.sendCommand( 'use 1' )
      
    def onRightMouseUpInCanvas(self, event):
        self.canvas.focus_set()
        self.agent_host.sendCommand( 'use 0' )
      
    def onKeyInCommandEntry(self, event):
        if event.char == '\r':
            self.onSendCommand()
            self.canvas.focus_set() # move focus back to the canvas to continue moving
           
    def onKeyPressInCanvas(self, event):
        char_map = { 'w':'move 1', 'a':'strafe -1', 's':'move -1', 'd':'strafe 1', ' ':'jump 1' }
        keysym_map = { 'Left':'turn -1', 'Right':'turn 1', 'Up':'pitch -1', 'Down':'pitch 1' }
        if event.char == '/':
            self.command_entry.focus_set() # interlude to allow user to type command
        elif event.char in char_map:
            self.agent_host.sendCommand( char_map[ event.char ] )
        elif event.keysym in keysym_map:
            self.agent_host.sendCommand( keysym_map[ event.keysym ] )

    def onKeyReleaseInCanvas(self, event):
        char_map = { 'w':'move 0', 'a':'strafe 0', 's':'move 0', 'd':'strafe 0', ' ':'jump 0' }
        keysym_map = { 'Left':'turn 0', 'Right':'turn 0', 'Up':'pitch 0', 'Down':'pitch 0' }
        if event.char in char_map:
            self.agent_host.sendCommand( char_map[ event.char ] )
        elif event.keysym in keysym_map:
            self.agent_host.sendCommand( keysym_map[ event.keysym ] )

# create a mission specification, and a mission record specification
            
my_mission = MalmoPython.MissionSpec()
my_mission.requestVideo(640,480)
my_mission.timeLimitInSeconds(10)
my_mission.allowAllChatCommands()
#my_mission.createDefaultTerrain()
#my_mission.startArt(19,0,19)
my_mission.setTimeOfDay(1000,False)
my_mission.observeChat()

human_agent_host = HumanAgentHost( sys.argv )
for rep in range(2):
    human_agent_host.runMission( my_mission, MalmoPython.MissionRecordSpec() )
