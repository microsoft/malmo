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

	my_mission := malmo.NewMissionSpec()
	my_mission_record := malmo.NewMissionRecordSpecTarget("/tmp/saved_data.tgz")

	err = agent_host.StartMissionSimple(my_mission, my_mission_record)
	if err != nil {
		fmt.Println(err)
		return

	}

	fmt.Println("Waiting for the mission to start")
	for {
		fmt.Print(".")
		time.Sleep(100 * time.Millisecond)
		world_state := agent_host.GetWorldState()
		if world_state.HasMissionBegun {
			break
		}
	}
	fmt.Println()

	for {
		agent_host.SendCommand("move 1")
		agent_host.SendCommand(fmt.Sprintf("turn %d", rand.Int()*2-1))
		time.Sleep(500 * time.Millisecond)
		world_state := agent_host.GetWorldState()
		if !world_state.IsMissionRunning {
			break
		}
	}
	fmt.Println("Mission has stopped.")
}
