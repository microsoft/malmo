// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Malmo:
#include <AgentHost.h>
#include <WorldState.h>
using namespace malmo;

// Boost:
#include <boost/shared_ptr.hpp>

// STL:
#include <iostream>
using namespace std;

int main() 
{
    AgentHost agent_host;
    agent_host.setVideoPolicy( AgentHost::VideoPolicy::LATEST_FRAME_ONLY );
    agent_host.setRewardsPolicy( AgentHost::RewardsPolicy::SUM_REWARDS );
    agent_host.setObservationsPolicy( AgentHost::ObservationsPolicy::LATEST_OBSERVATION_ONLY );
    
    boost::shared_ptr<const WorldState> world_state = agent_host.getWorldState();

    if( world_state->is_mission_running )
        return EXIT_FAILURE;
    
    if( world_state->number_of_observations_since_last_state != 0 )
        return EXIT_FAILURE;
    
    if( world_state->number_of_rewards_since_last_state != 0 )
        return EXIT_FAILURE;
    
    if( world_state->number_of_video_frames_since_last_state != 0 )
        return EXIT_FAILURE;
    
    if( world_state->observations.size() != 0 )
        return EXIT_FAILURE;
    
    if( world_state->rewards.size() != 0 )
        return EXIT_FAILURE;
    
    if( world_state->video_frames.size() != 0 )
        return EXIT_FAILURE;
    
    cout << agent_host.getUsage() << endl;

    return EXIT_SUCCESS;
}
