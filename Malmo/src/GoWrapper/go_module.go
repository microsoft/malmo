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
#include "connectmalmo.h"
#include "stdlib.h"
static inline char** make_argv(int argc) {
	return (char**)malloc(sizeof(char*) * argc);
}
static inline void set_arg(char** argv, int i, char* str) {
	argv[i] = str;
}
#ifdef WIN32
#define LONG long long
#else
#define LONG long
#endif
*/
import "C"

import (
	"errors"
	"unsafe"
)

// AgentHost mediates between the researcher's code (the agent) and the Mod (the target environment).
type AgentHost struct {
	agent_host C.ptAgentHost // pointer to C.AgentHost
}

func NewAgentHost() (o *AgentHost) {
	o = new(AgentHost)
	o.agent_host = C.new_agent_host()
	return
}

func (o *AgentHost) Free() {
	if o.agent_host != nil {
		C.free_agent_host(o.agent_host)
	}
}

func (o *AgentHost) Parse(args []string) (err error) {
	argc := C.int(len(args))
	argv := C.make_argv(argc)
	defer C.free(unsafe.Pointer(argv))
	for i, arg := range args {
		carg := C.CString(arg)
		C.set_arg(argv, C.int(i), carg)
		defer C.free(unsafe.Pointer(carg))
	}
	status := C.agent_host_parse(o.agent_host, argc, argv)
	if status != 0 {
		message := C.GoString(&C.ERRORMESSAGE[0])
		return errors.New(message)
	}
	return
}

func (o *AgentHost) ReceivedArgument(name string) bool {
	cname := C.CString(name)
	defer C.free(unsafe.Pointer(cname))
	yes_no := []int{0}
	cyes_no := (*C.LONG)(unsafe.Pointer(&yes_no[0]))
	status := C.agent_host_received_argument(o.agent_host, cname, cyes_no)
	if status != 0 {
		message := C.GoString(&C.ERRORMESSAGE[0])
		panic("ERROR:\n" + message)
	}
	if yes_no[0] == 1 {
		return true
	}
	return false
}

func (o *AgentHost) GetUsage() string {
	status := C.agent_host_get_usage(o.agent_host)
	if status != 0 {
		message := C.GoString(&C.ERRORMESSAGE[0])
		panic("ERROR:\n" + message)
	}
	usage := C.GoString(&C.USAGEMESSAGE[0])
	return usage
}
