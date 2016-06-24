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
#include "MissionRecordSpec.h"

// Boost:
#include <boost/iostreams/copy.hpp>
#include <boost/iostreams/filter/gzip.hpp>
#include <boost/iostreams/filtering_streambuf.hpp>
#include <boost/iostreams/stream.hpp>
#include <boost/uuid/uuid.hpp>
#include <boost/uuid/uuid_generators.hpp>
#include <boost/uuid/uuid_io.hpp>

// STL:
#include <exception>
#include <iostream>

namespace malmo
{
    MissionRecordSpec::MissionRecordSpec()
        : is_recording(false)
        , is_recording_mp4(false)
        , is_recording_observations(false)
        , is_recording_rewards(false)
        , is_recording_commands(false)
        , mp4_bit_rate(0)
        , mp4_fps(0)
    {
    }

    MissionRecordSpec::MissionRecordSpec(std::string destination)
        : is_recording(true)
        , is_recording_mp4(false)
        , is_recording_observations(false)
        , is_recording_rewards(false)
        , is_recording_commands(false)
        , mp4_bit_rate(0)
        , mp4_fps(0)
        , destination(destination)
    {
        boost::uuids::random_generator gen;
        boost::uuids::uuid temp_uuid = gen();
        this->temp_dir = boost::filesystem::path(".");
        this->temp_dir = this->temp_dir / "mission_records" / boost::uuids::to_string(temp_uuid);
        this->mp4_path = (this->temp_dir / "video.mp4").string();
        this->observations_path = (this->temp_dir / "observations.txt").string();
        this->rewards_path = (this->temp_dir / "rewards.txt").string();
        this->commands_path = (this->temp_dir / "commands.txt").string();
        this->mission_init_path = (this->temp_dir / "missionInit.xml").string();
 
        std::ofstream file(destination, std::ofstream::binary);
        if (file.fail()) {
           std::cout << "ERROR: Cannot write to " << destination << " - check the path exists and you have permission to write there." << std::endl;
           throw std::runtime_error("Can not write to recording destination.");
       }
    }

    void MissionRecordSpec::recordMP4(int frames_per_second, int64_t bit_rate)
    {
        if (!this->is_recording){
            throw std::runtime_error("Mission is not being recorded.");
        }

        this->is_recording_mp4 = true;
        this->mp4_fps = frames_per_second;
        this->mp4_bit_rate = bit_rate;
    }

    void MissionRecordSpec::recordObservations()
    {
        if (!this->is_recording){
            throw std::runtime_error("Mission is not being recorded.");
        }

        this->is_recording_observations = true;
    }

    void MissionRecordSpec::recordRewards()
    {
        if (!this->is_recording){
            throw std::runtime_error("Mission is not being recorded.");
        }

        this->is_recording_rewards = true;
    }

    void MissionRecordSpec::recordCommands()
    {
        if (!this->is_recording){
            throw std::runtime_error("Mission is not being recorded.");
        }

        this->is_recording_commands = true;
    }

    std::string MissionRecordSpec::getTemporaryDirectory()
    {
        if (!this->is_recording){
            throw std::runtime_error("Mission is not being recorded.");
        }

        if (boost::filesystem::exists(this->temp_dir)){
            return this->temp_dir.string();
        }else{
            throw std::runtime_error("Mission record does not yet exist. Temporary directory will be created once a mission has begun.");
        }
    }

    std::ostream& operator<<(std::ostream& os, const MissionRecordSpec& msp)
    {
        os << "MissionRecordSpec: ";
        os << "Recording? " << (msp.is_recording ? "Yes" : "No");
        if (msp.is_recording_mp4)
            os << "\n  -MP4 (bitrate: " << msp.mp4_bit_rate << ", fps: " << msp.mp4_fps << ")";
        if (msp.is_recording_observations)
            os << "\n  -observations";
        if (msp.is_recording_rewards)
            os << "\n  -rewards";
        if (msp.is_recording_commands)
            os << "\n  -commands";
        if (msp.destination.length())
            os << "\n to: " << msp.destination;
        return os;
    }
}
