// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

// Malmo:
#include <AgentHost.h>
#include <MissionSpec.h>
#include <MissionRecordSpec.h>
#include <ClientPool.h>
#include <WorldState.h>
using namespace malmo;

// STL:
#include <cstddef>
#include <string>
#include <exception>
#include <iostream>
using namespace std;

// Local:
#include "x_agent_host.h"

ptAgentHost new_agent_host() {
    try {
        AgentHost * pt = new AgentHost;
        return (void*)pt;
    } catch (const exception& e) {
        // returning NULL pointer
    }
    return NULL;
}

void free_agent_host(ptAgentHost agent_host) {
    if (agent_host != NULL) {
        AgentHost * pt = (AgentHost*)agent_host;
        delete pt;
    }
}

void agent_host_initialise_enums(
	int *latest_frame_only,
	int *keep_all_frames,

	int *latest_reward_only,
	int *sum_rewards,
	int *keep_all_rewards,

	int *latest_observation_only,
	int *keep_all_observations
) {
	*latest_frame_only       = AgentHost::LATEST_FRAME_ONLY;
	*keep_all_frames         = AgentHost::KEEP_ALL_FRAMES;
                                                                  
	*latest_reward_only      = AgentHost::LATEST_REWARD_ONLY;
	*sum_rewards             = AgentHost::SUM_REWARDS;
	*keep_all_rewards        = AgentHost::KEEP_ALL_REWARDS;
                                                                  
	*latest_observation_only = AgentHost::LATEST_OBSERVATION_ONLY;
	*keep_all_observations   = AgentHost::KEEP_ALL_OBSERVATIONS;
}

int agent_host_parse(ptAgentHost pt, char* err, int argc, const char** argv) {
    AH_CALL(
        agent_host->parseArgs(argc, argv);
    )
}

int agent_host_get_usage(ptAgentHost pt, char* err, char* usage) {
    AH_CALL(
        string str_usage = agent_host->getUsage();
        if (str_usage.size() > AH_USAGE_BUFFER_SIZE) {
            strncpy(err, "Size of usage string exceeds capacity", AH_ERROR_BUFFER_SIZE);
            return 1;
        }
        strncpy(usage, str_usage.c_str(), AH_USAGE_BUFFER_SIZE);
    )
}

int agent_host_received_argument(ptAgentHost pt, char* err, const char* name, int* response) {
    AH_CALL(
        if (agent_host->receivedArgument(name)) {
            *response = 1;
        } else {
            *response = 0;
        }
    )
}

int agent_host_start_mission(ptAgentHost pt, char* err, ptMissionSpec ptmission, ptClientPool ptclient_pool, ptMissionRecordSpec ptmission_record, int role, const char* unique_experiment_id) {
    AH_CALL(
        MissionSpec* mission = (MissionSpec*)ptmission;
        ClientPool* client_pool = (ClientPool*)ptclient_pool;
        MissionRecordSpec* mission_record = (MissionRecordSpec*)ptmission_record;
        agent_host->startMission(*mission, *client_pool, *mission_record, role, unique_experiment_id);
    )
}

int agent_host_start_mission_simple(ptAgentHost pt, char* err, ptMissionSpec mission, ptMissionRecordSpec mission_record) {
    AH_CALL(
    )
}

int agent_host_peek_world_state(ptAgentHost pt, char* err,
        int* has_mission_begun,
        int* is_mission_running,
        int* number_of_video_frames_since_last_state,
        int* number_of_rewards_since_last_state,
        int* number_of_observations_since_last_state) {
    AH_CALL(
        WorldState ws = agent_host->peekWorldState();
        *has_mission_begun  = ws.has_mission_begun  ? 1 : 0;
        *is_mission_running = ws.is_mission_running ? 1 : 0;
        *number_of_video_frames_since_last_state   = ws.number_of_video_frames_since_last_state;
        *number_of_rewards_since_last_state        = ws.number_of_rewards_since_last_state;
        *number_of_observations_since_last_state   = ws.number_of_observations_since_last_state;
    )
}

int agent_host_get_world_state(ptAgentHost pt, char* err,
        int* has_mission_begun,
        int* is_mission_running,
        int* number_of_video_frames_since_last_state,
        int* number_of_rewards_since_last_state,
        int* number_of_observations_since_last_state) {
    AH_CALL(
        WorldState ws = agent_host->getWorldState();
        *has_mission_begun  = ws.has_mission_begun  ? 1 : 0;
        *is_mission_running = ws.is_mission_running ? 1 : 0;
        *number_of_video_frames_since_last_state   = ws.number_of_video_frames_since_last_state;
        *number_of_rewards_since_last_state        = ws.number_of_rewards_since_last_state;
        *number_of_observations_since_last_state   = ws.number_of_observations_since_last_state;
    )
}

int agent_host_get_recording_temporary_directory(ptAgentHost pt, char* err, char* response) {
    AH_CALL(
    )
}

int agent_host_set_debug_output(ptAgentHost pt, char* err, int debug) {
    AH_CALL(
    )
}

int agent_host_set_video_policy(ptAgentHost pt, char* err, int videoPolicy) {
    AH_CALL(
        agent_host->setVideoPolicy((AgentHost::VideoPolicy)videoPolicy);
    )
}

int agent_host_set_rewards_policy(ptAgentHost pt, char* err, int rewardsPolicy) {
    AH_CALL(
        agent_host->setRewardsPolicy((AgentHost::RewardsPolicy)rewardsPolicy);
    )
}

int agent_host_set_observations_policy(ptAgentHost pt, char* err, int observationsPolicy) {
    AH_CALL(
        agent_host->setObservationsPolicy((AgentHost::ObservationsPolicy)observationsPolicy);
    )
}

int agent_host_send_command(ptAgentHost pt, char* err, const char* command) {
    AH_CALL(
    )
}

int agent_host_send_command_turnbased(ptAgentHost pt, char* err, const char* command, const char* key) {
    AH_CALL(
    )
}
