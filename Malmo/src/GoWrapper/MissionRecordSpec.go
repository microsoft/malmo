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
#include "x_auxiliary.h"
#include "x_definitions.h"
*/
import "C"

import "unsafe"

// MissionRecordSpec specifies the type of data that should be recorded from the mission.
type MissionRecordSpec struct {
	RecordMp4          bool   // Requests that video be recorded, at the specified quality. Ensure that the width of the video requested is divisible by 4, and the height of the video requested is divisible by 2.
	RecordObservations bool   // Requests that observations be recorded.
	RecordRewards      bool   // Requests that rewards be recorded.
	RecordCommands     bool   // Requests that commands be recorded.
	Mp4BitRate         int    // The bit rate to record at. e.g. 400000 for 400kbps.
	Mp4Fps             int    // The number of frames to record per second. e.g. 20.
	Destination        string // Specifies the destination for the recording.
}

// toC converts Go data to C data
func (o MissionRecordSpec) toC() (mrs C.mission_record_spec_t, free func()) {
	mrs.record_mp4 = 0
	mrs.record_observations = 0
	mrs.record_rewards = 0
	mrs.record_commands = 0
	if o.RecordMp4 {
		mrs.record_mp4 = 1
	}
	if o.RecordObservations {
		mrs.record_observations = 1
	}
	if o.RecordRewards {
		mrs.record_rewards = 1
	}
	if o.RecordCommands {
		mrs.record_commands = 1
	}
	mrs.mp4_bit_rate = C.long(o.Mp4BitRate)
	mrs.mp4_fps = C.int(o.Mp4Fps)
	mrs.destination = C.CString(o.Destination)
	free = func() {
		C.free(unsafe.Pointer(mrs.destination))
	}
	return
}
