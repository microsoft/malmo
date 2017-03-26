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

// +build ignore

package main

import (
	"fmt"
	"malmo"
	"os"
)

func main() {

	agent_host := malmo.NewAgentHost()
	defer agent_host.Free()

	err := agent_host.Parse(os.Args)
	if err != nil {
		fmt.Println(err)
		return
	}
	if agent_host.ReceivedArgument("help") {
		fmt.Println(agent_host.GetUsage())
		return
	}

	my_mission := malmo.NewMissionSpec()
	my_mission.TimeLimitInSeconds(10)
	my_mission.RequestVideo(320, 240)
	my_mission.RewardForReachingPosition(19.5, 0.0, 19.5, 100.0, 1.1)

	my_mission_record := malmo.NewMissionRecordSpecTarget("./saved_data.tgz")
	my_mission_record.RecordCommands()
	my_mission_record.RecordMP4(20, 400000)
	my_mission_record.RecordRewards()
	my_mission_record.RecordObservations()

	agent_host.StartMissionSimple(my_mission, my_mission_record)
}
