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

func main() {

	missionXML := `<?xml version="1.0" encoding="UTF-8" ?>
	<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	  <About>
		<Summary>Find the goal!</Summary>
	  </About>

	  <ServerSection>
		<ServerInitialConditions>
		  <Time>
			<StartTime>14000</StartTime>
			<AllowPassageOfTime>false</AllowPassageOfTime>
		  </Time>
		</ServerInitialConditions>
		<ServerHandlers>
		  <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
		  <ClassroomDecorator>
			<complexity>
			  <building>0.5</building>
			  <path>0.5</path>
			  <division>1</division>
			  <obstacle>1</obstacle>
			  <hint>0</hint>
			</complexity>
		  </ClassroomDecorator>
		  <ServerQuitFromTimeUp timeLimitMs="30000" description="out_of_time"/>
		  <ServerQuitWhenAnyAgentFinishes />
		</ServerHandlers>
	  </ServerSection>

	  <AgentSection mode="Survival">
		<Name>James Bond</Name>
		<AgentStart>
		  <Placement x="-203.5" y="81.0" z="217.5"/>
		</AgentStart>
		<AgentHandlers>
		  <ObservationFromFullStats />
		  <ContinuousMovementCommands turnSpeedDegs="180">
			<ModifierList type="deny-list">
			  <command>attack</command>
			</ModifierList>
		  </ContinuousMovementCommands>
		  <RewardForMissionEnd rewardForDeath="-10000">
			<Reward description="found_goal" reward="1000" />
			<Reward description="out_of_time" reward="-1000" />
		  </RewardForMissionEnd>
		  <RewardForTouchingBlockType>
			<Block type="gold_ore diamond_ore redstone_ore" reward="20" />
		  </RewardForTouchingBlockType>
		  <AgentQuitFromTouchingBlockType>
			<Block type="gold_block diamond_block redstone_block" description="found_goal" />
		  </AgentQuitFromTouchingBlockType>
		</AgentHandlers>
	  </AgentSection>
	</Mission>`

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

		my_mission := malmo.NewMissionSpecXML(missionXML, true)
		defer my_mission.Free()

		my_mission_record := &malmo.MissionRecordSpec{}

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
