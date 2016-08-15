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

// Local:
#include "WorldState.h"

namespace malmo
{
    WorldState::WorldState()
        : is_mission_running( false )
        , has_mission_begun( false )
        , number_of_video_frames_since_last_state(0)
        , number_of_rewards_since_last_state(0)
        , number_of_observations_since_last_state(0)
    {
    }
    
    void WorldState::clear()
    {
        this->is_mission_running = false;
        this->has_mission_begun = false;
        this->number_of_observations_since_last_state = 0;
        this->number_of_rewards_since_last_state = 0;
        this->number_of_video_frames_since_last_state = 0;
        this->observations.clear();
        this->rewards.clear();
        this->video_frames.clear();
        this->mission_control_messages.clear();
        this->errors.clear();
    }

    std::ostream& operator<<(std::ostream& os, const WorldState& ws)
    {
        os << "WorldState (";
        if (ws.is_mission_running)
            os << "running): ";
        else
            os << (ws.has_mission_begun ? "ended): " : "not running): ");
        os << ws.number_of_observations_since_last_state << " obs, ";
        os << ws.number_of_rewards_since_last_state << " rewards, ";
        os << ws.number_of_video_frames_since_last_state << " frames since last state.";
        return os;
    }
}
