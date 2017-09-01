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

// Header: 
#include "ALEAgentHost.h"

// Local:
#include "TCPClient.h"
#include "WorldState.h"

// Boost:
#include <boost/bind.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/make_shared.hpp>
#include <boost/preprocessor/stringize.hpp>
#include <boost/property_tree/json_parser.hpp>
#include <boost/property_tree/ptree.hpp>
#include <boost/property_tree/xml_parser.hpp>

// Schemas:
#include <MissionEnded.h>

// STL:
#include <exception>
#include <sstream>

// ALE:
#include <ale_interface.hpp>

namespace malmo
{
    // set defaults
    ALEAgentHost::ALEAgentHost()
        : ArgumentParser( std::string("Malmo version: ") + BOOST_PP_STRINGIZE(MALMO_VERSION) )
        , video_policy(AgentHost::VideoPolicy::LATEST_FRAME_ONLY)
        , rewards_policy(AgentHost::RewardsPolicy::SUM_REWARDS)
        , observations_policy(AgentHost::ObservationsPolicy::LATEST_OBSERVATION_ONLY)
        , current_role( 0 )
        , seed( 0 )
    {
        this->addOptionalFlag("help,h", "show description of allowed options");
    }

    ALEAgentHost::~ALEAgentHost()
    {
        this->close();
    }

    void ALEAgentHost::startMission(const MissionSpec& mission, const MissionRecordSpec& mission_record)
    {
        startMission(mission, ClientPool(), mission_record, 0, "");
    }

    void ALEAgentHost::startMission(const MissionSpec& mission, const ClientPool& client_pool, const MissionRecordSpec& mission_record, int role, std::string unique_experiment_id)
    {
        boost::lock_guard<boost::mutex> scope_guard(this->world_state_mutex);

        if (this->world_state.is_mission_running) {
            throw std::runtime_error("A mission is already running.");
        }

        this->world_state.clear();
        // NB. Sets is_mission_running to false. The ALE decides when the mission actually starts.

        initialize(mission, mission_record, role, unique_experiment_id);

        this->ale_interface = boost::make_shared<ALEInterface>(role != 0);
        this->ale_interface->setInt("random_seed", this->seed);
        this->ale_interface->setFloat("repeat_action_probability", 0); // Default is 0.25. We really don't want this!
        this->ale_interface->loadROM(unique_experiment_id);

        if (this->video_frame_writer)
            this->video_frame_writer->open();
    }
    
    void ALEAgentHost::initialize(const MissionSpec& mission, const MissionRecordSpec& mission_record, int role, std::string unique_experiment_id)
    {
        // make a MissionInit structure with default settings
        this->current_mission_record = boost::make_shared<MissionRecord>( mission_record );
        this->current_role = role;
        this->requested_width = 160;    // default width and height values for ALE screen size.
        this->requested_height = 210;
        if (mission.isVideoRequested(this->current_role))
        {
            this->requested_width = mission.getVideoWidth(0);
            this->requested_height = mission.getVideoHeight(0);
            if (this->current_mission_record->isRecordingMP4())
            {
                this->video_frame_writer = VideoFrameWriter::create(this->current_mission_record->getMP4Path(), this->requested_width, this->requested_height, this->current_mission_record->getMP4FramesPerSecond(), this->current_mission_record->getMP4BitRate());
            }
        }

        if (this->commands_stream.is_open()){
            this->commands_stream.close();
        }

        if (this->current_mission_record->isRecordingCommands()){
            this->commands_stream.open(this->current_mission_record->getCommandsPath());
        }
        if (this->current_mission_record->isRecordingRewards()){
            this->reward_stream.open(this->current_mission_record->getRewardsPath());
        }
    }

    WorldState ALEAgentHost::peekWorldState() const
    {
        boost::lock_guard<boost::mutex> scope_guard(this->world_state_mutex);

        return this->world_state;
    }
    
    WorldState ALEAgentHost::getWorldState()
    {
        boost::lock_guard<boost::mutex> scope_guard(this->world_state_mutex);

        WorldState old_world_state( this->world_state );
        this->world_state.clear();
        this->world_state.has_mission_begun = this->ale_interface != 0;
        this->world_state.is_mission_running = this->ale_interface && !this->ale_interface->game_over();

        return old_world_state;
    }

    std::string ALEAgentHost::getRecordingTemporaryDirectory() const
    {
        return this->current_mission_record && this->current_mission_record->isRecording() ? this->current_mission_record->getTemporaryDirectory() : "";
    }

    void ALEAgentHost::setVideoPolicy(AgentHost::VideoPolicy videoPolicy)
    {
        this->video_policy = videoPolicy;
    }

