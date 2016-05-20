-- --------------------------------------------------------------------------------------------------------------------
-- Copyright (C) Microsoft Corporation.  All rights reserved.
-- --------------------------------------------------------------------------------------------------------------------

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
my_mission:rewardForReachingPosition(19,0,19,100.0,1.1)

local my_mission_record = MissionRecordSpec("./saved_data.tgz")
my_mission_record:recordCommands()
my_mission_record:recordMP4(20, 400000)
my_mission_record:recordRewards()
my_mission_record:recordObservations()
                 
status, err = pcall( function() agent_host:startMission( my_mission, my_mission_record ) end )
if not status then
    print( "Error starting mission: "..err )
    os.exit(1)
end

io.write( "Waiting for the mission to start" )
world_state = agent_host:getWorldState()
while not world_state.is_mission_running do
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
        print( "Summed reward: "..reward.value )
        print( "Timestamp of most recent reward: "..reward:timestamp() )   -- in milliseconds since Jan 1st, 1970.
    end
    for error in world_state.errors do
        print( "Error: "..error.text )
    end
end

print( "Mission has stopped." )
