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
using namespace malmo;

// STL:
#include <cstddef>
#include <string>
#include <exception>
using namespace std;

// Local:
#include "go_agenthost.h"

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

int agent_host_received_argument(ptAgentHost pt, char* err, const char* name, int* response) {
    AH_CALL(
        if (agent_host->receivedArgument(name)) {
            *response = 1;
        } else {
            *response = 0;
        }
    )
}

int agent_host_get_usage(ptAgentHost pt, char* err, char* usage) {
    AH_CALL(
        string str_usage = agent_host->getUsage();
        strncpy(usage, str_usage.c_str(), AH_USAGE_MESSAGE_SIZE);
    )
}