    void ALEAgentHost::setRewardsPolicy(AgentHost::RewardsPolicy rewardsPolicy)
    {
        this->rewards_policy = rewardsPolicy;
    }

    void ALEAgentHost::setObservationsPolicy(AgentHost::ObservationsPolicy observationsPolicy)
    {
        this->observations_policy = observationsPolicy;
    }
    
    void ALEAgentHost::close()
    {
        this->world_state.is_mission_running = false;

        if (this->commands_stream.is_open()){
            this->commands_stream.close();
        }

        if (this->reward_stream.is_open()){
            this->reward_stream.close();
        }

        if (this->video_frame_writer && this->video_frame_writer->isOpen())
            this->video_frame_writer->close();
    }

    void ALEAgentHost::onVideo(TimestampedVideoFrame message)
    {
        if (this->video_frame_writer)
            this->video_frame_writer->write(message);

        boost::lock_guard<boost::mutex> scope_guard(this->world_state_mutex);

        switch( this->video_policy )
        {
        case AgentHost::VideoPolicy::LATEST_FRAME_ONLY:
                this->world_state.video_frames.clear();
                this->world_state.video_frames.push_back( boost::make_shared<TimestampedVideoFrame>( message ) );
                break;
        case AgentHost::VideoPolicy::KEEP_ALL_FRAMES:
                this->world_state.video_frames.push_back( boost::make_shared<TimestampedVideoFrame>( message ) );
                break;
        }
        
        this->world_state.number_of_video_frames_since_last_state++;
    }
    
    void ALEAgentHost::onReward(boost::posix_time::ptime ts, float reward)
    {
        boost::lock_guard<boost::mutex> scope_guard(this->world_state_mutex);
       
        if (this->reward_stream && this->reward_stream.is_open())
        {
            this->reward_stream << boost::posix_time::to_iso_string(ts) << " " << "<Reward xmlns=\"http://ProjectMalmo.microsoft.com\"><Value dimension=\"0\" value=\"" << reward << "\" /</Reward>" << std::endl;
        }

        TimestampedReward tsr(reward);
        switch( this->rewards_policy )
        {
        case AgentHost::RewardsPolicy::LATEST_REWARD_ONLY:
                this->world_state.rewards.clear();
                this->world_state.rewards.push_back( boost::make_shared<TimestampedReward>( tsr ) );
                break;
        case AgentHost::RewardsPolicy::SUM_REWARDS:
                if( !this->world_state.rewards.empty() ) {
                    tsr.add(*this->world_state.rewards.front());
                    this->world_state.rewards.clear();
                }
                this->world_state.rewards.push_back( boost::make_shared<TimestampedReward>( tsr ) );
                // (timestamp is that of latest reward, even if zero)
                break;
        case AgentHost::RewardsPolicy::KEEP_ALL_REWARDS:
                this->world_state.rewards.push_back( boost::make_shared<TimestampedReward>( tsr ) );
                break;
        }
        this->world_state.number_of_rewards_since_last_state++;
    }
    
    void ALEAgentHost::onObservation(TimestampedString message)
    {
        boost::lock_guard<boost::mutex> scope_guard(this->world_state_mutex);

        switch( this->observations_policy )
        {
        case AgentHost::ObservationsPolicy::LATEST_OBSERVATION_ONLY:
                this->world_state.observations.clear();
                this->world_state.observations.push_back( boost::make_shared<TimestampedString>( message ) );
                break;
        case AgentHost::ObservationsPolicy::KEEP_ALL_OBSERVATIONS:
                this->world_state.observations.push_back( boost::make_shared<TimestampedString>( message ) );
                break;
        }
        
        this->world_state.number_of_observations_since_last_state++;
    }
    
