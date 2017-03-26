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
#include <WorldState.h>
using namespace malmo;

// STL:
#include <exception>
using namespace std;

// Local:
#include "x_world_state.h"

ptWorldState new_world_state() {
    try {
        WorldState * pt = new WorldState;
        return (void*)pt;
    } catch (const exception& e) {
        // returning NULL pointer
    }
    return NULL;
}

void free_world_state(ptWorldState world_state) {
    if (world_state != NULL) {
        WorldState * pt = (WorldState*)world_state;
        delete pt;
    }
}

int world_state_clear(ptWorldState pt, char* err) {
    WS_CALL(
        world_state->clear();
    )
}

int world_state_has_mission_begun(ptWorldState pt, char* err, int* response) {
    WS_CALL(
        if (world_state->has_mission_begun) {
            *response = 1;
        } else {
            *response = 0;
        }
    )
}

int world_state_is_mission_running(ptWorldState pt, char* err, int* response) {
    WS_CALL(
        if (world_state->is_mission_running) {
            *response = 1;
        } else {
            *response = 0;
        }
    )
}

int world_state_number_of_video_frames_since_last_state(ptWorldState pt, char* err, int* response) {
    WS_CALL(
        *response = world_state->number_of_video_frames_since_last_state;
    )
}

int world_state_number_of_rewards_since_last_state(ptWorldState pt, char* err, int* response) {
    WS_CALL(
        *response = world_state->number_of_rewards_since_last_state;
    )
}

int world_state_number_of_observations_since_last_state(ptWorldState pt, char* err, int* response) {
    WS_CALL(
        *response = world_state->number_of_observations_since_last_state;
    )
}
