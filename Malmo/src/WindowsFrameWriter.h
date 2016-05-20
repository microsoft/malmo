// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _WINDOWSFRAMEWRITER_H_
#define _WINDOWSFRAMEWRITER_H_

// Local:
#include "VideoFrameWriter.h"

// Boost:
#include <boost/thread.hpp>

// STL:
#include <fstream>
#include <iostream>

// Windows:
#include <windows.h>

namespace malmo
{
    class WindowsFrameWriter : public VideoFrameWriter
    {
    public:

        WindowsFrameWriter(std::string path, short width, short height, int frames_per_second, int64_t bit_rate = 400000);
        ~WindowsFrameWriter();
        void open() override;
        void close() override;
        void doWrite(TimestampedVideoFrame message, int frame_index) override;

    private:
        void runFFMPEG();
        std::string search_path();

        int64_t bit_rate;
        std::string ffmpeg_path;

        boost::thread ffmpeg_thread;

        HANDLE g_hChildStd_IN_Rd;
        HANDLE g_hChildStd_IN_Wr;
        HANDLE g_hChildStd_OUT_Wr;
    };
}

#endif
