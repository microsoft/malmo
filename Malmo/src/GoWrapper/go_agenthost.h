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

// methods:
int agent_host_parse             (ptAgentHost pt, int argc, const char** argv);
int agent_host_received_argument (ptAgentHost pt, const char* name, int* response);
int agent_host_get_usage         (ptAgentHost pt);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // AGENTHOST_H
