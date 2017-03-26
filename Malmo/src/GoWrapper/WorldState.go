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

/*
#include "go_worldstate.h"
#include "auxiliary.h"
*/
import "C"

import "unsafe"

// WorldState represents the state of the game world at a moment in time.
type WorldState struct {
	pt  C.ptWorldState // pointer to C.WorldState
	err *C.char        // buffer to hold error messages from C++
}

// NewWorldState creates new WorldState
func NewWorldState() (o *WorldState) {
	o = new(WorldState)
	o.pt = C.new_world_state()
	if o.pt == nil {
		panic("ERROR: Cannot create new WorldState")
	}
	o.err = C.make_buffer(C.WS_ERROR_BUFFER_SIZE)
	return
}

// Free clears allocated memory
func (o *WorldState) Free() {
	if o.pt != nil {
		C.free_world_state(o.pt)
		C.free_buffer(o.err)
	}
}

//! Resets the world state to be empty, with no mission running.
func (o *WorldState) Clear() {
	status := C.world_state_clear(o.pt, o.err)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

// HasMissionBegun specifies whether the mission had begun when this world state was taken (whether or not it has since finished).
func (o WorldState) HasMissionBegun() bool {
	var response int
	cresponse := (*C.int)(unsafe.Pointer(&response))
	status := C.world_state_has_mission_begun(o.pt, o.err, cresponse)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	if response == 1 {
		return true
	}
	return false
}

// IsMissionRunning specifies whether the mission was still running at the moment this world state was taken.
func (o WorldState) IsMissionRunning() bool {
	var response int
	cresponse := (*C.int)(unsafe.Pointer(&response))
	status := C.world_state_is_mission_running(o.pt, o.err, cresponse)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	if response == 1 {
		return true
	}
	return false
}

// NumberOfVideoFramesSinceLastState contains the number of video frames that have been received since the last time the world state was taken.
// May differ from the number of video frames that are stored, depending on the video frames policy that was used.
//  see video_frames
func (o WorldState) NumberOfVideoFramesSinceLastState() int {
	var response int
	cresponse := (*C.int)(unsafe.Pointer(&response))
	status := C.world_state_number_of_video_frames_since_last_state(o.pt, o.err, cresponse)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	return response
}

// NumberOfRewardsSinceLastState contains the number of rewards that have been received since the last time the world state was taken.
// May differ from the number of rewards that are stored, depending on the rewards policy that was used.
//  see rewards
func (o WorldState) NumberOfRewardsSinceLastState() int {
	var response int
	cresponse := (*C.int)(unsafe.Pointer(&response))
	status := C.world_state_number_of_rewards_since_last_state(o.pt, o.err, cresponse)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	return response
}

// NumberOfObservationsSinceLastState contains the number of observations that have been received since the last time the world state was taken.
// May differ from the number of observations that are stored, depending on the observations policy that was used.
//  see observations
func (o WorldState) NumberOfObservationsSinceLastState() int {
	var response int
	cresponse := (*C.int)(unsafe.Pointer(&response))
	status := C.world_state_number_of_observations_since_last_state(o.pt, o.err, cresponse)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	return response
}
