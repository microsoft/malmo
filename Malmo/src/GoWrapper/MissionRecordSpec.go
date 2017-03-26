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
#include "x_mission_record_spec.h"
#include "auxiliary.h"
*/
import "C"

import "unsafe"

// MissionRecordSpec specifies the type of data that should be recorded from the mission.
type MissionRecordSpec struct {
	pt  C.ptMissionRecordSpec // pointer to C.MissionRecordSpec
	err *C.char               // buffer to hold error messages from C++
}

// NewMissionRecordSpec constructs an empty mission record specification, saying that nothing should be recorded.
func NewMissionRecordSpec() (o *MissionRecordSpec) {
	o = new(MissionRecordSpec)
	o.pt = C.new_mission_record_spec()
	o.err = C.make_buffer(C.MRS_ERROR_BUFFER_SIZE)
	return
}

// NewMissionRecordSpecTarget constructs a mission record with a target file (e.g. 'data.tgz').
// By default, nothing is recorded. Use the other functions to specify what channels should be recorded.
// WARNING: You cannot re-use the instance of MissionRecordSpec - make a new one per call to AgentHost.startMission.
// destination -- Filename to save to.
func NewMissionRecordSpecTarget(destination string) (o *MissionRecordSpec) {
	o = new(MissionRecordSpec)
	cdestination := C.CString(destination)
	defer C.free(unsafe.Pointer(cdestination))
	o.pt = C.new_mission_record_spec_target(cdestination)
	o.err = C.make_buffer(C.MRS_ERROR_BUFFER_SIZE)
	return
}

// Free deallocates MissionRecordSpec object
func (o *MissionRecordSpec) Free() {
	if o.pt != nil {
		C.free_mission_record_spec(o.pt)
		C.free_buffer(o.err)
	}
}

// Specifies the destination for the recording.
func (o *MissionRecordSpec) SetDestination(destination string) {
	cdestination := C.CString(destination)
	defer C.free(unsafe.Pointer(cdestination))
	status := C.mission_record_spec_set_destination(o.pt, o.err, cdestination)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

// Requests that video be recorded, at the specified quality.
// Ensure that the width of the video requested is divisible by 4, and the height of the video requested is divisible by 2.
// \param frames_per_second The number of frames to record per second. e.g. 20.
// \param bit_rate The bit rate to record at. e.g. 400000 for 400kbps.
func (o *MissionRecordSpec) RecordMP4(frames_per_second, bit_rate int) {
	status := C.mission_record_spec_record_mp4(o.pt, o.err, C.int(frames_per_second), C.int(bit_rate))
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

// Requests that observations be recorded.
func (o *MissionRecordSpec) RecordObservations() {
	status := C.mission_record_spec_record_observations(o.pt, o.err)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

// Requests that rewards be recorded.
func (o *MissionRecordSpec) RecordRewards() {
	status := C.mission_record_spec_record_rewards(o.pt, o.err)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

// Requests that commands be recorded.
func (o *MissionRecordSpec) RecordCommands() {
	status := C.mission_record_spec_record_commands(o.pt, o.err)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
}

//! Are we recording anything?
func (o MissionRecordSpec) IsRecording() bool {
	var response int
	cresponse := (*C.int)(unsafe.Pointer(&response))
	status := C.mission_record_spec_is_recording(o.pt, o.err, cresponse)
	if status != 0 {
		message := C.GoString(o.err)
		panic("ERROR:\n" + message)
	}
	if response == 1 {
		return true
	}
	return false
}
