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
	"math/rand"
	"os"
	"time"
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

	my_mission_record := malmo.NewMissionRecordSpecTarget("/tmp/saved_data.tgz")
	my_mission_record.RecordCommands()
	my_mission_record.RecordMP4(20, 400000)
	my_mission_record.RecordRewards()
	my_mission_record.RecordObservations()

	// TODO: fix this: error with "permission to write there"
	//destination := "/tmp/test_malmo"
	//os.MkdirAll(destination, 0777)
	//my_mission_record.SetDestination(destination)

	err = agent_host.StartMissionSimple(my_mission, my_mission_record)
	if err != nil {
		fmt.Println(err)
		return
	}

	//fmt.Printf("recording directory = %q\n", agent_host.GetRecordingTemporaryDirectory())

	fmt.Println("Waiting for the mission to start")
	for {
		fmt.Print(".")
		time.Sleep(100 * time.Millisecond)
		world_state := agent_host.GetWorldState()
		for _, e := range world_state.Errors {
			fmt.Printf("Error: %v\n", e.Text)
		}
		if world_state.HasMissionBegun {
			break
		}
	}
	fmt.Println()

	// main loop:
	for {
		agent_host.SendCommand("move 1")
		agent_host.SendCommand(fmt.Sprintf("turn %d", rand.Int()*2-1))

		time.Sleep(500 * time.Millisecond)
		world_state := agent_host.GetWorldState()

		fmt.Printf("num_video=%d, num_observations=%d, num_rewards=%d\n",
			world_state.NumberOfVideoFramesSinceLastState,
			world_state.NumberOfObservationsSinceLastState,
			world_state.NumberOfRewardsSinceLastState)

		for _, reward := range world_state.Rewards {
			fmt.Printf("  Summed reward: %g\n", reward.GetValue())
		}

		for _, the_error := range world_state.Errors {
			fmt.Printf("  Error: %s\n", the_error.Text)
		}

		for _, frame := range world_state.VideoFrames {
			fmt.Printf("  Frame: %v x %v : %v channels\n", frame.Width, frame.Height, frame.Channels)
		}

		if !world_state.IsMissionRunning {
			break
		}
	}

	fmt.Println("Mission has stopped.")
}
