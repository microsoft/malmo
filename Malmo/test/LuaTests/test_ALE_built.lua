-- --------------------------------------------------------------------------------------------------------------------
-- Copyright (C) Microsoft Corporation.  All rights reserved.
-- --------------------------------------------------------------------------------------------------------------------

require 'libMalmoLua'

agent_host = ALEAgentHost()
agent_host:setVideoPolicy( AgentHost.LATEST_FRAME_ONLY )
agent_host:setRewardsPolicy( AgentHost.SUM_REWARDS )
agent_host:setObservationsPolicy( AgentHost.LATEST_OBSERVATION_ONLY )

world_state = agent_host:getWorldState()

assert( not world_state.is_mission_running, 'World state says mission is already running.' )

assert( world_state.number_of_observations_since_last_state == 0, 'World state says observations already received.' )

assert( world_state.number_of_rewards_since_last_state == 0, 'World state says rewards already received.' )

assert( world_state.number_of_video_frames_since_last_state == 0, 'World state says video frames already received.' )

num_observations = 0
for obs in world_state.observations do
    num_observations = num_observations + 1
    print( "Observation: "..obs.text )
end
assert( num_observations == 0, 'World state says observations stored.' )

num_rewards = 0
for reward in world_state.rewards do
    num_rewards = num_rewards + 1
    print( "Reward: "..reward.value )
end
assert( num_rewards == 0, 'World state says rewards stored.' )

num_video_frames = 0
for frame in world_state.video_frames do
    num_video_frames = num_video_frames + 1
    print( "Video frame: "..frame.width.."x"..frame.height.."x"..frame.channels )
    for pixel in frame.pixels do
        print( "Pixel: "..pixel )
    end
end
assert( num_video_frames == 0, 'World state says video frames stored.' )

print( agent_host:getUsage() )
