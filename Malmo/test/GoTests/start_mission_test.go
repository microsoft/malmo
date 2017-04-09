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
	"errors"
	"fmt"
	"malmo"
	"math"
	"testing"
	"time"
)

func Test_startmission01(tst *testing.T) {

	num_agents := 3
	agent_hosts := make([]*malmo.AgentHost, num_agents)
	for i := 0; i < num_agents; i++ {
		agent_hosts[i] = malmo.NewAgentHost()
		defer agent_hosts[i].Free()
	}

	xml := createMissionXML(num_agents, 860, 480, true)
	my_mission := malmo.NewMissionSpecXML(xml, true)
	defer my_mission.Free()

	my_mission_record := malmo.NewMissionRecordSpec()
	defer my_mission_record.Free()

	client_pool := &malmo.ClientPool{}
	for i := 0; i < num_agents; i++ {
		client_pool.Add("127.0.0.1", 10000+i)
	}

	experimentID := "Test_startmission01"

	for i := 0; i < num_agents; i++ {
		err := startMission(agent_hosts[i], my_mission, client_pool, my_mission_record, i, experimentID)
		if err != nil {
			tst.Errorf("%v\n", err)
			return
		}
	}

	fmt.Printf("Waiting for the mission to start ")
	hasBegun := false
	hadErrors := false
	for !hasBegun && !hadErrors {
		fmt.Printf(".")
		time.Sleep(200 * time.Millisecond)
		hasBegun = true
		for i, ah := range agent_hosts {
			world_state := ah.GetWorldState()
			if !world_state.HasMissionBegun {
				hasBegun = false
			}
			if len(world_state.Errors) > 0 {
				hadErrors = true
				fmt.Printf("Errors from agent # %d\n", i)
				for _, e := range world_state.Errors {
					fmt.Printf("Error: %v\n", e.Text)
				}
			}
		}
	}
	fmt.Println()

	if !hasBegun {
		tst.Errorf("mission failed to begin")
		return
	}

	if hadErrors {
		tst.Errorf("hadErrors flag should be false at this point")
		return
	}
}

func startMission(ah *malmo.AgentHost, m *malmo.MissionSpec, cp *malmo.ClientPool, mr *malmo.MissionRecordSpec, role int, id string) (err error) {
	max_retries := 10
	for retry := 0; retry < max_retries; retry++ {
		e := ah.StartMission(m, cp, mr, role, id)
		if e == nil {
			break
		}
		if retry == max_retries-1 {
			err = errors.New(fmt.Sprintf("Error starting mission:\n%v\nIs the game running?\n", e))
			return
		}
		time.Sleep(1000 * time.Millisecond)
	}
	return
}

func getPlacementString(i, num_agents int) string {
	// Place agents at equal points around a circle, facing inwards.
	radius := 5.0
	accuracy := 1000.0
	angle := 2 * float64(i) * math.Pi / float64(num_agents)
	x := int(accuracy*radius*math.Cos(angle)) / int(accuracy)
	y := 227
	z := int(accuracy*radius*math.Sin(angle)) / int(accuracy)
	pitch := 0
	yaw := int(90 + angle*180.0/math.Pi)
	return fmt.Sprintf("x=\"%d\" y=\"%d\" z=\"%d\" pitch=\"%d\" yaw=\"%d\"", x, y, z, pitch, yaw)
}

func createMissionXML(num_agents, width, height int, reset bool) (xml string) {
	// Set up the Mission XML.
	// First, the server section.
	// Weather MUST be set to clear, since the dark thundery sky plays havoc with the image thresholding.
	xml = `<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <About>
        <Summary/>
      </About>
      <ServerSection>
        <ServerInitialConditions>
          <Time>
            <StartTime>1000</StartTime>
          </Time>
          <Weather>clear</Weather>
        </ServerInitialConditions>
        <ServerHandlers>
          <FlatWorldGenerator forceReset="` + fmt.Sprintf("%v", reset) + `" generatorString="3;2*4,225*22;1;" seed=""/>
          <ServerQuitFromTimeUp description="" timeLimitMs="10000"/>
        </ServerHandlers>
      </ServerSection>
    `

	// Add an agent section for each watcher.
	// We put them in a leather helmet because it makes the image processing slightly easier.
	for i := 0; i < num_agents; i++ {
		placement := getPlacementString(i, num_agents)
		xml += `<AgentSection mode="Survival">
        <Name>Watcher#` + fmt.Sprintf("%d", i) + `</Name>
        <AgentStart>
          <Placement ` + placement + `/>
          <Inventory>
            <InventoryObject type="leather_helmet" slot="39" quantity="1"/>
          </Inventory>
        </AgentStart>
        <AgentHandlers>
          <VideoProducer>
            <Width>` + fmt.Sprintf("%d", width) + `</Width>
            <Height>` + fmt.Sprintf("%d", height) + `</Height>
          </VideoProducer>
        </AgentHandlers>
      </AgentSection>`
	}

	xml += "</Mission>"
	return
}
