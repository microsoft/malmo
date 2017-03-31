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

// Tutorial sample #6: Discrete movement, rewards, and learning

// The "Cliff Walking" example using Q-learning.
// From pages 148-150 of:
// Richard S. Sutton and Andrews G. Barto
// Reinforcement Learning, An Introduction
// MIT Press, 1998

package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"malmo"
	"math/rand"
	"os"
	"time"
)

// Tabular Q-learning agent for discrete state/action spaces
type TabQAgent struct {
	epsilon float32  // chance of taking a random action instead of the best
	logger  Logger   // the logger (see end of this file)
	actions []string // allowed actions

	// the Q table; e.g. "4:1" => [0, 0, -1.0, -101.0]
	q_table map[string][]float32

	prev_s string // previous state; e.g. "4:1"
	prev_a int    // previous action index in [0,3]
}

// NewTabQAgent creates a new TabQAgent
func NewTabQAgent(debugging bool) (o *TabQAgent) {
	o = new(TabQAgent)
	o.epsilon = 0.01
	o.logger.Debugging = debugging
	o.actions = []string{"movenorth 1", "movesouth 1", "movewest 1", "moveeast 1"}
	o.q_table = make(map[string][]float32)
	return
}

// UpdateQTable changes q_table to reflect what we have learnt
func (o *TabQAgent) UpdateQTable(reward float32) {

	// retrieve the old action value from the Q-table (indexed by the previous state and the previous action)
	old_q := o.q_table[o.prev_s][o.prev_a]
	_ = old_q // NOTE: this line can be removed if old_q is not being used

	// TODO: what should the new action value be?
	new_q := reward

	// assign the new action value to the Q-table
	o.q_table[o.prev_s][o.prev_a] = new_q
}

// UpdateQTableFromTerminatingState changes q_table to reflect what we have learnt, after reaching a terminal state
func (o *TabQAgent) UpdateQTableFromTerminatingState(reward float32) {

	// retrieve the old action value from the Q-table (indexed by the previous state and the previous action)
	old_q := o.q_table[o.prev_s][o.prev_a]
	_ = old_q // NOTE: this line can be removed if old_q is not being used

	// TODO: what should the new action value be?
	new_q := reward

	// assign the new action value to the Q-table
	o.q_table[o.prev_s][o.prev_a] = new_q
}

// Act takes 1 action in response to the current world state
func (o *TabQAgent) Act(world_state *malmo.WorldState, agent_host *malmo.AgentHost, current_r float32) float32 {

	sz := len(world_state.Observations)
	obs_text := world_state.Observations[sz-1].Text
	obs, err := parseJsonObs(obs_text) // most recent observation
	o.logger.Debug("%+v\n", obs)
	if err != nil {
		o.logger.Error("Incomplete observation received: %s\n", obs_text)
		return 0
	}
	current_s := fmt.Sprintf("%d:%d", int(obs.XPos), int(obs.ZPos))
	o.logger.Debug("State: %s (x = %.2f, z = %.2f)\n", current_s, float32(obs.XPos), float32(obs.ZPos))
	if _, has_key := o.q_table[current_s]; !has_key {
		o.q_table[current_s] = make([]float32, len(o.actions))
	}

	// update Q values
	if o.prev_s != "" {
		o.UpdateQTable(current_r)
	}

	//o.DrawQ(int(obs.XPos), int(obs.ZPos)) TODO

	// select the next action
	var a int
	rnd := rand.Float32()
	if rnd < o.epsilon {
		a := rand.Intn(len(o.actions) - 1)
		o.logger.Info("Random action: %s\n", o.actions[a])
	} else {
		m := maxFloat(o.q_table[current_s])
		o.logger.Debug("Current values: %v\n", o.q_table[current_s])
		var l []int
		for j := 0; j < len(o.actions); j++ {
			if o.q_table[current_s][j] == m {
				l = append(l, j)
			}
		}
		var k int
		if len(l) > 1 {
			k = rand.Intn(len(l) - 1)
		}
		a = l[k]
		o.logger.Info("Taking q action: %s\n", o.actions[a])
	}

	// try to send the selected action, only update prev_s if this succeeds
	if err := agent_host.SendCommand(o.actions[a]); err != nil {
		o.logger.Error("Failed to send command: %v\n", err)
	} else {
		o.prev_s = current_s
		o.prev_a = a
	}

	return current_r
}

