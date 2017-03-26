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

#ifndef MISSIONRECORDSPEC_H
#define MISSIONRECORDSPEC_H

#ifdef __cplusplus
extern "C" {
#endif

// max number of characters for error message text from C++ to Go
#define MRS_ERROR_BUFFER_SIZE 1024

// macro to help with calling MissionRecordSpec methods and handling exception errors
//   pt  -- pointer to MissionSpec
//   err -- error message buffer
#define MRS_CALL(command)                                         \
    MissionRecordSpec* mr_spec = (MissionRecordSpec*)pt;          \
    try {                                                         \
        command                                                   \
    } catch (const exception& e) {                                \
        std::string message = std::string("ERROR: ") + e.what();  \
        strncpy(err, message.c_str(), MRS_ERROR_BUFFER_SIZE);     \
        return 1;                                                 \
    }                                                             \
    return 0;

// pointer to MissionRecordSpec
typedef void* ptMissionRecordSpec;

// constructor
ptMissionRecordSpec new_mission_record_spec();

// alternative constructor
ptMissionRecordSpec new_mission_record_spec_target(const char* destination);

// destructor
void free_mission_record_spec(ptMissionRecordSpec pt);

// All functions return:
//  0 = OK
//  1 = failed; e.g. exception happend => see ERROR_MESSAGE

int mission_record_spec_set_destination     (ptMissionRecordSpec pt, char* err, const char* destination);
int mission_record_spec_record_mp4          (ptMissionRecordSpec pt, char* err, int frames_per_second, int bit_rate);
int mission_record_spec_record_observations (ptMissionRecordSpec pt, char* err);
int mission_record_spec_record_rewards      (ptMissionRecordSpec pt, char* err);
int mission_record_spec_record_commands     (ptMissionRecordSpec pt, char* err);
int mission_record_spec_is_recording        (ptMissionRecordSpec pt, char* err, int* response);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // MISSIONRECORDSPEC_H
