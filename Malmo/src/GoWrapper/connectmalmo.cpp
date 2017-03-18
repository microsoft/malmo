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
#include <Mission.h>
#include <AgentHost.h>
using namespace malmo;

// STL:
#include <cstddef>
#include <cstring>
#include <exception>
#include <iostream>
using namespace std;

// Local:
#include "connectmalmo.h"



// MissionSpec --------------------------------------------------------------------------------------

ptMissionSpec new_mission_spec() {
    MissionSpec * pt = new MissionSpec;
    return (void*)pt;
}

void free_mission_spec(ptMissionSpec mission_spec) {
    if (mission_spec != NULL) {
        MissionSpec * pt = (MissionSpec*)mission_spec;
        delete pt;
    }
}

void mission_spec_time_limit_in_seconds(ptMissionSpec mission_spec_in, float s) {
    MissionSpec * mission_spec = (MissionSpec*)mission_spec_in;
    mission_spec->timeLimitInSeconds(s);
}



// AgentHost ----------------------------------------------------------------------------------------

ptAgentHost new_agent_host() {
    AgentHost * pt = new AgentHost;
    return (void*)pt;
}

void free_agent_host(ptAgentHost agent_host) {
    if (agent_host != NULL) {
        AgentHost * pt = (AgentHost*)agent_host;
        delete pt;
    }
}

void make_error_message(const AgentHost* agent_host, const exception& e) {
    string message = string("ERROR: ") + e.what();
    message += "\n\n" + agent_host->getUsage();
    strncpy(ERRORMESSAGE, message.c_str(), ERRMSIZE);
}

long agent_host_parse(ptAgentHost agent_host_in, int argc, const char** argv) {
    AgentHost * agent_host = (AgentHost*)agent_host_in;
    try {
        agent_host->parseArgs(argc, argv);
    } catch(const exception& e) {
        make_error_message(agent_host, e);
        return 1;
    }
    return 0;
}

long agent_host_received_argument(ptAgentHost agent_host_in, const char* name, long* yes_no) {
    AgentHost * agent_host = (AgentHost*)agent_host_in;
    try {
        if (agent_host->receivedArgument(name)) {
            yes_no[0] = 1;
        } else {
            yes_no[0] = 0;
        }
    } catch(const exception& e) {
        make_error_message(agent_host, e);
        return 1;
    }
    return 0;
}

long agent_host_get_usage(ptAgentHost agent_host_in) {
    AgentHost * agent_host = (AgentHost*)agent_host_in;
    try {
        string usage = agent_host->getUsage();
        strncpy(USAGEMESSAGE, usage.c_str(), USGMSIZE);
    } catch(const exception& e) {
        make_error_message(agent_host, e);
        return 1;
    }
    return 0;
}
