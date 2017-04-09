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
