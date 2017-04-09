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
	"math"
	"testing"
)

func Test_startmission01(tst *testing.T) {

	// allocate AgentHost
	agent_host := malmo.NewAgentHost()
	defer agent_host.Free()

	// allocate mission specification
	my_mission := malmo.NewMissionSpec()
	defer my_mission.Free()

	// allocate mission record specification
	my_mission_record := &malmo.MissionRecordSpec{}

	// start mission
	retries := 3
	verbose := false
	err := malmo.StartMissionSimple(retries, agent_host, my_mission, my_mission_record, verbose)
	if err != nil {
		tst.Errorf("%v\n", err)
		return
	}
}

func Test_startmission02(tst *testing.T) {

	// allocate agents
	num_agents := 3
	agent_hosts := make([]*malmo.AgentHost, num_agents)
	for i := 0; i < num_agents; i++ {
		agent_hosts[i] = malmo.NewAgentHost()
		defer agent_hosts[i].Free()
	}

	// create mission specification
	xml := createMissionXML(num_agents, 860, 480, true)
	my_mission := malmo.NewMissionSpecXML(xml, true)
	defer my_mission.Free()

	// allocate mission record specification
	my_mission_record := &malmo.MissionRecordSpec{
		RecordMp4:          false,
		RecordObservations: true,
		RecordRewards:      true,
		RecordCommands:     true,
		Mp4BitRate:         400000,
		Mp4Fps:             20,
		Destination:        "data.tgz",
	}

	// set pool of clients
	client_pool := &malmo.ClientPool{}
	for i := 0; i < num_agents; i++ {
		client_pool.Add("127.0.0.1", 10000+i)
	}

	// set experiment unique ID
	experimentID := "Test_startmission01"

	// start mission
	retries := 10
	verbose := false
	err := malmo.StartMission(retries, agent_hosts, my_mission, client_pool, my_mission_record, experimentID, verbose)
	if err != nil {
		tst.Errorf("%v\n", err)
		return
	}
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
