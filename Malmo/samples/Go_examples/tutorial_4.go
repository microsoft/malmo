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

// Tutorial sample #4: Challenge - get to the centre of the sponge

package main

import (
	"fmt"
	"malmo"
	"os"
	"time"
)

func GenCuboid(x1, y1, z1, x2, y2, z2 int, blocktype string) string {
	return fmt.Sprintf(`<DrawCuboid x1="%d" y1="%d" z1="%d" x2="%d" y2="%d" z2="%d" type="%s"/>`, x1, y1, z1, x2, y2, z2, blocktype)
}

func GenCuboidWithVariant(x1, y1, z1, x2, y2, z2 int, blocktype, variant string) string {
	return fmt.Sprintf(`<DrawCuboid x1="%d" y1="%d" z1="%d" x2="%d" y2="%d" z2="%d" type="%s" variant="%s"/>`, x1, y1, z1, x2, y2, z2, blocktype, variant)
}

func Menger(xorg, yorg, zorg, size int, blocktype, variant, holetype string) string {
	//draw solid chunk
	genstring := GenCuboidWithVariant(xorg, yorg, zorg, xorg+size-1, yorg+size-1, zorg+size-1, blocktype, variant) + "\n"
	//now remove holes
	unit := size
	for unit >= 3 {
		w := unit / 3
		for i := 0; i < size; i += unit {
			for j := 0; j < size; j += unit {
				x := xorg + i
				y := yorg + j
				genstring += GenCuboid(x+w, y+w, zorg, (x+2*w)-1, (y+2*w)-1, zorg+size-1, holetype) + "\n"
				y = yorg + i
				z := zorg + j
				genstring += GenCuboid(xorg, y+w, z+w, xorg+size-1, (y+2*w)-1, (z+2*w)-1, holetype) + "\n"
				genstring += GenCuboid(x+w, yorg, z+w, (x+2*w)-1, yorg+size-1, (z+2*w)-1, holetype) + "\n"
			}
		}
		unit /= 3
	}
	return genstring
}

func main() {

	missionXML := `<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
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
			<DrawSphere x="-27" y="70" z="0" radius="30" type="air"/>` + Menger(-40, 40, -13, 27, "stone", "smooth_granite", "air") + `
			<DrawBlock x="-27" y="39" z="0" type="diamond_block"/>
		  </DrawingDecorator>
		  <ServerQuitFromTimeUp timeLimitMs="30000"/>
		  <ServerQuitWhenAnyAgentFinishes/>
		</ServerHandlers>
	  </ServerSection>
	  
	  <AgentSection mode="Survival">
		<Name>MalmoTutorialBot</Name>
		<AgentStart>
			<Placement x="0.5" y="56.0" z="0.5" yaw="90"/>
			<Inventory>
				<InventoryItem slot="8" type="diamond_pickaxe"/>
			</Inventory>
		</AgentStart>
		<AgentHandlers>
		  <ObservationFromFullStats/>
		  <ContinuousMovementCommands turnSpeedDegs="180"/>
		  <InventoryCommands/>
		  <AgentQuitFromReachingPosition>
			<Marker x="-26.5" y="40" z="0.5" tolerance="0.5" description="Goal_found"/>
		  </AgentQuitFromReachingPosition>
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

	// Create Mission specification
	my_mission := malmo.NewMissionSpecXML(missionXML, true)
	defer my_mission.Free()

	// Create Mission Record specification
	my_mission_record := malmo.NewMissionRecordSpec()
	defer my_mission_record.Free()

	// Attempt to start a mission:
	err = agent_host.StartMissionSimple(my_mission, my_mission_record)
	if err != nil {
		fmt.Println(err)
		return

	}

	// Loop until mission starts:
	fmt.Print("Waiting for the mission to start ")
	for {
		fmt.Print(".")
		time.Sleep(100 * time.Millisecond)
		world_state := agent_host.GetWorldState()
		if world_state.HasMissionBegun {
			break
		}
	}

	// ADD YOUR CODE HERE
	// TO GET YOUR AGENT TO THE DIAMOND BLOCK

	// Main Loop:
	fmt.Println()
	fmt.Print("Mission running ")
	for {
		time.Sleep(100 * time.Millisecond)
		world_state := agent_host.GetWorldState()
		if !world_state.IsMissionRunning {
			break
		}
	}
	fmt.Println("Mission ended")
}
