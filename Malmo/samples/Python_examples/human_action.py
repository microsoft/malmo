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
import Tkinter as tk
from PIL import Image
from PIL import ImageTk

root = tk.Tk()
root.wm_title("Video Output")
canvas = tk.Canvas(root, width=320, height=200, borderwidth=0, highlightthickness=0, bg="white")
canvas.pack()

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
canvas.config( width=800, height=600 )
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

agent_host.sendCommand('move 1')

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
