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

# Human Action Component - use this to let humans play through the same missions as you give to agents

from future import standard_library
standard_library.install_aliases()
from builtins import range
from builtins import object
from past.utils import old_div
import MalmoPython
import os
import sys
import time
if sys.version_info[0] == 2:
    # Workaround for https://github.com/PythonCharmers/python-future/issues/262
    from Tkinter import *
    import tkMessageBox
    tkinter.messagebox = tkMessageBox
else:
    from tkinter import *
    import tkinter.messagebox
from PIL import Image
from PIL import ImageTk

class HumanAgentHost(object):

    def __init__( self ):
        '''Initializes the class.'''
        self.agent_host = MalmoPython.AgentHost()
        self.root = Tk()
        self.root.wm_title("Human Action Component")

    def parse( self, args ):
        '''Parses the command-line arguments.

        Parameters:
        args : list of strings, containing the command-line arguments (pass an empty list if unused).
        '''
        self.agent_host.parse( args )

    def getUsage(self):
        '''Returns the command-line arguments.''' 
        return self.agent_host.getUsage()

    def addOptionalStringArgument( self, name, description, default ):
        return self.agent_host.addOptionalStringArgument(name, description, default)

    def addOptionalIntArgument( self, name, description, default ):
        return self.agent_host.addOptionalIntArgument(name, description, default)

    def receivedArgument(self,arg):
        return self.agent_host.receivedArgument(arg)

    def getStringArgument(self,arg):
        return self.agent_host.getStringArgument(arg)

    def getIntArgument(self,arg):
        return self.agent_host.getIntArgument(arg)

    def runMission( self, mission_spec, mission_record_spec, role = 0 ):
        '''Sets a mission running.
        
        Parameters:
        mission_spec : MissionSpec instance, specifying the mission.
        mission_record_spec : MissionRecordSpec instance, specifying what should be recorded.
        role : int, the index of the role this human agent is to play. Zero based.
        '''
        
        self.world_state = None
        total_reward = 0
        
        # decide on the action space
        command_handlers = mission_spec.getListOfCommandHandlers(role)
        if 'ContinuousMovement' in command_handlers and 'DiscreteMovement' in command_handlers:
            print('ERROR: Ambiguous action space in supplied mission: both continuous and discrete command handlers present.')
            exit(1)
        elif 'ContinuousMovement' in command_handlers:
            self.action_space = 'continuous'
        elif 'DiscreteMovement' in command_handlers:
            self.action_space = 'discrete'
        else:
            print('ERROR: Unknown action space in supplied mission: neither continuous or discrete command handlers present.')
            exit(1)

        self.createGUI()
        
        if mission_spec.isVideoRequested(0):
            self.canvas.config( width=mission_spec.getVideoWidth(0), height=mission_spec.getVideoHeight(0) )

        # show the mission summary
        start_time = time.time()
        while time.time() - start_time < 4:
            canvas_id = self.canvas.create_rectangle(100, 100, 540, 200, fill="white", outline="red", width="5")
            self.canvas.create_text(320, 120, text=mission_spec.getSummary(), font=('Helvetica', '16'))
            self.canvas.create_text(320, 150, text=str(3 - int(time.time() - start_time)), font=('Helvetica', '16'), fill="red")
            self.root.update()
            time.sleep(0.2)
                
        try:
            self.agent_host.startMission( mission_spec, mission_record_spec )
        except RuntimeError as e:
            tkinter.messagebox.showerror("Error","Error starting mission: "+str(e))
            return

        print("Waiting for the mission to start", end=' ')
        self.world_state = self.agent_host.peekWorldState()
        while not self.world_state.has_mission_begun:
            print(".", end="")
            time.sleep(0.1)
            self.world_state = self.agent_host.peekWorldState()
            for error in self.world_state.errors:
                print("Error:",error.text)
        print()
        if self.action_space == 'continuous':
            self.canvas.config(cursor='none') # hide the mouse cursor while over the canvas
            self.canvas.event_generate('<Motion>', warp=True, x=old_div(self.canvas.winfo_width(),2), y=old_div(self.canvas.winfo_height(),2)) # put cursor at center
            self.root.after(50, self.update)
        self.canvas.focus_set()

        while self.world_state.is_mission_running:
            self.world_state = self.agent_host.getWorldState()
            if self.world_state.number_of_observations_since_last_state > 0:
                self.observation.config(text = self.world_state.observations[0].text )
            if mission_spec.isVideoRequested(0) and self.world_state.number_of_video_frames_since_last_state > 0:
                frame = self.world_state.video_frames[-1]
                image = Image.frombytes('RGB', (frame.width,frame.height), bytes(frame.pixels) )
                photo = ImageTk.PhotoImage(image)
                self.canvas.delete("all")
                self.canvas.create_image(old_div(frame.width,2), old_div(frame.height,2), image=photo)
            self.canvas.create_line( old_div(self.canvas.winfo_width(),2)-5, old_div(self.canvas.winfo_height(),2),   old_div(self.canvas.winfo_width(),2)+6, old_div(self.canvas.winfo_height(),2),   fill='white' )
            self.canvas.create_line( old_div(self.canvas.winfo_width(),2),   old_div(self.canvas.winfo_height(),2)-5, old_div(self.canvas.winfo_width(),2),   old_div(self.canvas.winfo_height(),2)+6, fill='white' )
            # parse reward
            for reward in self.world_state.rewards:
                total_reward += reward.getValue()
            self.reward.config(text = str(total_reward) )
            self.root.update()
            time.sleep(0.01)
        if self.action_space == 'continuous':
            self.canvas.config(cursor='arrow') # restore the mouse cursor
        print('Mission stopped')
        if not self.agent_host.receivedArgument("test"):
            tkinter.messagebox.showinfo("Mission ended","Mission has ended. Total reward: " + str(total_reward) )
        self.root_frame.destroy()
        
    def createGUI( self ):
        '''Create the graphical user interface.'''
        our_font = "Helvetica 16 bold"
        small_font = "Helvetica 9 bold"
        self.root_frame = Frame(self.root)
        if self.action_space == 'continuous':
            desc = "Running continuous-action mission.\nUse the mouse to turn, WASD to move."
        else:
            desc = "Running discrete-action mission.\nUse the arrow keys to turn and move."
        Label(self.root_frame, text=desc,font = our_font,wraplength=640).pack(padx=5, pady=5)
        self.canvas = Canvas(self.root_frame, borderwidth=0, highlightthickness=0, width=640, height=480, bg="gray" )
        self.canvas.bind('<Motion>',self.onMouseMoveInCanvas)
        self.canvas.bind('<Button-1>',self.onLeftMouseDownInCanvas)
        self.canvas.bind('<ButtonRelease-1>',self.onLeftMouseUpInCanvas)
        if sys.platform == 'darwin': right_mouse_button = '2' # on MacOSX, the right button is 'Button-2'
        else:                        right_mouse_button = '3' # on Windows and Linux the right button is 'Button-3'
        self.canvas.bind('<Button-'+right_mouse_button+'>',self.onRightMouseDownInCanvas)
        self.canvas.bind('<ButtonRelease-'+right_mouse_button+'>',self.onRightMouseUpInCanvas)
        self.canvas.bind('<KeyPress>',self.onKeyPressInCanvas)
        self.canvas.bind('<KeyRelease>',self.onKeyReleaseInCanvas)
        self.canvas.pack(padx=5, pady=5)
        self.entry_frame = Frame(self.root_frame)
        Label(self.entry_frame, text="Type '/' to enter command:",font = small_font).pack(padx=5, pady=5, side=LEFT)
        self.command_entry = Entry(self.entry_frame,font = small_font)
        self.command_entry.bind('<Key>',self.onKeyInCommandEntry)
        self.command_entry.pack(padx=5, pady=5, side=LEFT)
        Button(self.entry_frame, text='Send', command=self.onSendCommand,font = small_font).pack(padx=5, pady=5, side=LEFT)
        self.entry_frame.pack()
        self.observation = Label(self.root_frame, text='observations will appear here', wraplength=640, font = small_font)
        self.observation.pack()
        self.reward = Label(self.root_frame, text='rewards will appear here', wraplength=640, font = small_font)
        self.reward.pack()
        self.root_frame.pack()
        self.mouse_event = self.prev_mouse_event = None
 
    def onSendCommand(self):
        '''Called when user presses the 'send' button or presses 'Enter' while the command entry box has focus.'''
        self.agent_host.sendCommand(self.command_entry.get())
        self.command_entry.delete(0,END)
        self.canvas.focus_set()

    def update(self):
        '''Called at regular intervals to poll the mouse position to send continuous commands.'''
        if self.action_space == 'continuous': # mouse movement only used for continuous action space
            if self.world_state and self.world_state.is_mission_running:
                if self.mouse_event and self.prev_mouse_event:
                        rotation_speed = 0.1
                        turn_speed = ( self.mouse_event.x - self.prev_mouse_event.x ) * rotation_speed
                        pitch_speed = ( self.mouse_event.y - self.prev_mouse_event.y ) * rotation_speed
                        self.agent_host.sendCommand( 'turn '+str(turn_speed) )
                        self.agent_host.sendCommand( 'pitch '+str(pitch_speed) )
                if self.mouse_event:
                    if os.name == 'nt': # (moving the mouse cursor only seems to work on Windows)
                        self.canvas.event_generate('<Motion>', warp=True, x=old_div(self.canvas.winfo_width(),2), y=old_div(self.canvas.winfo_height(),2)) # put cursor at center
                        self.mouse_event.x = old_div(self.canvas.winfo_width(),2)
                        self.mouse_event.y = old_div(self.canvas.winfo_height(),2)
                    self.prev_mouse_event = self.mouse_event
        if self.world_state.is_mission_running:
            self.root.after(50, self.update)

    def onMouseMoveInCanvas(self, event):
        '''Called when the mouse moves inside the canvas.'''
        self.mouse_event = event
      
    def onLeftMouseDownInCanvas(self, event):
        '''Called when the left mouse button is pressed on the canvas.'''
        self.canvas.focus_set()
        self.agent_host.sendCommand( 'attack 1' )
      
    def onLeftMouseUpInCanvas(self, event):
        '''Called when the left mouse button is released on the canvas.'''
        self.canvas.focus_set()
        self.agent_host.sendCommand( 'attack 0' )
      
    def onRightMouseDownInCanvas(self, event):
        '''Called when the right mouse button is pressed on the canvas.'''
        self.canvas.focus_set()
        self.agent_host.sendCommand( 'use 1' )
      
    def onRightMouseUpInCanvas(self, event):
        '''Called when the right mouse button is released on the canvas.'''
        self.canvas.focus_set()
        self.agent_host.sendCommand( 'use 0' )
      
    def onKeyInCommandEntry(self, event):
        '''Called when a key is pressed when the command entry box has focus.'''
        if event.char == '\r':
            self.onSendCommand()
            self.canvas.focus_set() # move focus back to the canvas to continue moving
           
    def onKeyPressInCanvas(self, event):
        '''Called when a key is pressed when the canvas has focus.'''
        char_map = { 'w':'move 1', 'a':'strafe -1', 's':'move -1', 'd':'strafe 1', ' ':'jump 1' }
        keysym_map = { 'continuous': { 'Left':'turn -1', 'Right':'turn 1', 'Up':'pitch -1', 'Down':'pitch 1', 'Shift_L':'crouch 1',
                                       'Shift_R':'crouch 1', 
                                       '1':'hotbar.1 1', '2':'hotbar.2 1', '3':'hotbar.3 1', '4':'hotbar.4 1', '5':'hotbar.5 1',
                                       '6':'hotbar.6 1', '7':'hotbar.7 1', '8':'hotbar.8 1', '9':'hotbar.9 1' },
                       'discrete':   { 'Left':'turn -1', 'Right':'turn 1', 'Up':'move 1', 'Down':'move -1', 
                                       '1':'hotbar.1 1', '2':'hotbar.2 1', '3':'hotbar.3 1', '4':'hotbar.4 1', '5':'hotbar.5 1',
                                       '6':'hotbar.6 1', '7':'hotbar.7 1', '8':'hotbar.8 1', '9':'hotbar.9 1' } }
        if event.char == '/':
            self.command_entry.focus_set() # interlude to allow user to type command
        elif event.char.lower() in char_map:
            self.agent_host.sendCommand( char_map[ event.char.lower() ] )
        elif event.keysym in keysym_map[self.action_space]:
            self.agent_host.sendCommand( keysym_map[self.action_space][ event.keysym ] )

    def onKeyReleaseInCanvas(self, event):
        '''Called when a key is released when the command entry box has focus.'''
        char_map = { 'w':'move 0', 'a':'strafe 0', 's':'move 0', 'd':'strafe 0', ' ':'jump 0' }
        keysym_map = { 'Left':'turn 0', 'Right':'turn 0', 'Up':'pitch 0', 'Down':'pitch 0', 'Shift_L':'crouch 0', 'Shift_R':'crouch 0', 
                       '1':'hotbar.1 0', '2':'hotbar.2 0', '3':'hotbar.3 0', '4':'hotbar.4 0', '5':'hotbar.5 0',
                       '6':'hotbar.6 0', '7':'hotbar.7 0', '8':'hotbar.8 0', '9':'hotbar.9 0' }
        if event.char.lower() in char_map:
            self.agent_host.sendCommand( char_map[ event.char.lower() ] )
        elif event.keysym in keysym_map:
            self.agent_host.sendCommand( keysym_map[ event.keysym ] )
            