// Run runs the agent on the world
func (o *TabQAgent) Run(agent_host *malmo.AgentHost) float32 {

	var current_r, total_reward float32

	o.prev_s = ""

	is_first_action := true

	// main loop:
	world_state := agent_host.GetWorldState()
	for world_state.IsMissionRunning {

		current_r = 0.0

		if is_first_action {
			// wait until have received a valid observation
			for {
				time.Sleep(100 * time.Millisecond)
				world_state = agent_host.GetWorldState()
				for _, e := range world_state.Errors {
					o.logger.Error("Error: %s", e.Text)
				}
				for _, reward := range world_state.Rewards {
					current_r += float32(reward.GetValue())
				}
				sz := len(world_state.Observations)
				if world_state.IsMissionRunning && sz > 0 && world_state.Observations[sz-1].Text != "{}" {
					total_reward += o.Act(&world_state, agent_host, current_r)
					break
				}
				if !world_state.IsMissionRunning {
					break
				}
			}
			is_first_action = false
		} else {
			// wait for non-zero reward
			for world_state.IsMissionRunning && current_r == 0.0 {
				time.Sleep(100 * time.Millisecond)
				world_state = agent_host.GetWorldState()
				for _, e := range world_state.Errors {
					o.logger.Error("Error: %s", e.Text)
				}
				for _, reward := range world_state.Rewards {
					current_r += float32(reward.GetValue())
				}
			}
			// allow time to stabilise after action
			for {
				time.Sleep(100 * time.Millisecond)
				world_state = agent_host.GetWorldState()
				for _, e := range world_state.Errors {
					o.logger.Error("Error: %s", e.Text)
				}
				for _, reward := range world_state.Rewards {
					current_r += float32(reward.GetValue())
				}
				sz := len(world_state.Observations)
				if world_state.IsMissionRunning && sz > 0 && world_state.Observations[sz-1].Text != "{}" {
					total_reward += o.Act(&world_state, agent_host, current_r)
					break
				}
				if !world_state.IsMissionRunning {
					break
				}
			}
		}
	}

	// process final reward
	o.logger.Debug("Final reward: %v\n", current_r)
	total_reward += current_r

	// update Q values
	if o.prev_s != "" {
		o.UpdateQTableFromTerminatingState(current_r)
	}

	//o.DrawQ() TODO

	return total_reward
}

// --- main ---------------------------------------------------------------------------------------

func main() {

	// Create Agent
	debugging := false
	agent := NewTabQAgent(debugging)
	agent_host := malmo.NewAgentHost()
	defer agent_host.Free()

	// Parse input
	err := agent_host.Parse(os.Args)
	if err != nil {
		fmt.Println(err)
		return
	}

	// -- set up the mission --
	mission_file := "./tutorial_6.xml"
	dat, err := ioutil.ReadFile(mission_file)
	if err != nil {
		fmt.Printf("cannot read mission file: %v\n", err)
		return
	}
	my_mission := malmo.NewMissionSpecXML(string(dat), true)
	defer my_mission.Free()

	max_retries := 3

	num_repeats := 150
	if agent_host.ReceivedArgument("test") {
		num_repeats = 1
	}

	var cumulative_rewards []float32
	for i := 0; i < num_repeats; i++ {

		fmt.Println()
		fmt.Printf("Repeat %d of %d\n", i+1, num_repeats)

		my_mission_record := malmo.NewMissionRecordSpec()
		defer my_mission_record.Free()

		for retry := 0; retry < max_retries; retry++ {
			err = agent_host.StartMissionSimple(my_mission, my_mission_record)
			if err == nil {
				break
			}
			if retry == max_retries-1 {
				fmt.Printf("Error starting mission: %v\n", err)
				os.Exit(1)
			}
			time.Sleep(2500 * time.Millisecond)
			fmt.Println("retry ", retry)
		}

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

		// -- run the agent in the world --
		cumulative_reward := agent.Run(agent_host)
		fmt.Printf("Cumulative reward: %v\n", cumulative_reward)
		cumulative_rewards = append(cumulative_rewards, cumulative_reward)

		// -- clean up --
		time.Sleep(500 * time.Millisecond) // (let the Mod reset)
	}

	fmt.Println("Done.")

	fmt.Println()
	fmt.Printf("Cumulative rewards for all %d runs:", num_repeats)
	fmt.Println(cumulative_rewards)
}

// --- Auxiliary ----------------------------------------------------------------------------------

func maxFloat(numbers []float32) (res float32) {
	res = numbers[0]
	for i := 1; i < len(numbers); i++ {
		if numbers[i] > res {
			res = numbers[i]
		}
	}
	return
}

// --- Observation --------------------------------------------------------------------------------

type Observation struct {
	Floor3x3 []string
	XPos     float32
	YPos     float32
	ZPos     float32
}

func parseJsonObs(strObs string) (o Observation, err error) {
	err = json.Unmarshal([]byte(strObs), &o)
	return
}

// --- Logger -------------------------------------------------------------------------------------

type Logger struct {
	Debugging bool
}

func (o *Logger) Info(msg string, prm ...interface{}) {
	fmt.Printf(msg, prm...)
}

func (o *Logger) Debug(msg string, prm ...interface{}) {
	if o.Debugging {
		fmt.Printf(msg, prm...)
	}
}

func (o *Logger) Error(msg string, prm ...interface{}) {
	fmt.Printf("ERROR: "+msg, prm...)
}
