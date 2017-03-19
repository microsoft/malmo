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
#include <cstring>
#include <exception>
using namespace std;

// Local:
#include "go_messages.h"
#include "go_agenthost.h"

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
    strncpy(ERROR_MESSAGE, message.c_str(), ERROR_MESSAGE_SIZE);
}

#define AGENT_HOST_CALL(command)              \
    AgentHost * agent_host = (AgentHost*)pt;  \
    try {                                     \
        command                               \
    } catch (const exception& e) {            \
        MAKE_ERROR_MESSAGE_AH(e, agent_host)  \
        return 1;                             \
    }                                         \
    return 0;

long agent_host_parse(ptAgentHost pt, int argc, const char** argv) {
    AGENT_HOST_CALL(
        agent_host->parseArgs(argc, argv);
    )
}

long agent_host_received_argument(ptAgentHost pt, const char* name, long* yes_no) {
    AGENT_HOST_CALL(
        if (agent_host->receivedArgument(name)) {
            yes_no[0] = 1;
        } else {
            yes_no[0] = 0;
        }
    )
}

long agent_host_get_usage(ptAgentHost pt) {
    AGENT_HOST_CALL(
        string usage = agent_host->getUsage();
        strncpy(USAGE_MESSAGE, usage.c_str(), USAGE_MESSAGE_SIZE);
    )
}
