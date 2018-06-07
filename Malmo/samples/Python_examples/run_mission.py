from __future__ import print_function
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

from builtins import range
import sys
import os
import random
import time
import uuid
#from PIL import Image

# Allow MalmoPython to be imported both from an installed
# malmo module and (as an override) separately as a native library.
try:
    import MalmoPython
    import malmoutils
except ImportError:
    import malmo.MalmoPython as MalmoPython
    import malmo.malmoutils as malmoutils


class MissionTimeoutException(Exception):
    pass


def restart_minecraft(world_state, agent_host, client_info, message):
    """"Attempt to quit mission if running and kill the client"""
    if world_state.is_mission_running:
        agent_host.sendCommand("quit")
        time.sleep(10)
    agent_host.killClient(client_info)
    raise MissionTimeoutException(message)


def run(argv=['']):
    if "MALMO_XSD_PATH" not in os.environ:
        print("Please set the MALMO_XSD_PATH environment variable.")
        return

    malmoutils.fix_print()

    agent_host = MalmoPython.AgentHost()
    malmoutils.parse_command_line(agent_host, argv)

    my_mission = MalmoPython.MissionSpec()
    my_mission.timeLimitInSeconds( 10 )
    my_mission.requestVideo( 320, 240 )
    my_mission.rewardForReachingPosition( 19.5, 0.0, 19.5, 100.0, 1.1 )

    my_mission_record = malmoutils.get_default_recording_object(agent_host, "saved_data")

    # client_info = MalmoPython.ClientInfo('localhost', 10000)
    client_info = MalmoPython.ClientInfo('127.0.0.1', 10000)
    pool = MalmoPython.ClientPool()
    pool.add(client_info)

    experiment_id = str(uuid.uuid1())
    print("experiment id " + experiment_id)

    max_retries = 3
    max_response_time = 60  # seconds

    for retry in range(max_retries):
        try:
            agent_host.startMission(my_mission, pool, my_mission_record, 0, experiment_id)
            break
        except RuntimeError as e:
            if retry == max_retries - 1:
                print("Error starting mission:",e)
                exit(1)
            else:
                time.sleep(2)

    print("Waiting for the mission to start", end=' ')
    start_time = time.time()
    world_state = agent_host.getWorldState()
    while not world_state.has_mission_begun:
        print(".", end="")
        time.sleep(0.1)
        if time.time() - start_time > max_response_time:
            print("Max delay exceeded for mission to begin")
            restart_minecraft(world_state, agent_host, client_info, "begin mission")
        world_state = agent_host.getWorldState()
        for error in world_state.errors:
            print("Error:",error.text)
    print()

    last_delta = time.time()
    # main loop:
    while world_state.is_mission_running:
        agent_host.sendCommand( "move 1" )
        agent_host.sendCommand( "turn " + str(random.random()*2-1) )
        time.sleep(0.5)
        world_state = agent_host.getWorldState()
        print("video,observations,rewards received:",world_state.number_of_video_frames_since_last_state,world_state.number_of_observations_since_last_state,world_state.number_of_rewards_since_last_state)
        if (world_state.number_of_video_frames_since_last_state > 0 or
           world_state.number_of_observations_since_last_state > 0 or
           world_state.number_of_rewards_since_last_state > 0):
            last_delta = time.time()
        else:
            if time.time() - last_delta > max_response_time:
                print("Max delay exceeded for world state change")
                restart_minecraft(world_state, agent_host, client_info, "world state change")
        for reward in world_state.rewards:
            print("Summed reward:",reward.getValue())
        for error in world_state.errors:
            print("Error:",error.text)
        for frame in world_state.video_frames:
            print("Frame:",frame.width,'x',frame.height,':',frame.channels,'channels')
            #image = Image.frombytes('RGB', (frame.width, frame.height), bytes(frame.pixels) ) # to convert to a PIL image
    print("Mission has stopped.")


if __name__ == "__main__":
    run(sys.argv)
