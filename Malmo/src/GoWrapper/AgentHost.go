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
#cgo CXXFLAGS: -std=c++11 -Wno-deprecated-declarations
#cgo LDFLAGS: -lMalmo -lboost_system -lboost_filesystem -lboost_thread -lboost_iostreams -lboost_program_options -lboost_date_time -lboost_regex -lxerces-c

#include "x_auxiliary.h"
#include "x_agent_host.h"
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
	pt     C.ptAgentHost // pointer to C.AgentHost
	err    *C.char       // buffer to hold error messages from C++
	usage  *C.char       // buffer to hold usage message from C++
	recdir *C.char       // buffer to hold recording_directory message from C++
	strarg *C.char       // buffer to hold string argument response from C++
}

// NewAgentHost creates new AgentHost
func NewAgentHost() (o *AgentHost) {
	o = new(AgentHost)
	o.pt = C.new_agent_host()
	if o.pt == nil {
		panic("ERROR: Cannot create new NewAgentHost")
	}
	o.err = C.make_buffer(C.AH_ERROR_BUFFER_SIZE)
	o.usage = C.make_buffer(C.AH_USAGE_BUFFER_SIZE)
	o.recdir = C.make_buffer(C.AH_RECDIR_BUFFER_SIZE)
	o.strarg = C.make_buffer(C.AH_STRING_ARG_SIZE)
	return
}

// Free clears allocated memory
func (o *AgentHost) Free() {
	if o.pt != nil {
		C.free_agent_host(o.pt)
		C.free_buffer(o.err)
		C.free_buffer(o.usage)
		C.free_buffer(o.recdir)
		C.free_buffer(o.strarg)
	}
}

// methods from ArgumentParser ---------------------------------------------------------------------

// Parse parses a list of strings given in the C style. Throws exception if parsing fails.
// param args The arguments to parse.
func (o *AgentHost) Parse(args []string) error {
	argc, argv, free := makeArrayChar(args)
	defer free()
	status := C.agent_host_parse(o.pt, o.err, argc, argv)
	if status != 0 {
		return errors.New(C.GoString(o.err))
	}
	return nil
}

// AddOptionalIntArgument specifies an integer argument that can be given on the command line.
// name -- The name of the argument. To be given as "--name <value>"
// description -- The explanation of the argument that can be printed out.
// defaultValue -- The value that this argument should have if not given on the command line.
func (o *AgentHost) AddOptionalIntArgument(name, description string, defaultValue int) {
	cname, cdesc, cvalue, free := make2stringsInt(name, description, defaultValue)
	defer free()
	status := C.agent_host_add_optional_int_argument(o.pt, o.err, cname, cdesc, cvalue)
	if status != 0 {
		panic("ERROR:\n" + C.GoString(o.err))
	}
}

// AddOptionalFloatArgument specifies a floating-point argument that can be given on the command line.
// name -- The name of the argument. To be given as "--name <value>"
// description -- The explanation of the argument that can be printed out.
// defaultValue -- The value that this argument should have if not given on the command line.
func (o *AgentHost) AddOptionalFloatArgument(name, description string, defaultValue float64) {
	cname, cdesc, cvalue, free := make2stringsFloat(name, description, defaultValue)
	defer free()
	status := C.agent_host_add_optional_float_argument(o.pt, o.err, cname, cdesc, cvalue)
	if status != 0 {
		panic("ERROR:\n" + C.GoString(o.err))
	}
}

// AddOptionalStringArgument specifies a string argument that can be given on the command line.
// name -- The name of the argument. To be given as "--name <value>"
// description -- The explanation of the argument that can be printed out.
// defaultValue -- The value that this argument should have if not given on the command line.
func (o *AgentHost) AddOptionalStringArgument(name, description, defaultValue string) {
	cname, cdesc, cvalue, free := make3strings(name, description, defaultValue)
	defer free()
	status := C.agent_host_add_optional_string_argument(o.pt, o.err, cname, cdesc, cvalue)
	if status != 0 {
		panic("ERROR:\n" + C.GoString(o.err))
	}
}

