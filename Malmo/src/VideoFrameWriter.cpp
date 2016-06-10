// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "VideoFrameWriter.h"

#if WIN32
#include "WindowsFrameWriter.h"
#else
#include "PosixFrameWriter.h"
#endif

// STL:
#include <exception>
#include <sstream>

namespace malmo
{
    VideoFrameWriter::VideoFrameWriter(std::string path, short width, short height, int frames_per_second)
        : path(path)
        , width(width)
        , height(height)
        , frames_per_second(frames_per_second)
        , is_open(false)
        , frame_duration(boost::posix_time::milliseconds(1000) / frames_per_second)
    {
        boost::filesystem::path fs_path(path);
        if (boost::filesystem::is_directory(fs_path)) {
            this->frame_info_path = fs_path / "frame_info.txt";
        }
        else {
            this->frame_info_path = fs_path.parent_path() / "frame_info.txt";
        }
    }

    VideoFrameWriter::~VideoFrameWriter()
    {
        this->close();
    }

    void VideoFrameWriter::open()
    {
        this->close();

        this->frame_info_stream.open(this->frame_info_path.string());

        this->frame_info_stream << "width=" << this->width << std::endl;
        this->frame_info_stream << "height=" << this->height << std::endl;

        this->is_open = true;

        this->start_time = boost::posix_time::microsec_clock::universal_time();
        this->last_timestamp = this->start_time - this->frame_duration;

        this->frame_index = 0;

        this->frames_available = false;
        this->frame_writer_thread = boost::thread(&VideoFrameWriter::writeFrames, this);
    }

    bool VideoFrameWriter::isOpen() const
    {
        return this->is_open;
    }

    void VideoFrameWriter::close()
    {
        if (this->is_open) {
            this->frame_info_stream.close();

            this->is_open = false;

            {
                boost::lock_guard<boost::mutex> frames_available_guard(this->frames_available_mutex);

                this->frames_available = true;
            }

            this->frames_available_cond.notify_one();

            this->frame_writer_thread.join();
        }
    }

    void VideoFrameWriter::writeFrames()
    {
        int count = 0;
        while (this->is_open) {
            {
                boost::unique_lock<boost::mutex> lock(this->frames_available_mutex);

                while (!this->frames_available) {
                    this->frames_available_cond.wait(lock);
                }
            }

            while (true) {
                TimestampedVideoFrame frame;
                {
                    boost::lock_guard<boost::mutex> buffer_guard(this->frame_buffer_mutex);

                    if (this->frame_buffer.size() > 0) {
                        frame = this->frame_buffer.front();
                        this->frame_buffer.pop();
                    }
                }

                if (frame.width == 0) {
                    boost::lock_guard<boost::mutex> frames_available_guard(this->frames_available_mutex);

                    this->frames_available = false;
                    break;
                }

                if (frame.channels == 4)
                {
                    // extract DDD from RGBD
                    // TODO: support other options, output multiple videos
                    const DWORD num_out_bytes = frame.width * frame.height * 3;
                    char *out_pixels = new char[num_out_bytes];
                    int j = 0;
                    for (int i = 0; i < frame.width*frame.height; i++)
                    {
                        out_pixels[j++] = frame.pixels[i * 4 + 3];
                        out_pixels[j++] = frame.pixels[i * 4 + 3];
                        out_pixels[j++] = frame.pixels[i * 4 + 3];
                    }
                    this->doWrite(out_pixels, frame.width, frame.height, count);

                    delete[]out_pixels;
                }
                else if (frame.channels == 3)
                {
                    // write the RGB data directly
                    this->doWrite((char*)&frame.pixels[0], frame.width, frame.height, count);
                }
                else throw std::runtime_error("Unsupported number of channels");

                count++;
            }
        }
    }

    void VideoFrameWriter::write(TimestampedVideoFrame frame)
    {
        boost::lock_guard<boost::mutex> write_guard(this->write_mutex);

        if (frame.timestamp - this->last_timestamp >= this->frame_duration) {
            this->last_timestamp = frame.timestamp;

            std::stringstream name;
            name << "frame_" << std::setfill('0') << std::setw(6) << this->frame_index;
            this->frame_info_stream << boost::posix_time::to_iso_string(frame.timestamp) << " " << name.str() << std::endl;

            this->frame_index++;

            {
                boost::lock_guard<boost::mutex> buffer_guard(this->frame_buffer_mutex);

                this->frame_buffer.push(frame);
            }

            {
                boost::lock_guard<boost::mutex> frames_available_guard(this->frames_available_mutex);

                this->frames_available = true;
            }

            this->frames_available_cond.notify_one();
        }
    }

    std::unique_ptr<VideoFrameWriter> VideoFrameWriter::create(std::string path, short width, short height, int frames_per_second, int64_t bit_rate)
    {
#if WIN32
        std::unique_ptr<VideoFrameWriter> instance( new WindowsFrameWriter(path, width, height, frames_per_second, bit_rate) );
#else
        std::unique_ptr<VideoFrameWriter> instance( new PosixFrameWriter(path, width, height, frames_per_second, bit_rate) );
#endif
        return instance;
    }
}
