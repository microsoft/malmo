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

#ifndef _VIDEOFRAMEWRITER_H_
#define _VIDEOFRAMEWRITER_H_

// Local:
#include "TimestampedVideoFrame.h"

// Boost:
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/filesystem.hpp>
#include <boost/thread.hpp>

// STL:
#include <string>
#include <fstream>
#include <iostream>
#include <queue>

namespace malmo
{
    class VideoFrameWriter
    {
    public:
        VideoFrameWriter(std::string path, short width, short height, int frames_per_second);
        virtual ~VideoFrameWriter();
        virtual void open();
        virtual void close();
        
        void write(TimestampedVideoFrame frame);
        bool isOpen() const;

        static std::unique_ptr<VideoFrameWriter> create(std::string path, short width, short height, int frames_per_second, int64_t bit_rate);

    protected:
        virtual void doWrite(char* rgb, int width, int height, int frame_index) = 0;

        std::string path;
        short width;
        short height;
        int frames_per_second;
        bool is_open;

    private:
        void writeFrames();

        boost::posix_time::ptime start_time;
        boost::posix_time::ptime last_timestamp;
        boost::posix_time::time_duration frame_duration;
        std::ofstream frame_info_stream;
        boost::filesystem::path frame_info_path;
        int frame_index;

        std::queue<TimestampedVideoFrame> frame_buffer;
        boost::mutex write_mutex;
        boost::mutex frame_buffer_mutex;
        boost::mutex frames_available_mutex;
        boost::condition_variable frames_available_cond;
        bool frames_available;
        boost::thread frame_writer_thread;
    };
}

#endif