// AddOptionalFlag specifies a boolean flag that can be given on the command line.
// name -- The name of the flag. To be given as "--name"
// description -- The explanation of the flag that can be printed out.
func (o *AgentHost) AddOptionalFlag(name, description string) {
	cname, cdesc, free := make2strings(name, description)
	defer free()
	status := C.agent_host_add_optional_flag(o.pt, o.err, cname, cdesc)
	if status != 0 {
		panic("ERROR:\n" + C.GoString(o.err))
	}
}

// GetUsage gets a string that describes the current set of options we expect.
// returns The usage string, for displaying.
func (o *AgentHost) GetUsage() string {
	status := C.agent_host_get_usage(o.pt, o.err, o.usage)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	return C.GoString(o.usage)
}

// ReceivedArgument gets whether a named argument was parsed on the command-line arguments.
// name -- The name of the argument.
// returns True if the named argument was received.
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

// GetIntArgument retrieves the value of a named integer argument.
// name -- The name of the argument.
// returns The value of the named argument.
func (o *AgentHost) GetIntArgument(name string) (response int) {
	cname := C.CString(name)
	defer C.free(unsafe.Pointer(cname))
	cresponse := (*C.int)(unsafe.Pointer(&response))
	status := C.agent_host_get_int_argument(o.pt, o.err, cname, cresponse)
	if status != 0 {
		panic("ERROR:\n" + C.GoString(o.err))
	}
	return
}

// GetFloatArgument retrieves the value of a named floating-point argument.
// name -- The name of the argument.
// returns The value of the named argument.
func (o *AgentHost) GetFloatArgument(name string) (response float64) {
	cname := C.CString(name)
	defer C.free(unsafe.Pointer(cname))
	cresponse := (*C.double)(unsafe.Pointer(&response))
	status := C.agent_host_get_float_argument(o.pt, o.err, cname, cresponse)
	if status != 0 {
		panic("ERROR:\n" + C.GoString(o.err))
	}
	return
}

// GetStringArgument retrieves the value of a named string argument.
// name -- The name of the argument.
// returns The value of the named argument.
func (o *AgentHost) GetStringArgument(name string) string {
	cname := C.CString(name)
	defer C.free(unsafe.Pointer(cname))
	status := C.agent_host_get_string_argument(o.pt, o.err, cname, o.strarg)
	if status != 0 {
		panic("ERROR:\n" + C.GoString(o.err))
	}
	return C.GoString(o.strarg)
}

// methods from AgentHost --------------------------------------------------------------------------

// Starts a mission running. Throws an exception if something goes wrong.
// mission -- The mission specification.
// client_pool -- A list of the Minecraft instances that can be used.
// mission_record -- The specification of the mission recording to make.
// role -- Index of the agent that this agent host is to manage. Zero-based index. Use zero if there is only one agent in this mission.
// unique_experiment_id -- An arbitrary identifier that is used to disambiguate our mission from other runs.
func (o *AgentHost) StartMission(mission *MissionSpec, client_pool *ClientPool, mission_record *MissionRecordSpec, role int, unique_experiment_id string) error {

	// ClientPool: allocate C variables
	cp, err, freeCP := client_pool.toC()
	if err != nil {
		return err
	}
	defer freeCP()

	// MissionRecordSpec: allocate C variables
	mrs, freeMRS := mission_record.toC()
	defer freeMRS()

	// Id: allocate C variable
	cid := C.CString(unique_experiment_id)
	defer C.free(unsafe.Pointer(cid))

	// call C++ code
	status := C.agent_host_start_mission(o.pt, o.err, mission.pt, cp, mrs, C.int(role), cid)
	if status != 0 {
		return errors.New(C.GoString(o.err))
	}
	return nil
}

