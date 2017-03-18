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
#include "go_missionrecordspec.h"

#include "stdlib.h"
*/
import "C"

import "unsafe"

// MissionRecordSpec specifies the type of data that should be recorded from the mission.
type MissionRecordSpec struct {
	mission_record_spec C.ptMissionRecordSpec // pointer to C.MissionRecordSpec
}

// NewMissionRecordSpec constructs an empty mission record specification, saying that nothing should be recorded.
func NewMissionRecordSpec() (o *MissionRecordSpec) {
	o = new(MissionRecordSpec)
	o.mission_record_spec = C.new_mission_record_spec()
	return
}

// NewMissionRecordSpecTarget constructs a mission record with a target file (e.g. 'data.tgz').
// By default, nothing is recorded. Use the other functions to specify what channels should be recorded.
// WARNING: You cannot re-use the instance of MissionRecordSpec - make a new one per call to AgentHost.startMission.
// destination -- Filename to save to.
func NewMissionRecordSpecTarget(destination string) (o *MissionRecordSpec) {
	o = new(MissionRecordSpec)
	cdest := C.CString(destination)
	defer C.free(unsafe.Pointer(cdest))
	o.mission_record_spec = C.new_mission_record_spec_target(cdest)
	return
}

// Free deallocates MissionRecordSpec object
func (o *MissionRecordSpec) Free() {
	if o.mission_record_spec != nil {
		C.free_mission_record_spec(o.mission_record_spec)
	}
}

// Specifies the destination for the recording.
func (o *MissionRecordSpec) SetDestination(destination string) {
	panic("TODO")
}

// Requests that video be recorded, at the specified quality.
// Ensure that the width of the video requested is divisible by 4, and the height of the video requested is divisible by 2.
// \param frames_per_second The number of frames to record per second. e.g. 20.
// \param bit_rate The bit rate to record at. e.g. 400000 for 400kbps.
func (o *MissionRecordSpec) RecordMP4(frames_per_second, bit_rate int) {
	panic("TODO")
}

// Requests that observations be recorded.
func (o *MissionRecordSpec) RecordObservations() {
	panic("TODO")
}

// Requests that rewards be recorded.
func (o *MissionRecordSpec) RecordRewards() {
	panic("TODO")
}

// Requests that commands be recorded.
func (o *MissionRecordSpec) RecordCommands() {
	panic("TODO")
}

//! Are we recording anything?
func (o MissionRecordSpec) IsRecording() bool {
	panic("TODO")
	return false
}
