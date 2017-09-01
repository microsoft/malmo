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
        : is_recording_mp4(false)
        , is_recording_observations(false)
        , is_recording_rewards(false)
        , is_recording_commands(false)
        , mp4_bit_rate(0)
        , mp4_fps(0)
    {
    }

    MissionRecordSpec::MissionRecordSpec(std::string destination)
        : is_recording_mp4(false)
        , is_recording_observations(false)
        , is_recording_rewards(false)
        , is_recording_commands(false)
        , mp4_bit_rate(0)
        , mp4_fps(0)
    {
        setDestination(destination);
    }

    void MissionRecordSpec::setDestination(const std::string& destination)
    {
        std::ofstream file(destination, std::ofstream::binary);
        if (file.fail()) {
            std::cout << "ERROR: Cannot write to " << destination << " - check the path exists and you have permission to write there." << std::endl;
            throw std::runtime_error("Can not write to recording destination.");
        }
        this->destination = destination;
    }

    void MissionRecordSpec::recordMP4(int frames_per_second, int64_t bit_rate)
    {
        this->is_recording_mp4 = true;
        this->mp4_fps = frames_per_second;
        this->mp4_bit_rate = bit_rate;
    }

    void MissionRecordSpec::recordObservations()
    {
        this->is_recording_observations = true;
    }

    void MissionRecordSpec::recordRewards()
    {
        this->is_recording_rewards = true;
    }

    void MissionRecordSpec::recordCommands()
    {
        this->is_recording_commands = true;
    }

    bool MissionRecordSpec::isRecording() const
    {
        return !this->destination.empty()
            && (this->is_recording_commands
            || this->is_recording_mp4
            || this->is_recording_rewards
            || this->is_recording_observations);
    }

    std::ostream& operator<<(std::ostream& os, const MissionRecordSpec& msp)
    {
        os << "MissionRecordSpec: ";
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
