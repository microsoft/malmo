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

// POSIX:
#include <fcntl.h>
#include <sys/wait.h>
#include <unistd.h>

// STL:
#include <exception>
#include <sstream>

namespace malmo
{
    PosixFrameWriter::PosixFrameWriter(std::string path, std::string info_filename, short width, short height, int frames_per_second, int64_t bit_rate)
        : VideoFrameWriter(path, info_filename, width, height, frames_per_second)
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
                int out_fd = ::open("./ffmpeg.out", O_WRONLY | O_CREAT, S_IRUSR | S_IWUSR);
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

            ret = execlp( this->ffmpeg_path.c_str(), 
                          this->ffmpeg_path.c_str(),
                          "-y",
                          "-f",
                          "image2pipe",
                          "-framerate",
                          std::to_string(this->frames_per_second).c_str(),
                          "-vcodec",
                          "ppm",
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
        this->close();
    }

    void PosixFrameWriter::close()
    {
        if (this->is_open)
        {
            VideoFrameWriter::close();
        }
        
        // if the parent process then close the pipe and wait for ffmpeg to finish
        if( this->process_id ) 
        {
            int ret = ::close( this->pipe_fd[1] );
            if( ret )
                throw std::runtime_error( "Failed to close the pipe." );

            int status;
            ret = waitpid( this->process_id, &status, 0 );
            if( ret != this->process_id )
                throw std::runtime_error( "Call to waitpid failed." );
            if( !WIFEXITED( status ) )
                throw std::runtime_error( "FFMPEG process exited abnormally." );

            this->process_id = 0;
        }
    }

    void PosixFrameWriter::doWrite(char* rgb, int width, int height, int frame_index)
    {
        std::ostringstream oss;
        oss << "P6\n" << width << " " << height << "\n255\n";
        ssize_t ret = ::write( this->pipe_fd[1], oss.str().c_str(), oss.str().size() );
        if( ret < 0 )
            throw std::runtime_error( "Call to write failed." );

        ret = ::write( this->pipe_fd[1], rgb, width*height*3 );
        if( ret < 0 )
            throw std::runtime_error( "Call to write failed." );
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
