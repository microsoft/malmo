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

// WorldState represents the state of the game world at a moment in time.
type WorldState struct {
	HasMissionBegun                    bool // Specifies whether the mission had begun when this world state was taken (whether or not it has since finished).
	IsMissionRunning                   bool // Specifies whether the mission was still running at the moment this world state was taken.
	NumberOfVideoFramesSinceLastState  int  // Contains the number of video frames that have been received since the last time the world state was taken. May differ from the number of video frames that are stored, depending on the video frames policy that was used.
	NumberOfRewardsSinceLastState      int  // Contains the number of rewards that have been received since the last time the world state was taken. May differ from the number of rewards that are stored, depending on the rewards policy that was used.
	NumberOfObservationsSinceLastState int  // Contains the number of observations that have been received since the last time the world state was taken. May differ from the number of observations that are stored, depending on the observations policy that was used.
}

// Clear resets the world state to be empty, with no mission running.
func (o *WorldState) Clear() {
	o.HasMissionBegun = false
	o.IsMissionRunning = false
	o.NumberOfVideoFramesSinceLastState = 0
	o.NumberOfRewardsSinceLastState = 0
	o.NumberOfObservationsSinceLastState = 0
}