if sys.version_info[0] == 2:
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
else:
    import functools
    print = functools.partial(print, flush=True)

human_agent_host = HumanAgentHost()
human_agent_host.addOptionalStringArgument( "mission_xml,m", "Mission XML file name.", "" )
human_agent_host.addOptionalIntArgument( "role", "The role of the human agent. Zero based", 0 )
try:
    human_agent_host.parse( sys.argv )
except RuntimeError as e:
    print('ERROR:',e)
    print(human_agent_host.getUsage())
    exit(1)
if human_agent_host.receivedArgument("help"):
    print(human_agent_host.getUsage())
    exit(0)
    
my_role = human_agent_host.getIntArgument("role")

xml_filename = human_agent_host.getStringArgument("mission_xml")
if not xml_filename == "":
    # load the mission from the specified XML file

    my_mission = MalmoPython.MissionSpec( open(xml_filename).read(), True)
    my_mission.requestVideo( 640, 480 )
    my_mission.timeLimitInSeconds( 300 )
    
    my_mission_record = MalmoPython.MissionRecordSpec('./hac_saved_mission.tgz')
    my_mission_record.recordCommands()
    my_mission_record.recordMP4( 20, 400000 )
    my_mission_record.recordRewards()
    my_mission_record.recordObservations()

    human_agent_host.runMission( my_mission, my_mission_record, role = my_role )

