// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _POSIXFRAMEWRITER_H_
#define _POSIXFRAMEWRITER_H_

// Local:
#include "VideoFrameWriter.h"

namespace malmo
{
    class PosixFrameWriter : public VideoFrameWriter
    {
    public:

        PosixFrameWriter(std::string path, short width, short height, int frames_per_second, int64_t bit_rate = 400000);
        ~PosixFrameWriter();
        void open() override;
        void close() override;
        void doWrite(TimestampedVideoFrame message, int frame_index) override;

    private:
        std::string search_path();
        
        int64_t bit_rate;

        std::string ffmpeg_path;
        
        int pipe_fd[2];
        pid_t process_id;
    };
}

#endif
