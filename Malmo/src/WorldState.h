// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _WORLDSTATE_H_
#define _WORLDSTATE_H_

// Local:
#include "TimestampedFloat.h"
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

        void clear();

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
        std::vector< TimestampedVideoFrame > video_frames;

        //! Contains the timestamped rewards that are stored in this world state.
        /*! May differ from the number of rewards that were received, depending on the rewards policy that was used.
         * \see AgentHost::setRewardsPolicy
         */
        std::vector< TimestampedFloat > rewards;

        //! Contains the timestamped observations that are stored in this world state.
        /*! May differ from the number of observations that were received, depending on the observations policy that was used.
         * \see AgentHost::setObservationsPolicy
         */
        std::vector< TimestampedString > observations;

        //! Contains the timestamped mission control messages that are stored in this world state.
        std::vector< TimestampedString > mission_control_messages;

        //! If there are errors in receiving the messages then we log them here.
        std::vector< TimestampedString > errors;
    };
}

#endif
