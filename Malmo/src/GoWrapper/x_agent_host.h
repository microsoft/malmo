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

#ifndef AGENTHOST_H
#define AGENTHOST_H

#ifdef __cplusplus
extern "C" {
#endif

// max number of characters for error message from C++ to Go
#define AH_ERROR_BUFFER_SIZE 1024

// max number of characters for usage text from C++ to Go
#define AH_USAGE_BUFFER_SIZE 1024

// max number of characters for recording_directory text from C++ to Go
#define AH_RECDIR_BUFFER_SIZE 252

// macro to help with calling AgentHost methods and handling exception errors
// this macro assumes that the following variables are defined:
//   pt  -- pointer to AgentHost
//   err -- error message buffer
#define AH_CALL(command)                                          \
    AgentHost* agent_host = (AgentHost*)pt;                       \
    try {                                                         \
        command                                                   \
    } catch (const exception& e) {                                \
        std::string message = std::string("ERROR: ") + e.what();  \
        message += "\n\n" + agent_host->getUsage();               \
        strncpy(err, message.c_str(), AH_ERROR_BUFFER_SIZE);      \
        return 1;                                                 \
    }                                                             \
    return 0;

// external structures
typedef void* ptMissionSpec;
typedef void* ptMissionRecordSpec;
typedef void* ptClientPool;
typedef void* ptWorldState;
typedef void* goptWorldState;

// pointer to AgentHost type
typedef void* ptAgentHost;

// constructor
ptAgentHost new_agent_host();

// destructor
void free_agent_host(ptAgentHost agent_host);

// initialise enums
void agent_host_initialise_enums(
	int *latest_frame_only,
	int *keep_all_frames,

	int *latest_reward_only,
	int *sum_rewards,
	int *keep_all_rewards,

	int *latest_observation_only,
	int *keep_all_observations
);

// All functions return:
//  0 = OK
//  1 = failed; e.g. exception happend => see ERROR_MESSAGE

// methods from ArgumentParser:
int agent_host_parse             (ptAgentHost pt, char* err, int argc, const char** argv);
int agent_host_get_usage         (ptAgentHost pt, char* err, char* usage);
int agent_host_received_argument (ptAgentHost pt, char* err, const char* name, int* response);

// methods from AgentHost:
int agent_host_start_mission                     (ptAgentHost pt, char* err, ptMissionSpec mission, ptClientPool client_pool, ptMissionRecordSpec mission_record, int role, const char* unique_experiment_id);
int agent_host_start_mission_simple              (ptAgentHost pt, char* err, ptMissionSpec mission, ptMissionRecordSpec mission_record);
int agent_host_peek_world_state                  (ptAgentHost pt, char* err, goptWorldState goptws);
int agent_host_get_world_state                   (ptAgentHost pt, char* err, goptWorldState goptws);
int agent_host_get_recording_temporary_directory (ptAgentHost pt, char* err, char* response);
int agent_host_set_debug_output                  (ptAgentHost pt, char* err, int debug);
int agent_host_set_video_policy                  (ptAgentHost pt, char* err, int videoPolicy);
int agent_host_set_rewards_policy                (ptAgentHost pt, char* err, int rewardsPolicy);
int agent_host_set_observations_policy           (ptAgentHost pt, char* err, int observationsPolicy);
int agent_host_send_command                      (ptAgentHost pt, char* err, const char* command);
int agent_host_send_command_turnbased            (ptAgentHost pt, char* err, const char* command, const char* key);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // AGENTHOST_H
