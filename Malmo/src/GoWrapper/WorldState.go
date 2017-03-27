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
typedef void* goptWorldState; // go-pointer
#include "x_timestamp.h"
*/
import "C"

// WorldState represents the state of the game world at a moment in time.
type WorldState struct {
	HasMissionBegun                    bool // Specifies whether the mission had begun when this world state was taken (whether or not it has since finished).
	IsMissionRunning                   bool // Specifies whether the mission was still running at the moment this world state was taken.
	NumberOfVideoFramesSinceLastState  int  // Contains the number of video frames that have been received since the last time the world state was taken. May differ from the number of video frames that are stored, depending on the video frames policy that was used.
	NumberOfRewardsSinceLastState      int  // Contains the number of rewards that have been received since the last time the world state was taken. May differ from the number of rewards that are stored, depending on the rewards policy that was used.
	NumberOfObservationsSinceLastState int  // Contains the number of observations that have been received since the last time the world state was taken. May differ from the number of observations that are stored, depending on the observations policy that was used.

	VideoFrames            []*TimestampedVideoFrame // Contains the timestamped video frames that are stored in this world state. May differ from the number of video frames that were received, depending on the video policy that was used.
	Rewards                []*TimestampedReward     // Contains the timestamped rewards that are stored in this world state. May differ from the number of rewards that were received, depending on the rewards policy that was used.
	Observations           []*TimestampedString     // Contains the timestamped observations that are stored in this world state. May differ from the number of observations that were received, depending on the observations policy that was used.
	MissionControlMessages []*TimestampedString     // Contains the timestamped mission control messages that are stored in this world state.
	Errors                 []*TimestampedString     // If there are errors in receiving the messages then we log them here.
}

// Clear resets the world state to be empty, with no mission running.
func (o *WorldState) Clear() {

	// clear values
	o.HasMissionBegun = false
	o.IsMissionRunning = false
	o.NumberOfVideoFramesSinceLastState = 0
	o.NumberOfRewardsSinceLastState = 0
	o.NumberOfObservationsSinceLastState = 0

	// clear arrays
	o.VideoFrames = []*TimestampedVideoFrame{}
	o.Rewards = []*TimestampedReward{}
	o.Observations = []*TimestampedString{}
	o.MissionControlMessages = []*TimestampedString{}
	o.Errors = []*TimestampedString{}
}

// _callfromcpp_world_state_set_values sets WorldState with data from Cpp code
//export _callfromcpp_world_state_set_values
func _callfromcpp_world_state_set_values(gopt C.goptWorldState,
	cHasMissionBegun,
	cIsMissionRunning,
	cNumberOfVideoFramesSinceLastState,
	cNumberOfRewardsSinceLastState,
	cNumberOfObservationsSinceLastState C.int) {

	ws := (*WorldState)(gopt)
	ws.HasMissionBegun = CI2B(cHasMissionBegun)
	ws.IsMissionRunning = CI2B(cIsMissionRunning)
	ws.NumberOfVideoFramesSinceLastState = int(cNumberOfVideoFramesSinceLastState)
	ws.NumberOfRewardsSinceLastState = int(cNumberOfRewardsSinceLastState)
	ws.NumberOfObservationsSinceLastState = int(cNumberOfObservationsSinceLastState)
}

// _callfromcpp_world_state_append_reward appends new timestamped reward message to Errors
//export _callfromcpp_world_state_append_reward
func _callfromcpp_world_state_append_reward(gopt C.goptWorldState, ts *C.timestamp_t, ndim C.int, values *C.double) {
	ws := (*WorldState)(gopt)
	ws.Rewards = append(ws.Rewards, newTimestampedRewardFromCpp(ts, ndim, values))
}

// _callfromcpp_world_state_append_observation appends new timestamped observation message to Errors
//export _callfromcpp_world_state_append_observation
func _callfromcpp_world_state_append_observation(gopt C.goptWorldState, ts *C.timestamp_t, text *C.char, text_size C.int) {
	ws := (*WorldState)(gopt)
	ws.Observations = append(ws.Observations, newTimestampedStringFromCpp(ts, text, text_size))
}

// _callfromcpp_world_state_append_controlmessage appends new timestamped missioncontrolmessage message to Errors
//export _callfromcpp_world_state_append_controlmessage
func _callfromcpp_world_state_append_controlmessage(gopt C.goptWorldState, ts *C.timestamp_t, text *C.char, text_size C.int) {
	ws := (*WorldState)(gopt)
	ws.MissionControlMessages = append(ws.MissionControlMessages, newTimestampedStringFromCpp(ts, text, text_size))
}

// _callfromcpp_world_state_append_error appends new timestamped error message to Errors
//export _callfromcpp_world_state_append_error
func _callfromcpp_world_state_append_error(gopt C.goptWorldState, ts *C.timestamp_t, text *C.char, text_size C.int) {
	ws := (*WorldState)(gopt)
	ws.Errors = append(ws.Errors, newTimestampedStringFromCpp(ts, text, text_size))
}
