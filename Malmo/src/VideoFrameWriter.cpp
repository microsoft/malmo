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
#include "VideoFrameWriter.h"
#include "Logger.h"

#if WIN32
#include "WindowsFrameWriter.h"
#else
#include "PosixFrameWriter.h"
#endif

// STL:
#include <exception>
#include <sstream>

#define LOG_COMPONENT Logger::LOG_VIDEO

namespace malmo
{
    VideoFrameWriter::VideoFrameWriter(std::string path, std::string frame_info_filename, short width, short height, int frames_per_second, int channels, bool drop_input_frames)
        : path(path)
        , width(width)
        , height(height)
        , frames_per_second(frames_per_second)
        , drop_input_frames(drop_input_frames)
        , channels(channels)
        , is_open(false)
        , frame_duration(boost::posix_time::milliseconds(1000) / frames_per_second)
    {
        boost::filesystem::path fs_path(path);
        if (boost::filesystem::is_directory(fs_path)) {
            this->frame_info_path = fs_path / frame_info_filename;
        }
        else {
            this->frame_info_path = fs_path.parent_path() / frame_info_filename;
        }
    }

    VideoFrameWriter::~VideoFrameWriter()
    {
        this->close();
    }

    void VideoFrameWriter::open()
    {
        this->close();

        // Create helpful script:
        boost::filesystem::path fs_path(this->path);
        std::string ffmpeg_helpfile = (fs_path.parent_path() / (fs_path.stem().string() + "_to_pngs.sh")).string();
        std::ofstream helpfile(ffmpeg_helpfile);
        helpfile << "#! To extract individual frames from the mp4\n";
        helpfile << "mkdir " << fs_path.stem().string() << "_frames\n";
        helpfile << "ffmpeg -i " << fs_path.filename() << " " << fs_path.stem().string() << "_frames/frame_%06d.png\n";

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
        LOGSECTION(LOG_FINE, "In VideoFrameWriter::close()...");
        if (this->is_open) {
            this->frame_info_stream.close();

            this->is_open = false;
            LOGFINE(LT("Set is_open to false"));
            {
                boost::lock_guard<boost::mutex> frames_available_guard(this->frames_available_mutex);

                this->frames_available = true;
            }

            LOGFINE(LT("Notifying worker thread that frames are available, in order to close."));
            this->frames_available_cond.notify_one();
            LOGFINE(LT("Waiting for worker thread to join."));
            this->frame_writer_thread.join();
            LOGFINE(LT("Worker thread joined."));
            LOGFINE(LT("Frames received for writing: "), this->frame_index);
            LOGFINE(LT("Frames actually written: "), this->frames_actually_written);
        }
    }

