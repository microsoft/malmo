// ------------------------------------------------------------------------------------------------
// Copyright (c) 2016 Microsoft Corporation
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// ------------------------------------------------------------------------------------------------

// Tutorial sample //7: The Maze Decorator

package main

import (
	"fmt"
	"malmo"
	"os"
	"time"
)

func GetMissionXML(seed string, gp float32) string {
	return `<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
	<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	
	  <About>
		<Summary>Hello world!</Summary>
	  </About>
	  
	<ServerSection>
	  <ServerInitialConditions>
		<Time>
			<StartTime>1000</StartTime>
			<AllowPassageOfTime>false</AllowPassageOfTime>
		</Time>
		<Weather>clear</Weather>
	  </ServerInitialConditions>
	  <ServerHandlers>
		  <FlatWorldGenerator generatorString="3;7,44*49,73,35:1,159:4,95:13,35:13,159:11,95:10,159:14,159:6,35:6,95:6;12;"/>
		  <DrawingDecorator>
			<DrawSphere x="-27" y="70" z="0" radius="30" type="air"/>
		  </DrawingDecorator>
		  <MazeDecorator>
			<Seed>` + seed + `</Seed>
			<SizeAndPosition width="10" length="10" height="10" xOrigin="-32" yOrigin="69" zOrigin="-5"/>
			<StartBlock type="emerald_block" fixedToEdge="true"/>
			<EndBlock type="redstone_block" fixedToEdge="true"/>
			<PathBlock type="diamond_block"/>
			<FloorBlock type="air"/>
			<GapBlock type="air"/>
			<GapProbability>` + fmt.Sprintf("%g", gp) + `</GapProbability>
			<AllowDiagonalMovement>false</AllowDiagonalMovement>
		  </MazeDecorator>
		  <ServerQuitFromTimeUp timeLimitMs="30000"/>
		  <ServerQuitWhenAnyAgentFinishes/>
		</ServerHandlers>
	  </ServerSection>
	  
	  <AgentSection mode="Survival">
		<Name>MalmoTutorialBot</Name>
		<AgentStart>
			<Placement x="0.5" y="56.0" z="0.5"/>
		</AgentStart>
		<AgentHandlers>
			<AgentQuitFromTouchingBlockType>
				<Block type="redstone_block"/>
			</AgentQuitFromTouchingBlockType>
		</AgentHandlers>
	  </AgentSection>
	</Mission>`
}

func main() {

	// Create AgentHost
	agent_host := malmo.NewAgentHost()
	defer agent_host.Free()

	// Parse input
	err := agent_host.Parse(os.Args)
	if err != nil {
		fmt.Println(err)
		return
	}
	num_repeats := 10
	if agent_host.ReceivedArgument("test") {
		num_repeats = 1
	}

	for i := 0; i < num_repeats; i++ {

		my_mission := malmo.NewMissionSpecXML(GetMissionXML("random", float32(i)/10.0), true)
		defer my_mission.Free()

		my_mission_record := malmo.NewMissionRecordSpec()
		defer my_mission_record.Free()

		// Attempt to start a mission:
		max_retries := 3
		for retry := 0; retry < max_retries; retry++ {
			err = agent_host.StartMissionSimple(my_mission, my_mission_record)
			if err == nil {
				break
			}
			if retry == max_retries-1 {
				fmt.Printf("Error starting mission: %v\n", err)
				os.Exit(1)
			}
			time.Sleep(2000 * time.Millisecond)
			fmt.Println("retry ", retry)
		}

		// Loop until mission starts:
		fmt.Print("Waiting for the mission to start")
		world_state := agent_host.GetWorldState()
		for !world_state.HasMissionBegun {
			fmt.Print(".")
			time.Sleep(100 * time.Millisecond)
			world_state = agent_host.GetWorldState()
			for _, e := range world_state.Errors {
				fmt.Printf("Error: %v\n", e.Text)
			}
		}
		fmt.Println()
		fmt.Print("Mission running ")

		// Loop until mission ends:
		for world_state.IsMissionRunning {
			fmt.Print(".")
			time.Sleep(100 * time.Millisecond)
			world_state = agent_host.GetWorldState()
			for _, e := range world_state.Errors {
				fmt.Printf("Error: %v\n", e.Text)
			}

		}

		fmt.Println()
		fmt.Println("Mission ended")
	}
}
