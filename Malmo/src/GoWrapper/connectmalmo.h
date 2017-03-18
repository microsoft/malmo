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

#ifndef CONNECTMALMO_H
#define CONNECTMALMO_H

#ifdef __cplusplus
extern "C" {
#endif

// MissionSpec --------------------------------------------------------------------------------------

typedef void* ptMissionSpec;

// ptMissionRecordSpec ------------------------------------------------------------------------------

typedef void* ptMissionRecordSpec;

// AgentHost ----------------------------------------------------------------------------------------

typedef void* ptAgentHost;

// constructor and destructor
ptAgentHost new_agent_host();
void free_agent_host(ptAgentHost);

// methods
void agent_host_start_mission_simple(ptAgentHost, ptMissionSpec, ptMissionRecordSpec);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // CONNECTMALMO_H
