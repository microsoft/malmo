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

package malmo

import (
	"errors"
	"fmt"
	"time"
)

// StartMissionSimple calls AgentHost.StartMissionSimple until mission starts
// retries -- number of trials
func StartMissionSimple(retries int, ah *AgentHost, m *MissionSpec, mr *MissionRecordSpec, verbose bool) (err error) {

	// attempt to start a mission
	for retry := 0; retry < retries; retry++ {
		e := ah.StartMissionSimple(m, mr)
		if e == nil {
			break
		}
		if retry == retries-1 {
			err = errors.New(fmt.Sprintf("Error starting mission:\n%v\nIs the game running?\n", e))
			return
		}
		time.Sleep(1000 * time.Millisecond)
	}

	// loop until mission starts
	if verbose {
		fmt.Print("Waiting for the mission to start")
	}
	ws := ah.GetWorldState()
	for !ws.HasMissionBegun {
		if verbose {
			fmt.Print(".")
		}
		time.Sleep(100 * time.Millisecond)
		ws = ah.GetWorldState()
		if verbose {
			for _, e := range ws.Errors {
				fmt.Printf("Error: %v\n", e.Text)
			}
		}
	}
	if verbose {
		fmt.Println()
		fmt.Println("Mission running")
	}
	return
}

// StartMission calls AgentHost.StartMission until mission starts
// retries -- number of trials
func StartMission(retries int, ahs []*AgentHost, m *MissionSpec, cp *ClientPool, mr *MissionRecordSpec, id string, verbose bool) (err error) {

	// attempt to start a mission
	for i, ah := range ahs {
		for retry := 0; retry < retries; retry++ {
			e := ah.StartMission(m, cp, mr, i, id)
			if e == nil {
				break
			}
			if retry == retries-1 {
				err = errors.New(fmt.Sprintf("Error starting mission:\n%v\nIs the game running?\n", e))
				return
			}
			time.Sleep(1000 * time.Millisecond)
		}
	}

	// loop until mission starts
	if verbose {
		fmt.Printf("Waiting for the mission to start ")
	}
	hasBegun := false
	hadErrors := false
	for !hasBegun && !hadErrors {
		if verbose {
			fmt.Printf(".")
		}
		time.Sleep(200 * time.Millisecond)
		hasBegun = true
		for i, ah := range ahs {
			ws := ah.GetWorldState()
			if !ws.HasMissionBegun {
				hasBegun = false
			}
			if len(ws.Errors) > 0 {
				hadErrors = true
				if verbose {
					fmt.Printf("Errors from agent # %d\n", i)
					for _, e := range ws.Errors {
						fmt.Printf("Error: %v\n", e.Text)
					}
				}
			}
		}
	}
	if verbose {
		fmt.Println()
	}

	// check
	if !hasBegun {
		err = errors.New(fmt.Sprintf("mission failed to begin"))
		return
	}
	if hadErrors {
		err = errors.New(fmt.Sprintf("hadErrors flag should be false at this point"))
		return
	}
	if verbose {
		fmt.Println("Mission running")
	}
	return
}
