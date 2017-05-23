-- --------------------------------------------------------------------------------------------------
--  Copyright (c) 2016 Microsoft Corporation
--  
--  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
--  associated documentation files (the "Software"), to deal in the Software without restriction,
--  including without limitation the rights to use, copy, modify, merge, publish, distribute,
--  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
--  furnished to do so, subject to the following conditions:
--  
--  The above copyright notice and this permission notice shall be included in all copies or
--  substantial portions of the Software.
--  
--  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
--  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
--  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
--  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
--  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-- --------------------------------------------------------------------------------------------------

require 'libMalmoLua'
require "socket"

function sleep(sec)
    socket.select(nil, nil, sec)
end

local agent_host = AgentHost()
local status, err = pcall( function() agent_host:parse( arg ) end )
if not status then
    print('Error parsing command-line arguments: '..err)
    print(agent_host:getUsage())
    os.exit(1)
end
if agent_host:receivedArgument("help") then
    print(agent_host:getUsage())
    os.exit(0)
end

local my_mission = MissionSpec()
my_mission:timeLimitInSeconds( 10 )
my_mission:requestVideo( 320, 240 )
my_mission:rewardForReachingPosition(19.5,0.0,19.5,100.0,1.1)

local my_mission_record = MissionRecordSpec("./saved_data.tgz")
my_mission_record:recordCommands()
my_mission_record:recordMP4(20, 400000)
my_mission_record:recordRewards()
my_mission_record:recordObservations()
                 
status, err = pcall( function() agent_host:startMission( my_mission, my_mission_record ) end )
if not status then
    print( "Error starting mission: "..err )
    -- err is actually a MissionException object, containing a message and a code.
    -- we can use the code to do more systematic error handling. For example:
    if err:code() == MissionErrorCode.MISSION_INSUFFICIENT_CLIENTS_AVAILABLE then
        print( "Need to make sure a Minecraft client is running." )
    end
    os.exit(1)
end

io.write( "Waiting for the mission to start" )
world_state = agent_host:getWorldState()
while not world_state.has_mission_begun do
    io.write( "." )
    io.flush()
    sleep(0.1)
    world_state = agent_host:getWorldState()
    for error in world_state.errors do
        print("Error: "..error.text)
    end
end
io.write( "\n" )

-- main loop:
while world_state.is_mission_running do
    agent_host:sendCommand( "move 1" );
    agent_host:sendCommand( "turn "..math.random() );
    sleep(0.5)
    world_state = agent_host:getWorldState()
    print( "video,observations,rewards received: "..world_state.number_of_video_frames_since_last_state..","..world_state.number_of_observations_since_last_state..","..world_state.number_of_rewards_since_last_state )
    for reward in world_state.rewards do
        print( "Summed reward: "..reward.getValue() )
        print( "Timestamp of most recent reward: "..reward:timestamp() )   -- in milliseconds since Jan 1st, 1970.
    end
    for error in world_state.errors do
        print( "Error: "..error.text )
    end
end

print( "Mission has stopped." )