    void VideoFrameWriter::writeFrames()
    {
        this->frames_actually_written = 0;
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

                try
                {
                    writeSingleFrame(frame, this->frames_actually_written);
                    this->frames_actually_written++;
                }
                catch (std::exception& e)
                {
                    LOGERROR(LT("Failed to write frame: "), e.what());
                }
            }
        }
    }

    void VideoFrameWriter::writeSingleFrame(const TimestampedVideoFrame& frame, int count)
    {
        LOGTRACE(LT("Writing frame "), count + 1, LT(", "), frame.width, LT("x"), frame.height, LT("x"), frame.channels);
        if (frame.channels == 4)
        {
            if (frame.frametype == TimestampedVideoFrame::DEPTH_MAP)
            {
                // For making videos out of 32bpp depth maps, what exactly should we display?
                // We could reduce to greyscale, but that way we loose a lot of precision.
                // Instead, convert to an HSV colour cone, which hopefully gives a greater range
                // of colour values to map to.
                const float* fPixels = reinterpret_cast<const float*>(&(frame.pixels[0]));
                char *out_pixels = new char[frame.width * frame.height * 3];
                for (int i = 0; i < frame.width*frame.height; i++)
                {
                    float f = fPixels[i];
                    float h = 60.0f * f;
                    while (h >= 360.0)
                        h -= 360.0;
                    float s = 1.0;
                    float v = 1.0f - (f / 200.0f);
                    if (v < 0)
                        v = 0;
                    if (v > 1.0)
                        v = 1.0;
                    h = h / 60.0f;
                    float fract = h - floor(h);

                    v *= 255.0;

                    float p = v*(1.0f - s);
                    float q = v*(1.0f - s*fract);
                    float t = v*(1.0f - s*(1.0f - fract));

                    unsigned int out;
                    if (0. <= h && h < 1.)
                        out = int(v) + (int(t) << 8) + (int(p) << 16);
                    else if (1. <= h && h < 2.)
                        out = int(q) + (int(v) << 8) + (int(p) << 16);
                    else if (2. <= h && h < 3.)
                        out = int(p) + (int(v) << 8) + (int(t) << 16);
                    else if (3. <= h && h < 4.)
                        out = int(p) + (int(q) << 8) + (int(v) << 16);
                    else if (4. <= h && h < 5.)
                        out = int(t) + (int(p) << 8) + (int(v) << 16);
                    else if (5. <= h && h < 6.)
                        out = int(v) + (int(p) << 8) + (int(q) << 16);
                    else
                        out = 0;

                    out_pixels[3 * i] = out & 0xff;
                    out_pixels[3 * i + 1] = (out >> 8) & 0xff;
                    out_pixels[3 * i + 2] = (out >> 16) & 0xff;
                }
                this->doWrite(out_pixels, frame.width, frame.height, count);
                delete[] out_pixels;
            }
            else
            {
                // extract DDD from RGBD
                char *out_pixels = new char[frame.width * frame.height * 3];
                for (int i = 0; i < frame.width*frame.height; i++)
                {
                    out_pixels[i * 3] = out_pixels[i * 3 + 1] = out_pixels[i * 3 + 2] = frame.pixels[i * 4 + 3];
                }
                this->doWrite(out_pixels, frame.width, frame.height, count);
                delete[] out_pixels;
            }
        }
        else if (frame.channels == 3 || frame.channels == 1)
        {
            // write the pixel data directly
            this->doWrite((char*)&frame.pixels[0], frame.width, frame.height, count);
        }
        else throw std::runtime_error("Unsupported number of channels");
    }

    bool VideoFrameWriter::write(TimestampedVideoFrame frame)
    {
        boost::lock_guard<boost::mutex> write_guard(this->write_mutex);

        if (!this->drop_input_frames || frame.timestamp - this->last_timestamp >= this->frame_duration) {
            this->last_timestamp = frame.timestamp;

            std::stringstream name;
            name << "frame_" << std::setfill('0') << std::setw(6) << this->frame_index + 1;
            std::stringstream posdata;
            posdata << "xyzyp: " << frame.xPos << " " << frame.yPos << " " << frame.zPos << " " << frame.yaw << " " << frame.pitch;
            this->frame_info_stream << boost::posix_time::to_iso_string(frame.timestamp) << " " << name.str() << " " << posdata.str() << std::endl;

            this->frame_index++;
            {
                boost::lock_guard<boost::mutex> buffer_guard(this->frame_buffer_mutex);

                LOGTRACE(LT("Pushing frame "), this->frame_index, LT(", "), frame.width, LT("x"), frame.height, LT("x"), frame.channels, LT(" to write buffer."));
                this->frame_buffer.push(frame);
            }

            {
                boost::lock_guard<boost::mutex> frames_available_guard(this->frames_available_mutex);

                this->frames_available = true;
            }

            this->frames_available_cond.notify_one();
            return true;
        }
        return false;
    }

    std::unique_ptr<VideoFrameWriter> VideoFrameWriter::create(std::string path, std::string info_filename, short width, short height, int frames_per_second, int64_t bit_rate, int channels, bool drop_input_frames)
    {
#if WIN32
        std::unique_ptr<VideoFrameWriter> instance( new WindowsFrameWriter(path, info_filename, width, height, frames_per_second, bit_rate, channels, drop_input_frames) );
#else
        std::unique_ptr<VideoFrameWriter> instance( new PosixFrameWriter(path, info_filename, width, height, frames_per_second, bit_rate, channels, drop_input_frames) );
#endif
        return instance;
    }
}

#undef LOG_COMPONENT
