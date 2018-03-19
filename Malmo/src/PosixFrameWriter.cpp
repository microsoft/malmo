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
#include "PosixFrameWriter.h"
#include "Logger.h"

// Boost:
#include <boost/filesystem.hpp>

// POSIX:
#include <fcntl.h>
#include <sys/wait.h>
#include <unistd.h>

// STL:
#include <exception>
#include <sstream>

#define LOG_COMPONENT Logger::LOG_VIDEO

namespace malmo
{
    std::stack<PosixFrameWriter::pid_fd> PosixFrameWriter::child_process_stack;
    std::vector<PosixFrameWriter::pid_fd> PosixFrameWriter::child_processes_pending_deletion;

    PosixFrameWriter::PosixFrameWriter(std::string path, std::string info_filename, short width, short height, int frames_per_second, int64_t bit_rate, int channels, bool drop_input_frames)
        : VideoFrameWriter(path, info_filename, width, height, frames_per_second, channels, drop_input_frames)
        , bit_rate(bit_rate)
        , process_id(0)
    {
        this->ffmpeg_path = search_path();
        if (this->ffmpeg_path.length() == 0) {
            throw std::runtime_error( "FFMPEG not available. For .mp4 recording, install ffmpeg (or libav-tools)." );
        }

        int ret = pipe( this->pipe_fd );
        if( ret )
            throw std::runtime_error( "Failed to create pipe." );
    }

    void PosixFrameWriter::open()
    {
        VideoFrameWriter::open();
        
        this->process_id = fork();
        if( this->process_id < 0 )
            throw std::runtime_error( "Failed to fork." );
        
        int ret;

        if( this->process_id ) 
        {
            // this is the parent process, can write to pipe_fd[1]
            // push our child's proc id onto the stack:
            child_process_stack.push(std::make_pair(this->process_id, this->pipe_fd[1]));
            ret = ::close( this->pipe_fd[0] ); // close the end of the pipe we don't want to use
            if( ret )
                throw std::runtime_error( "Failed to close unused pipe end." );
        } 
        else 
        {
            // this is the child process, replace it with ffmpeg
            
            ret = dup2( this->pipe_fd[0], 0 );     // map stdin to pipe_fd[0]
            if( ret < 0 )
                throw std::runtime_error( "Failed to map stdin to pipe." );

            ret = ::close( this->pipe_fd[1] ); // close the end of the pipe we don't want to use
            if( ret )
                throw std::runtime_error( "Failed to close unused pipe end." );

            // send ffmpeg's output to file
            {
                boost::filesystem::path fs_path(this->path);
                std::string ffmpeg_outfile = (fs_path.parent_path() / (fs_path.stem().string() + "_ffmpeg.out")).string();

                int out_fd = ::open(ffmpeg_outfile.c_str(), O_WRONLY | O_CREAT, S_IRUSR | S_IWUSR);
                if( out_fd < 0 )
                    throw std::runtime_error( "Failed to open ffmpeg.out for writing." );
                    
                ret = dup2(out_fd, 1); // stdout
                if( ret < 0 )
                    throw std::runtime_error( "Failed to map ffmpeg's stdout to file." );
                    
                ret = dup2(out_fd, 2); // stderr
                if( ret < 0 )
                    throw std::runtime_error( "Failed to map ffmpeg's stderr to file." );

                ret = ::close(out_fd); 
                if( ret )
                    throw std::runtime_error( "Failed to close ffmpeg.out file descriptor." );
            }

            std::string input_format = this->channels == 1 ? "pgm" : "ppm";

            ret = execlp( this->ffmpeg_path.c_str(), 
                          this->ffmpeg_path.c_str(),
                          "-y",
                          "-f",
                          "image2pipe",
                          "-framerate",
                          std::to_string(this->frames_per_second).c_str(),
                          "-vcodec",
                          input_format.c_str(),
                          "-i",
                          "-",
                          "-vcodec",
                          "libx264",
                          "-b:v",
                          std::to_string(this->bit_rate).c_str(),
                          "-pix_fmt",
                          "yuv420p",
                          this->path.c_str(),
                          NULL
                    );
            if( ret )
                throw std::runtime_error( "Call to execlp failed." );
        }
    }