// Starts a mission running, in the simple case where there is only one agent running on the local machine. Throws an exception if something goes wrong.
// mission -- The mission specification.
// mission_record -- The specification of the mission recording to make.
func (o *AgentHost) StartMissionSimple(mission *MissionSpec, mission_record *MissionRecordSpec) error {

	// MissionRecordSpec: allocate C variables
	mrs, freeMRS := mission_record.toC()
	defer freeMRS()

	// call C++ code
	status := C.agent_host_start_mission_simple(o.pt, o.err, mission.pt, mrs)
	if status != 0 {
		return errors.New(C.GoString(o.err))
	}
	return nil
}

// Gets the latest world state received from the game.
// returns The world state.
func (o AgentHost) PeekWorldState() (ws WorldState) {
	status := C.agent_host_peek_world_state(o.pt, o.err, (C.goptWorldState)(unsafe.Pointer(&ws)))
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	return
}

// Gets the latest world state received from the game and resets it to empty.
// returns The world state.
func (o AgentHost) GetWorldState() (ws WorldState) {
	status := C.agent_host_get_world_state(o.pt, o.err, (C.goptWorldState)(unsafe.Pointer(&ws)))
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	return
}

// Gets the temporary directory being used for the mission record, if recording is taking place.
// returns The temporary directory for the mission record, or an empty string if no recording is going on.
func (o *AgentHost) GetRecordingTemporaryDirectory() string {
	status := C.agent_host_get_recording_temporary_directory(o.pt, o.err, o.recdir)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	return C.GoString(o.recdir)
}

// Switches on/off debug print statements. (Currently just client-pool / agenthost connection messages.)
func (o *AgentHost) SetDebugOutput(debug bool) {
	var cdebug C.int
	if debug {
		cdebug = 1
	} else {
		cdebug = 0
	}
	status := C.agent_host_set_debug_output(o.pt, o.err, cdebug)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

// Specifies how you want to deal with multiple video frames.
// videoPolicy -- How you want to deal with multiple video frames coming in asynchronously.
func (o *AgentHost) SetVideoPolicy(videoPolicy int) {
	status := C.agent_host_set_video_policy(o.pt, o.err, (C.int)(videoPolicy))
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

// Specifies how you want to deal with multiple rewards.
// rewardsPolicy -- How you want to deal with multiple rewards coming in asynchronously.
func (o *AgentHost) SetRewardsPolicy(rewardsPolicy int) {
	status := C.agent_host_set_rewards_policy(o.pt, o.err, (C.int)(rewardsPolicy))
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

// Specifies how you want to deal with multiple observations.
// observationsPolicy -- How you want to deal with multiple observations coming in asynchronously.
func (o *AgentHost) SetObservationsPolicy(observationsPolicy int) {
	status := C.agent_host_set_observations_policy(o.pt, o.err, (C.int)(observationsPolicy))
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

// Sends a command to the game client.
// See the mission handlers documentation for the permitted commands for your chosen command handler.
// command -- The command to send as a string. e.g. "move 1"
func (o *AgentHost) SendCommand(command string) error {
	ccommand := C.CString(command)
	defer C.free(unsafe.Pointer(ccommand))
	status := C.agent_host_send_command(o.pt, o.err, ccommand)
	if status != 0 {
		return errors.New(C.GoString(o.err))
	}
	return nil
}

// Sends a turn-based command to the game client.
// See the mission handlers documentation for the permitted commands for your chosen command handler.
// command -- The command to send as a string. e.g. "move 1"
// key -- The command-key (provided via observations) which must match in order for the command to be processed.
func (o *AgentHost) SendCommandTurnBased(command, key string) error {
	ccommand := C.CString(command)
	defer C.free(unsafe.Pointer(ccommand))
	ckey := C.CString(key)
	defer C.free(unsafe.Pointer(ckey))
	status := C.agent_host_send_command_turnbased(o.pt, o.err, ccommand, ckey)
	if status != 0 {
		return errors.New(C.GoString(o.err))
	}
	return nil
}
