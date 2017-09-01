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

#ifndef _WORLDSTATE_H_
#define _WORLDSTATE_H_

// Local:
#include "TimestampedReward.h"
#include "TimestampedString.h"
#include "TimestampedVideoFrame.h"

// STL:
#include <vector>

namespace malmo
{
    //! Represents the state of the game world at a moment in time.
    struct WorldState
    {
        WorldState();

        //! Resets the world state to be empty, with no mission running.
        void clear();

        //! Specifies whether the mission had begun when this world state was taken (whether or not it has since finished).
        bool has_mission_begun;

        //! Specifies whether the mission was still running at the moment this world state was taken.
        bool is_mission_running;

        //! Contains the number of video frames that have been received since the last time the world state was taken.
        /*! May differ from the number of video frames that are stored, depending on the video frames policy that was used.
         *  \see video_frames
         */
        int number_of_video_frames_since_last_state;

        //! Contains the number of rewards that have been received since the last time the world state was taken.
        /*! May differ from the number of rewards that are stored, depending on the rewards policy that was used.
         *  \see rewards
         */
        int number_of_rewards_since_last_state;

        //! Contains the number of observations that have been received since the last time the world state was taken.
        /*! May differ from the number of observations that are stored, depending on the observations policy that was used.
         *  \see observations
         */
        int number_of_observations_since_last_state;

        //! Contains the timestamped video frames that are stored in this world state.
        /*! May differ from the number of video frames that were received, depending on the video policy that was used.
         * \see AgentHost::setVideoPolicy
         */
        std::vector< boost::shared_ptr< TimestampedVideoFrame > > video_frames;

        //! Contains the timestamped rewards that are stored in this world state.
        /*! May differ from the number of rewards that were received, depending on the rewards policy that was used.
         * \see AgentHost::setRewardsPolicy
         */
        std::vector< boost::shared_ptr< TimestampedReward > > rewards;

        //! Contains the timestamped observations that are stored in this world state.
        /*! May differ from the number of observations that were received, depending on the observations policy that was used.
         * \see AgentHost::setObservationsPolicy
         */
        std::vector< boost::shared_ptr< TimestampedString > > observations;

        //! Contains the timestamped mission control messages that are stored in this world state.
        std::vector< boost::shared_ptr< TimestampedString > > mission_control_messages;

        //! If there are errors in receiving the messages then we log them here.
        std::vector< boost::shared_ptr< TimestampedString > > errors;

        friend std::ostream& operator<<(std::ostream& os, const WorldState& ws);
    };
}

#endif
