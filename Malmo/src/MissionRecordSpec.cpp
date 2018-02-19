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
        : is_recording_observations(false)
        , is_recording_rewards(false)
        , is_recording_commands(false)
    {
    }

    MissionRecordSpec::MissionRecordSpec(std::string destination)
        : is_recording_observations(false)
        , is_recording_rewards(false)
        , is_recording_commands(false)
    {
        setDestination(destination);
    }

    void MissionRecordSpec::setDestination(const std::string& destination)
    {
        boost::filesystem::path filepath = boost::filesystem::absolute(destination);
        std::ofstream file(filepath.string(), std::ofstream::binary);
        if (file.fail()) {
            std::cout << "ERROR: Cannot write to " << filepath.string() << " - check the path exists and you have permission to write there." << std::endl;
            throw std::runtime_error("Can not write to recording destination.");
        }
        this->destination = filepath.string();
    }

    void MissionRecordSpec::recordMP4(int frames_per_second, int64_t bit_rate)
    {
        // Set spec for all video producers:
        this->video_recordings.clear();
        for (int ftype = TimestampedVideoFrame::_MIN_FRAME_TYPE; ftype < TimestampedVideoFrame::_MAX_FRAME_TYPE; ftype++)
        {
            FrameRecordingSpec fspec;
            fspec.fr_type = VIDEO;
            fspec.mp4_bit_rate = bit_rate;
            fspec.mp4_fps = frames_per_second;
            fspec.drop_input_frames = true; // nasty behaviour, but preserved for backwards compatibility
            this->video_recordings[(TimestampedVideoFrame::FrameType)ftype] = fspec;
        }
    }

    void MissionRecordSpec::recordMP4(TimestampedVideoFrame::FrameType type, int frames_per_second, int64_t bit_rate, bool drop_input_frames)
    {
        FrameRecordingSpec fspec;
        fspec.fr_type = VIDEO;
        fspec.mp4_bit_rate = bit_rate;
        fspec.mp4_fps = frames_per_second;
        fspec.drop_input_frames = drop_input_frames;
        this->video_recordings[type] = fspec;
    }

    void MissionRecordSpec::recordBitmaps(TimestampedVideoFrame::FrameType type)
    {
        FrameRecordingSpec fspec;
        fspec.fr_type = BMP;
        this->video_recordings[type] = fspec;
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
            || this->video_recordings.size()
            || this->is_recording_rewards
            || this->is_recording_observations);
    }

    std::ostream& operator<<(std::ostream& os, const MissionRecordSpec& msp)
    {
        os << "MissionRecordSpec: ";
        if (msp.is_recording_observations)
            os << "\n  -observations";
        if (msp.is_recording_rewards)
            os << "\n  -rewards";
        if (msp.is_recording_commands)
            os << "\n  -commands";
        for (auto r : msp.video_recordings)
        {
            os << "\n  -" << r.first << ": ";
            os << (r.second.fr_type == MissionRecordSpec::BMP ? "bitmaps" : "mp4");
            if (r.second.fr_type == MissionRecordSpec::VIDEO)
                os << " (bitrate: " << r.second.mp4_bit_rate << ", fps: " << r.second.mp4_fps << ")";
        }
        if (msp.destination.length())
            os << "\n to: " << msp.destination;

        return os;
    }
}