else:
    # create some sample missions

    for rep in range(2):
        my_mission = MalmoPython.MissionSpec()
        my_mission.setSummary('A sample mission - run onto the gold block')
        my_mission.requestVideo( 640, 480 )
        my_mission.timeLimitInSeconds( 30 )
        my_mission.allowAllChatCommands()
        my_mission.allowAllInventoryCommands()
        my_mission.setTimeOfDay( 1000, False )
        my_mission.observeChat()
        my_mission.observeGrid( -1, -1, -1, 1, 1, 1, 'grid' )
        my_mission.observeHotBar()
        my_mission.drawBlock( 5, 226, 5, 'gold_block' )
        my_mission.rewardForReachingPosition( 5.5, 227, 5.5, 100, 0.5 )
        my_mission.endAt( 5.5, 227, 5.5, 0.5 )
        my_mission.startAt( 0.5, 227, 0.5 )
        if rep%2 == 1: # alternate between continuous and discrete missions, for fun
            my_mission.removeAllCommandHandlers()
            my_mission.allowAllDiscreteMovementCommands()

        my_mission_record = MalmoPython.MissionRecordSpec('./hac_saved_mission_'+str(rep)+'.tgz')
        my_mission_record.recordCommands()
        my_mission_record.recordMP4( 20, 400000 )
        my_mission_record.recordRewards()
        my_mission_record.recordObservations()

        human_agent_host.runMission( my_mission, my_mission_record, role = my_role )
