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

import "testing"

func Test_worldstate01(tst *testing.T) {

	world_state := NewWorldState()
	defer world_state.Free()

	world_state.Clear()

	if world_state.HasMissionBegun() {
		tst.Errorf("HasMissionBegun should be false")
		return
	}

	if world_state.IsMissionRunning() {
		tst.Errorf("IsMissionRunning should be false")
		return
	}

	if world_state.NumberOfVideoFramesSinceLastState() != 0 {
		tst.Errorf("NumberOfVideoFramesSinceLastState should be 0")
		return
	}

	if world_state.NumberOfRewardsSinceLastState() != 0 {
		tst.Errorf("NumberOfRewardsSinceLastState should be 0")
		return
	}

	if world_state.NumberOfObservationsSinceLastState() != 0 {
		tst.Errorf("NumberOfObservationsSinceLastState should be 0")
		return
	}
}
