package malmo

/*
#cgo CXXFLAGS: -I. -I.. -I../../../Schemas -std=c++11 -Wno-deprecated-declarations
#cgo LDFLAGS: -L. -lMalmo -lboost_system -lboost_filesystem -lboost_thread -lboost_iostreams -lboost_program_options -lboost_date_time -lboost_regex -lxerces-c
#include "connectmalmo.h"
*/
import "C"

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