    void ALEAgentHost::sendCommand(std::string command)
    {
        /*
            PLAYER_A_NOOP           = 0,
            PLAYER_A_FIRE           = 1,
            PLAYER_A_UP             = 2,
            PLAYER_A_RIGHT          = 3,
            PLAYER_A_LEFT           = 4,
            PLAYER_A_DOWN           = 5,
            PLAYER_A_UPRIGHT        = 6,
            PLAYER_A_UPLEFT         = 7,
            PLAYER_A_DOWNRIGHT      = 8,
            PLAYER_A_DOWNLEFT       = 9,
            PLAYER_A_UPFIRE         = 10,
            PLAYER_A_RIGHTFIRE      = 11,
            PLAYER_A_LEFTFIRE       = 12,
            PLAYER_A_DOWNFIRE       = 13,
            PLAYER_A_UPRIGHTFIRE    = 14,
            PLAYER_A_UPLEFTFIRE     = 15,
            PLAYER_A_DOWNRIGHTFIRE  = 16,
            PLAYER_A_DOWNLEFTFIRE   = 17,
         */
        Action a = (Action)::atoi(command.c_str());

        boost::posix_time::ptime ts = boost::posix_time::microsec_clock::universal_time();
        std::string timestamp = boost::posix_time::to_iso_string(ts);
        this->commands_stream << timestamp << " " << command << std::endl;
        float reward = this->ale_interface->act(a);
        onReward( ts, reward );

        // Get the video frame:
        const ALEScreen& screen = this->ale_interface->getScreen();

        // Create a TimestampedVideoFrame for our own use:
        TimestampedVideoFrame tsframe;
        tsframe.timestamp = ts;
        tsframe.channels = 3;
        tsframe.width = this->requested_width;
        tsframe.height = this->requested_height;

        int actual_width = screen.width();
        int actual_height = screen.height();

        int pixels = requested_width * requested_height;
        tsframe.pixels.resize(3 * pixels);
        // AleInterfrace.getScreenRGB has a stupid bug whereby it only returns the top third of the screen,
        // so we need to do this manually.
        // We also do the scaling at the same time, using nearest-neighbour for speed.
        double actual_to_requested_height = (double)requested_height / (double)actual_height;
        double actual_to_requested_width = (double)requested_width / (double)actual_width;
        double requested_to_actual_height = (double)actual_height / (double)requested_height;
        double requested_to_actual_width = (double)actual_width / (double)requested_width;

        if (actual_height < requested_height)
        {
            // Scaling up:
            for (int dsty = 0; dsty < requested_height; dsty++)
            {
                int srcy = (int)((double)dsty * requested_to_actual_height);

                if (actual_width < requested_width)
                {
                    // Scaling up:
                    for (int dstx = 0; dstx < requested_width; dstx++)
                    {
                        int srcx = (int)((double)dstx * requested_to_actual_width);
                        unsigned int pix = this->ale_interface->theOSystem->colourPalette().getRGB(screen.getArray()[srcx + srcy * actual_width]);
                        int i = 3 * (dstx + dsty* requested_width);
                        tsframe.pixels[i] = (unsigned char)(pix >> 16);     // red
                        tsframe.pixels[i + 1] = (unsigned char)(pix >> 8);  // green
                        tsframe.pixels[i + 2] = (unsigned char)pix;         // blue
                    }
                }
                else
                {
                    // Scaling down:
                    for (int srcx = 0; srcx < actual_width; srcx++)
                    {
                        int dstx = (int)((double)srcx * actual_to_requested_width);
                        unsigned int pix = this->ale_interface->theOSystem->colourPalette().getRGB(screen.getArray()[srcx + srcy * actual_width]);
                        int i = 3 * (dstx + dsty* requested_width);
                        tsframe.pixels[i] = (unsigned char)(pix >> 16);     // red
                        tsframe.pixels[i + 1] = (unsigned char)(pix >> 8);  // green
                        tsframe.pixels[i + 2] = (unsigned char)pix;         // blue
                    }
                }
            }
        }
        else
        {
            // Scaling down:
            for (int srcy = 0; srcy < actual_height; srcy++)
            {
                int dsty = (int)((double)srcy * actual_to_requested_height);

                if (actual_width < requested_width)
                {
                    // Scaling up:
                    for (int dstx = 0; dstx < requested_width; dstx++)
                    {
                        int srcx = (int)((double)dstx * requested_to_actual_width);
                        unsigned int pix = this->ale_interface->theOSystem->colourPalette().getRGB(screen.getArray()[srcx + srcy * actual_width]);
                        int i = 3 * (dstx + dsty* requested_width);
                        tsframe.pixels[i] = (unsigned char)(pix >> 16);     // red
                        tsframe.pixels[i + 1] = (unsigned char)(pix >> 8);  // green
                        tsframe.pixels[i + 2] = (unsigned char)pix;         // blue
                    }
                }
                else
                {
                    // Scaling down:
                    for (int srcx = 0; srcx < actual_width; srcx++)
                    {
                        int dstx = (int)((double)srcx * actual_to_requested_width);
                        unsigned int pix = this->ale_interface->theOSystem->colourPalette().getRGB(screen.getArray()[srcx + srcy * actual_width]);
                        int i = 3 * (dstx + dsty* requested_width);
                        tsframe.pixels[i] = (unsigned char)(pix >> 16);     // red
                        tsframe.pixels[i + 1] = (unsigned char)(pix >> 8);  // green
                        tsframe.pixels[i + 2] = (unsigned char)pix;         // blue
                    }
                }
            }
        }
        onVideo(tsframe);

        // Is the game still running, or did we just die?
        if (this->ale_interface->game_over())
            this->close();
    }

    std::ostream& operator<<(std::ostream& os, const ALEAgentHost& agent_host)
    {
        os << "ALEAgentHost";
        return os;
    }
}
