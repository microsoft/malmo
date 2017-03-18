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
#include "go_missionspec.h"
*/
import "C"

// MissionSpec specifies a mission to be run.
type MissionSpec struct {
	mission_spec C.ptMissionSpec // pointer to C.MissionSpec
}

func NewMissionSpec() (o *MissionSpec) {
	o = new(MissionSpec)
	o.mission_spec = C.new_mission_spec()
	return
}

func (o *MissionSpec) Free() {
	if o.mission_spec != nil {
		C.free_mission_spec(o.mission_spec)
	}
}

func (o *MissionSpec) TimeLimitInSeconds(s float32) {
	C.mission_spec_time_limit_in_seconds(o.mission_spec, C.float(s))
}
