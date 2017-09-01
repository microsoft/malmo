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

#ifndef _ALEAGENTHOST_H_
#define _ALEAGENTHOST_H_

// Local:
#include "ArgumentParser.h"
#include "ClientConnection.h"
#include "ClientPool.h"
#include "MissionInitSpec.h"
#include "MissionRecord.h"
#include "MissionSpec.h"
#include "StringServer.h"
#include "VideoServer.h"
#include "WorldState.h"
#include "AgentHost.h"

// Boost:
#include <boost/thread.hpp>

// STL:
#include <string>

class ALEInterface;

namespace malmo
{
    //! An ALE agent host mediates between the researcher's code (the agent) and the ALE (the target environment).
    class ALEAgentHost : public ArgumentParser
    {
        public:
            //! Creates an agent host with default settings.
            ALEAgentHost();

            //! Destructor
            ~ALEAgentHost();

            //! Set the random seed used to seed the ALE.
            //! If 0 (the default), the ALE will use the current time instead.
            void setSeed(int seed) { this->seed = seed; }

            //! Starts a mission running. Throws an exception if something goes wrong.
            //! \param mission The mission specification.
            //! \param client_pool leave this null - meaningless for ALE
            //! \param mission_record The specification of the mission recording to make.
            //! \param role Index of the agent that this agent host is to manage. Zero-based index. Use zero if there is only one agent in this mission.
            //! \param unique_experiment_id An arbitrary identifier that is used to disambiguate our mission from other runs.
            void startMission( const MissionSpec& mission, const ClientPool& client_pool, const MissionRecordSpec& mission_record, int role, std::string unique_experiment_id );

            //! Starts a mission running, in the simple case where there is only one agent running on the local machine. Throws an exception if something goes wrong.
            //! \param mission The mission specification.
            //! \param mission_record The specification of the mission recording to make.
            void startMission(const MissionSpec& mission, const MissionRecordSpec& mission_record);

            //! Gets the latest world state received from the game.
            //! \returns The world state.
            WorldState peekWorldState() const;

            //! Gets the latest world state received from the game. Resets the count of items received since the last time.
            //! \returns The world state.
            WorldState getWorldState();

            //! Gets the temporary directory being used for the mission record, if recording is taking place.
            //! \returns The temporary directory for the mission record, or an empty string if no recording is going on.
            std::string getRecordingTemporaryDirectory() const;

            //! Specifies how you want to deal with multiple video frames.
            //! \param videoPolicy How you want to deal with multiple video frames coming in asynchronously.
            void setVideoPolicy(AgentHost::VideoPolicy videoPolicy);

            //! Specifies how you want to deal with multiple rewards.
            //! \param rewardsPolicy How you want to deal with multiple rewards coming in asynchronously.
            void setRewardsPolicy(AgentHost::RewardsPolicy rewardsPolicy);

            //! Specifies how you want to deal with multiple observations.
            //! \param observationsPolicy How you want to deal with multiple observations coming in asynchronously.
            void setObservationsPolicy(AgentHost::ObservationsPolicy observationsPolicy);
            
            //! Sends a command to the game client.
            //! See the mission handlers documentation for the permitted commands for your chosen command handler.
            //! \param command The command to send as a string. e.g. "move 1"
            void sendCommand(std::string command);

            void close();
            friend std::ostream& operator<<(std::ostream& os, const ALEAgentHost& ah);

        private:
            void initialize(const MissionSpec& mission, const MissionRecordSpec& mission_record, int role, std::string unique_experiment_id);

            void onVideo(TimestampedVideoFrame message);
            void onReward(boost::posix_time::ptime ts, float reward);
            void onObservation(TimestampedString message);

            AgentHost::VideoPolicy        video_policy;
            AgentHost::RewardsPolicy      rewards_policy;
            AgentHost::ObservationsPolicy observations_policy;
            
            WorldState world_state;
            boost::shared_ptr<ALEInterface> ale_interface;
            std::unique_ptr<VideoFrameWriter> video_frame_writer;

            mutable boost::mutex world_state_mutex;
            
            boost::shared_ptr<MissionRecord> current_mission_record;
            int current_role;
            int requested_width;
            int requested_height;
            std::ofstream commands_stream;
            std::ofstream reward_stream;
            int seed;
    };
}

#endif
