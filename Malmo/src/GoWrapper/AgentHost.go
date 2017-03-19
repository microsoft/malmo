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

package malmo

/*
#cgo CXXFLAGS: -I. -I.. -I../../../Schemas -std=c++11 -Wno-deprecated-declarations
#cgo LDFLAGS: -L../../../build/install/Cpp_Examples/lib -lMalmo -lboost_system -lboost_filesystem -lboost_thread -lboost_iostreams -lboost_program_options -lboost_date_time -lboost_regex -lxerces-c

#include "go_agenthost.h"
#include "stdlib.h"

static inline char** make_argv(int argc) {
	return (char**)malloc(sizeof(char*) * argc);
}

static inline void set_arg(char** argv, int i, char* str) {
	argv[i] = str;
}

static inline char* make_buffer(int size) {
	return (char*)malloc(size * sizeof(char));
}

static inline void free_buffer(char* buf) {
	free(buf);
}
*/
import "C"

import (
	"errors"
	"unsafe"
)

// enums
var (
	// VideoPolicy: specifies what to do when there are more video frames being received than can be processed.
	LATEST_FRAME_ONLY int // Discard all but the most recent frame. This is the default.
	KEEP_ALL_FRAMES   int // Attempt to store all of the frames.

	// RewardsPolicy: specifies what to do when there are more rewards being received than can be processed.
	LATEST_REWARD_ONLY int // Discard all but the most recent reward.
	SUM_REWARDS        int // Add up all the rewards received. This is the default.
	KEEP_ALL_REWARDS   int // Attempt to store all the rewards.

	// ObservationsPolicy: Specifies what to do when there are more observations being received than can be processed.
	LATEST_OBSERVATION_ONLY int // Discard all but the most recent observation. This is the default.
	KEEP_ALL_OBSERVATIONS   int // Attempt to store all the observations.
)

// initialise constants (enums)
func init() {
	C.agent_host_initialise_enums(
		(*C.int)(unsafe.Pointer(&LATEST_FRAME_ONLY)),
		(*C.int)(unsafe.Pointer(&KEEP_ALL_FRAMES)),

		(*C.int)(unsafe.Pointer(&LATEST_REWARD_ONLY)),
		(*C.int)(unsafe.Pointer(&SUM_REWARDS)),
		(*C.int)(unsafe.Pointer(&KEEP_ALL_REWARDS)),

		(*C.int)(unsafe.Pointer(&LATEST_OBSERVATION_ONLY)),
		(*C.int)(unsafe.Pointer(&KEEP_ALL_OBSERVATIONS)),
	)
}

// AgentHost mediates between the researcher's code (the agent) and the Mod (the target environment).
type AgentHost struct {
	pt    C.ptAgentHost // pointer to C.AgentHost
	err   *C.char       // buffer to hold error messages from C++
	usage *C.char       // buffer to hold usage message from C++
}

func NewAgentHost() (o *AgentHost) {
	o = new(AgentHost)
	o.pt = C.new_agent_host()
	if o.pt == nil {
		panic("ERROR: Cannot create new NewAgentHost")
	}
	o.err = C.make_buffer(C.AH_ERROR_MESSAGE_SIZE)
	o.usage = C.make_buffer(C.AH_USAGE_MESSAGE_SIZE)
	return
}

func (o *AgentHost) Free() {
	if o.pt != nil {
		C.free_agent_host(o.pt)
		C.free_buffer(o.err)
		C.free_buffer(o.usage)
	}
}

func (o *AgentHost) Parse(args []string) (err error) {

	// allocate C variables
	argc := C.int(len(args))
	argv := C.make_argv(argc)
	defer C.free(unsafe.Pointer(argv))

	// allocate and set ARGV array
	for i, arg := range args {
		carg := C.CString(arg)
		C.set_arg(argv, C.int(i), carg)
		defer C.free(unsafe.Pointer(carg))
	}

	// call C command
	status := C.agent_host_parse(o.pt, o.err, argc, argv)
	if status != 0 {
		message := C.GoString(o.err)
		return errors.New(message)
	}
	return
}

func (o *AgentHost) ReceivedArgument(name string) bool {

	// allocate C variables
	cname := C.CString(name)
	defer C.free(unsafe.Pointer(cname))
	var response int
	cresponse := (*C.int)(unsafe.Pointer(&response))

	// call C command
	status := C.agent_host_received_argument(o.pt, o.err, cname, cresponse)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}

	// handle return value
	if response == 1 {
		return true
	}
	return false
}

func (o *AgentHost) GetUsage() string {
	status := C.agent_host_get_usage(o.pt, o.err, o.usage)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	usage := C.GoString(o.usage)
	return usage
}

// Starts a mission running. Throws an exception if something goes wrong.
// mission -- The mission specification.
// client_pool -- A list of the Minecraft instances that can be used.
// mission_record -- The specification of the mission recording to make.
// role -- Index of the agent that this agent host is to manage. Zero-based index. Use zero if there is only one agent in this mission.
// unique_experiment_id -- An arbitrary identifier that is used to disambiguate our mission from other runs.
func (o *AgentHost) StartMission(mission *MissionSpec, client_pool *ClientPool, mission_record *MissionRecordSpec, role int, unique_experiment_id string) {
	panic("TODO")
}

// Starts a mission running, in the simple case where there is only one agent running on the local machine. Throws an exception if something goes wrong.
// \param mission The mission specification.
// \param mission_record The specification of the mission recording to make.
func (o *AgentHost) StartMissionSimple(mission *MissionSpec, mission_record *MissionRecordSpec) {
	panic("TODO")
}

// Gets the latest world state received from the game.
// returns The world state.
//WorldState peekWorldState() const;

// Gets the latest world state received from the game and resets it to empty.
// returns The world state.
//WorldState getWorldState();

// Gets the temporary directory being used for the mission record, if recording is taking place.
// returns The temporary directory for the mission record, or an empty string if no recording is going on.
func (o *AgentHost) GetRecordingTemporaryDirectory() string {
	return ""
	panic("TODO")
}

// Switches on/off debug print statements. (Currently just client-pool / agenthost connection messages.)
func (o *AgentHost) SetDebugOutput(debug bool) {
	panic("TODO")
}

// Specifies how you want to deal with multiple video frames.
// videoPolicy -- How you want to deal with multiple video frames coming in asynchronously.
func (o *AgentHost) SetVideoPolicy(videoPolicy int) {
	panic("TODO")
}

// Specifies how you want to deal with multiple rewards.
// rewardsPolicy -- How you want to deal with multiple rewards coming in asynchronously.
func (o *AgentHost) SetRewardsPolicy(rewardsPolicy int) {
	panic("TODO")
}

// Specifies how you want to deal with multiple observations.
// observationsPolicy -- How you want to deal with multiple observations coming in asynchronously.
func (o *AgentHost) SetObservationsPolicy(observationsPolicy int) {
	panic("TODO")
}

// Sends a command to the game client.
// See the mission handlers documentation for the permitted commands for your chosen command handler.
// command -- The command to send as a string. e.g. "move 1"
func (o *AgentHost) SendCommand(command string) {
	panic("TODO")
}

// Sends a turn-based command to the game client.
// See the mission handlers documentation for the permitted commands for your chosen command handler.
// command -- The command to send as a string. e.g. "move 1"
// key -- The command-key (provided via observations) which must match in order for the command to be processed.
func (o *AgentHost) SendCommandTurn(command, key string) {
	panic("TODO")
}
