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

#ifndef _POSIXFRAMEWRITER_H_
#define _POSIXFRAMEWRITER_H_

// Local:
#include "VideoFrameWriter.h"

namespace malmo
{
    class PosixFrameWriter : public VideoFrameWriter
    {
    public:

        PosixFrameWriter(std::string path, std::string info_filename, short width, short height, int frames_per_second, int64_t bit_rate = 400000, int channels = 3, bool drop_input_frames = false);
        ~PosixFrameWriter();
        void open() override;
        void close() override;

    private:
        void doWrite(char* rgb, int width, int height, int frame_index) override;
        std::string search_path();
        
        int64_t bit_rate;
        std::string ffmpeg_path;
        
        int pipe_fd[2];
        pid_t process_id;

        typedef std::pair<pid_t, int> pid_fd;
        static std::stack<pid_fd> child_process_stack;
        static std::vector<pid_fd> child_processes_pending_deletion;
        static void close_pending_children();
    };
}

#endif
