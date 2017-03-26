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

// Malmo:
#include <MissionRecordSpec.h>
using namespace malmo;

// Local:
#include "x_mission_record_spec.h"

ptMissionRecordSpec new_mission_record_spec() {
    MissionRecordSpec * pt = new MissionRecordSpec;
    return (void*)pt;
}

ptMissionRecordSpec new_mission_record_spec_target(const char* destination) {
    MissionRecordSpec * pt = new MissionRecordSpec(destination);
    return (void*)pt;
}

void free_mission_record_spec(ptMissionRecordSpec mission_record_spec) {
    if (mission_record_spec != NULL) {
        MissionRecordSpec * pt = (MissionRecordSpec*)mission_record_spec;
        delete pt;
    }
}