    PosixFrameWriter::~PosixFrameWriter()
    {
        LOGFINE(LT("Destructing PosixFrameWriter - calling close()"));
        this->close();
    }

    void PosixFrameWriter::close()
    {
        LOGFINE(LT("In PosixFrameWriter::close()"));
        if (this->is_open)
        {
            VideoFrameWriter::close();
        }

        // if the parent process then close the pipe and wait for ffmpeg to finish
        // it's VERY important we do this in reverse order of creation, since later child processes
        // will be holding handles to previous child processes (they inherit the full fd table from the
        // parent.) Closing them first ensures that all handles have been closed by the time the first
        // child processes are closed.
        if (this->process_id)
        {
            LOGFINE(LT("Parent PosixFrameWriter process requesting pipe close - fd: "), this->pipe_fd[1], LT(" pid: "), this->process_id);
            child_processes_pending_deletion.push_back(std::make_pair(this->process_id, this->pipe_fd[1]));
            this->process_id = 0;
            close_pending_children();
        }
    }

    void PosixFrameWriter::close_pending_children()
    {
        // we can only close the process at the top of the stack.
        while (child_process_stack.size() && std::find(child_processes_pending_deletion.begin(), child_processes_pending_deletion.end(), child_process_stack.top()) != child_processes_pending_deletion.end())
        {
            pid_fd child = child_process_stack.top();
            child_process_stack.pop();
            LOGFINE(LT("Parent PosixFrameWriter process is closing pipe - fd: "), child.second, LT(" pid: "), child.first);
            int ret = ::close(child.second);
            if (ret)
            {
                LOGERROR(LT("Failed to close pipe: "), ret);
                throw std::runtime_error("Failed to close the pipe.");
            }

            int status;
            LOGFINE(LT("Pipe closed, waiting for ffmpeg to end..."));
            ret = waitpid( child.first, &status, 0 );
            if (ret != child.first)
            {
                LOGERROR(LT("Call to waitpid failed: "), ret);
                throw std::runtime_error("Call to waitpid failed.");
            }
            if (!WIFEXITED(status))
            {
                LOGERROR(LT("FFMPEG process exited abnormally: "), status);
                throw std::runtime_error("FFMPEG process exited abnormally.");
            }
        }
    }

    void PosixFrameWriter::doWrite(char* rgb, int width, int height, int frame_index)
    {
        std::string magic_number = this->channels == 1 ? "P5" : "P6";
        std::ostringstream oss;
        oss << magic_number << "\n" << width << " " << height << "\n255\n";
        ssize_t ret = ::write( this->pipe_fd[1], oss.str().c_str(), oss.str().size() );
        if (ret < 0)
        {
            LOGERROR(LT("Failed to write frame header: "), std::strerror(errno), LT(" - throwing runtime_error"));
            throw std::runtime_error("Call to write failed.");
        }

        ret = ::write( this->pipe_fd[1], rgb, width*height*this->channels );
        if (ret < 0)
        {
            LOGERROR(LT("Failed to write frame body: "), std::strerror(errno), LT(" - throwing runtime_error"));
            throw std::runtime_error("Call to write failed.");
        }
    }

    std::string PosixFrameWriter::search_path()
    {
        std::string path = ::getenv("PATH");
        if (path.empty())
        {
            throw std::runtime_error("Environment variable PATH not found");
        }

        std::stringstream path_stream(path);
        std::string folder_path;
        
        std::string ffmpeg_names[2] = { "ffmpeg", "avconv" };

        while (std::getline(path_stream, folder_path, ':')) {
            for( auto ffmpeg_name : ffmpeg_names ) {
                boost::filesystem::path executable_path = folder_path;
                executable_path /= ffmpeg_name;

                boost::system::error_code ec;
                bool file = boost::filesystem::is_regular_file(executable_path, ec);
                bool is_executable = !access( executable_path.string().c_str(), X_OK );
                if (!ec && file && is_executable)
                {
                    return executable_path.string();
                }
            }
        }

        return "";
    }
}

#undef LOG_COMPONENT
