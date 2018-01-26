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

#ifndef _MISSIONRECORDSPEC_H_
#define _MISSIONRECORDSPEC_H_

// Boost:
#include <boost/filesystem.hpp>

// STL:
#include <cstdint>
#include <fstream>
#include <string>
#include <vector>

// Local:
#include "Logger.h"
#include "TimestampedVideoFrame.h"

#define LOG_COMPONENT Logger::LOG_RECORDING

namespace malmo
{
    //! Specifies the type of data that should be recorded from the mission.
    struct MissionRecordSpec
    {
        MALMO_LOGGABLE_OBJECT(MissionRecordSpec)
        friend class MissionRecord;
        
    public:

        //! Constructs an empty mission record specification, saying that nothing should be recorded.
        MissionRecordSpec();

        //! Constructs a mission record with a target file (e.g. 'data.tgz').
        //! By default, nothing is recorded. Use the other functions to specify what channels should be recorded.
        //! WARNING: You cannot re-use the instance of MissionRecordSpec - make a new one per call to AgentHost.startMission.
        //! \param destination Filename to save to.
        MissionRecordSpec(std::string destination);

        //! Specifies the destination for the recording.
        void setDestination(const std::string& destination);

        //! Requests that video be recorded, for *each* video producer, at the specified quality.
        //! Ensure that the width of the video requested is divisible by 4, and the height of the video requested is divisible by 2.
        //! Bitmaps and MP4 cannot both be recorded for a given video producer;
        //! whichever is called last out of recordMP4 and recordBitmaps will take effect.
        //! \param frames_per_second The number of frames to record per second. e.g. 20.
        //! \param bit_rate The bit rate to record at. e.g. 400000 for 400kbps.
        void recordMP4(int frames_per_second, int64_t bit_rate);

        //! Requests that video be recorded, for the specified video producer, at the specified quality.
        //! Ensure that the width of the video requested is divisible by 4, and the height of the video requested is divisible by 2.
        //! \param frames_per_second The number of frames to output per second. e.g. 24.
        //! \param bit_rate The bit rate to record at. e.g. 400000 for 400kbps.
        //! \param drop_input_frames If true, will drop input frames to match frames_per_second (default behaviour) - pass false to avoid losing data.
        //! Bitmaps and MP4 cannot both be recorded for a given video producer;
        //! whichever is called last out of recordMP4 and recordBitmaps will take effect.
        void recordMP4(TimestampedVideoFrame::FrameType type, int frames_per_second, int64_t bit_rate, bool drop_input_frames);

        //! Requests that video be recorded, for the specified video producer, in individual bitmap frames.
        //! Bitmaps and MP4 cannot both be recorded for a given video producer;
        //! whichever is called last out of recordMP4 and recordBitmaps will take effect.
        void recordBitmaps(TimestampedVideoFrame::FrameType type);

        //! Requests that observations be recorded.
        void recordObservations();

        //! Requests that rewards be recorded.
        void recordRewards();

        //! Requests that commands be recorded.
        void recordCommands();

        //! Are we recording anything?
        bool isRecording() const;

        friend std::ostream& operator<<(std::ostream& os, const MissionRecordSpec& msp);

    private:
        enum FrameRecordingType
        {
            BMP,
            VIDEO
        };
        struct FrameRecordingSpec
        {
            FrameRecordingType fr_type;
            int64_t mp4_bit_rate;
            int mp4_fps;
            bool drop_input_frames;
        };
        std::map<TimestampedVideoFrame::FrameType, FrameRecordingSpec> video_recordings;
        bool is_recording_observations;
        bool is_recording_rewards;
        bool is_recording_commands;
        std::string destination;
    };
}

#undef LOG_COMPONENT
#endif
