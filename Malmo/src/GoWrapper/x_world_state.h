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

#ifndef WORLDSTATE_H
#define WORLDSTATE_H

#ifdef __cplusplus
extern "C" {
#endif

// max number of characters for error message text from C++ to Go
#define WS_ERROR_BUFFER_SIZE 1024

// macro to help with calling WorldState methods and handling exception errors
//   pt  -- pointer to WorldState
//   err -- error message buffer
#define WS_CALL(command)                                          \
    WorldState* world_state = (WorldState*)pt;                    \
    try {                                                         \
        command                                                   \
    } catch (const exception& e) {                                \
        std::string message = std::string("ERROR: ") + e.what();  \
        strncpy(err, message.c_str(), WS_ERROR_BUFFER_SIZE);      \
        return 1;                                                 \
    }                                                             \
    return 0;

// pointer to WorldState type
typedef void* ptWorldState;

// constructor
ptWorldState new_world_state();

// destructor
void free_world_state(ptWorldState world_state);

// All functions return:
//  0 = OK
//  1 = failed; e.g. exception happend => see ERROR_MESSAGE

int world_state_clear                                   (ptWorldState pt, char* err);
int world_state_has_mission_begun                       (ptWorldState pt, char* err, int* response);
int world_state_is_mission_running                      (ptWorldState pt, char* err, int* response);
int world_state_number_of_video_frames_since_last_state (ptWorldState pt, char* err, int* response);
int world_state_number_of_rewards_since_last_state      (ptWorldState pt, char* err, int* response);
int world_state_number_of_observations_since_last_state (ptWorldState pt, char* err, int* response);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // WORLDSTATE_H
