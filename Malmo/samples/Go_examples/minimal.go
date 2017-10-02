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

package main

import (
	"fmt"
	"malmo"
	"math/rand"
	"os"
	"time"
)

func main() {

	// allocate AgentHost
	agent_host := malmo.NewAgentHost()
	defer agent_host.Free()

	// parse input arguments
	err := agent_host.Parse(os.Args)
	if err != nil {
		fmt.Println(err)
		return
	}

	// allocate mission specification
	my_mission := malmo.NewMissionSpec()
	defer my_mission.Free()

	// allocate mission record specification
	my_mission_record := &malmo.MissionRecordSpec{}

	// start mission
	retries := 3
	verbose := true
	err = malmo.StartMissionSimple(retries, agent_host, my_mission, my_mission_record, verbose)
	if err != nil {
		fmt.Println(err)
		return

	}

	// loop until mission ends
	for {
		agent_host.SendCommand("move 1")
		agent_host.SendCommand(fmt.Sprintf("turn %d", rand.Int()*2-1))
		time.Sleep(500 * time.Millisecond)
		world_state := agent_host.GetWorldState()
		if !world_state.IsMissionRunning {
			break
		}
	}
	fmt.Println()
	fmt.Println("Mission has stopped.")
}
