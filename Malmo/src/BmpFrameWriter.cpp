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
#include "BmpFrameWriter.h"
#include "Tarball.hpp"

// STL:
#include <exception>
#include <sstream>
#include <stdio.h>

#include <boost/iostreams/filter/gzip.hpp>
#include <boost/iostreams/filtering_stream.hpp>
#include <boost/iostreams/stream.hpp>

namespace malmo
{
    class TarHelper
    {
    public:
        TarHelper(boost::filesystem::path path, std::size_t max_tar_size = 1 << 30) : path(path), frame_count(0), max_tar_size(max_tar_size), unzipped_byte_count(0), compression_level(6)
        {
            // For now, get this from the environment:
            char *compression_env = getenv("MALMO_BMP_COMPRESSION_LEVEL");
            if (compression_env) {
                this->compression_level = std::min(std::max(atoi(compression_env), 0), 9);
            }
        }

        ~TarHelper()
        {
            if (this->tarball)
                this->tarball->finish();
            if (this->filter)
                this->filter->pop();
        }

        void addFrame(const TimestampedVideoFrame& frame)
        {
            if (!this->tarball || frame.pixels.size() + this->unzipped_byte_count > this->max_tar_size)
                reset();

            std::stringstream frame_name;
            std::string extension = (frame.channels == 1) ? ".pgm" : ".ppm";
            frame_name << "frame_" << std::setfill('0') << std::setw(6) << this->frame_count << extension;

            std::stringstream frame_header;
            std::string magic_number = (frame.channels == 1) ? "P5" : "P6";
            frame_header << magic_number << "\n" << frame.width << " " << frame.height << "\n255\n";
            std::string header = frame_header.str();

            this->tarball->putMemWithHeader(header.c_str(), header.length(), reinterpret_cast<const char*>(&(frame.pixels[0])), frame.pixels.size(), frame_name.str().c_str());
            this->unzipped_byte_count += header.length() + frame.pixels.size();
            this->frame_count++;
        }

        int getFrameCount() const { return this->frame_count; }

    private:
        void reset()
        {
            if (this->tarball)
                this->tarball->finish();
            if (this->filter)
                this->filter->pop();
            std::stringstream tarname;
            tarname << "bmps_" << std::setfill('0') << std::setw(6) << this->frame_count << ".tar.gz";
            boost::filesystem::path tarpath = this->path / tarname.str();
            this->backing_file = boost::make_shared<std::ofstream>(tarpath.string(), std::fstream::binary);
            this->filter = boost::make_shared<boost::iostreams::filtering_ostream>();
            boost::iostreams::gzip_params params;
            params.level = this->compression_level;
            this->filter->push(boost::iostreams::gzip_compressor(params));
            this->filter->push(*(this->backing_file));
            this->tarball = boost::make_shared<lindenb::io::Tar>(*(this->filter));
            this->unzipped_byte_count = 0;
            std::cout << tarname.str() << " created." << std::endl;
        }

        int frame_count;
        int compression_level;  // gzip compression level - 1=best speed, 9=best compression (6 is currently default according to zlib docs)
        std::size_t max_tar_size;
        std::size_t unzipped_byte_count;
        boost::filesystem::path path;
        boost::shared_ptr<lindenb::io::Tar> tarball;
        boost::shared_ptr<boost::iostreams::filtering_ostream> filter;
        boost::shared_ptr<std::ofstream> backing_file;
    };

    BmpFrameWriter::BmpFrameWriter(std::string path, std::string frame_info_filename)
        : path(path)
        , is_open(false)
    {
        boost::filesystem::path fs_path(path);
        if (!boost::filesystem::exists(fs_path)) {
            boost::filesystem::create_directories(fs_path);
        }
        this->frame_info_path = fs_path / frame_info_filename;
        this->frames_path = fs_path / std::string("bmps.tar");
    }

    BmpFrameWriter::~BmpFrameWriter()
    {
        this->close();
    }

    void BmpFrameWriter::open()
    {
        this->close();

        this->frame_info_stream.open(this->frame_info_path.string());

        this->is_open = true;

        this->start_time = boost::posix_time::microsec_clock::universal_time();
        this->last_timestamp = this->start_time - this->frame_duration;

        this->frame_index = 0;

        this->frames_available = false;
        this->frame_writer_thread = boost::thread(&BmpFrameWriter::writeFrames, this);
    }

    bool BmpFrameWriter::isOpen() const
    {
        return this->is_open;
    }

    void BmpFrameWriter::close()
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

    void BmpFrameWriter::writeFrames()
    {
        TarHelper tarrer(boost::filesystem::path(this->path));
        while (this->is_open) {
            // Wait for frames to become available
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
                        //std::cout << "Frames in queue: " << this->frame_buffer.size() << std::endl;
                    }
                    else {
                        boost::lock_guard<boost::mutex> frames_available_guard(this->frames_available_mutex);
                        this->frames_available = false;
                        break;
                    }
                }
                // Write frame into tarball:
                tarrer.addFrame(frame);
                // And add details to index files:
                std::stringstream name;
                name << "frame_" << std::setfill('0') << std::setw(6) << tarrer.getFrameCount();
                std::stringstream posdata;
                posdata << "xyzyp: " << frame.xPos << " " << frame.yPos << " " << frame.zPos << " " << frame.yaw << " " << frame.pitch;
                this->frame_info_stream << boost::posix_time::to_iso_string(frame.timestamp) << " " << name.str() << " " << posdata.str() << std::endl;
            }
        }
        this->frame_info_stream.flush();
    }

    bool BmpFrameWriter::write(TimestampedVideoFrame frame)
    {
        this->last_timestamp = frame.timestamp;
        bool addedFrame = false;
        {
            boost::lock_guard<boost::mutex> buffer_guard(this->frame_buffer_mutex);
            // Drop the frame if our buffer has reached a certain size.
            if (this->frame_buffer.size() < 300) {
                this->frame_buffer.push(frame);
                addedFrame = true;
            }
        }

        if (addedFrame) {
            {
                boost::lock_guard<boost::mutex> frames_available_guard(this->frames_available_mutex);
                this->frames_available = true;
            }

            this->frames_available_cond.notify_one();
        }
        return addedFrame;
    }

    std::unique_ptr<BmpFrameWriter> BmpFrameWriter::create(std::string path, std::string info_filename)
    {
        std::unique_ptr<BmpFrameWriter> instance( new BmpFrameWriter(path, info_filename) );
        return instance;
    }
}
