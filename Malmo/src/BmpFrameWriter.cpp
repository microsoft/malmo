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
#include "Logger.h"

// STL:
#include <exception>
#include <sstream>
#include <stdio.h>

#include <boost/iostreams/filter/gzip.hpp>
#include <boost/iostreams/filtering_stream.hpp>
#include <boost/iostreams/stream.hpp>
#include <boost/asio.hpp>   // For htonl
#include <boost/make_shared.hpp>

#define LOG_COMPONENT Logger::LOG_VIDEO

namespace malmo
{
    class TarHelper
    {
    public:
        TarHelper(boost::filesystem::path path, bool numpy_format, std::size_t max_tar_size = 1 << 30) : path(path), frame_count(0), max_tar_size(max_tar_size), unzipped_byte_count(0), compression_level(6)
        {
            // For now, get this from the environment:
            char *compression_env = getenv("MALMO_BMP_COMPRESSION_LEVEL");
            if (compression_env) {
                this->compression_level = std::min(std::max(atoi(compression_env), 0), 9);
            }
            // Are big-endian or little-endian?
            // Simple way to check using htonl (always converts to big-endian)
            this->bigendian = (htonl(1) == 1);
            this->numpy_format = numpy_format;
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
            // For 1bpp, save as portable greyscale (pgm)
            // For 3bpp, save as ppm.
            // For 4bpp, save as numpy array if this->numpy_format is true.
            // Otherwise, strip out the alpha channel and save as two files - a pgm and a pbm.
            // Note - these are horribly inefficient file formats, but speed of writing is paramount - there's no time to do png compression, etc.
            // We just dump the frames to the disk as fast as possible, in a format that is relatively easy to open.
            std::string extension = (frame.channels == 1) ? ".pgm" : ((this->numpy_format) ? ".npy" : ".ppm");
            frame_name << "frame_" << std::setfill('0') << std::setw(6) << this->frame_count << extension;

            std::stringstream frame_header;
            if (frame.channels != 4 )
            {
                std::string magic_number = (frame.channels == 1) ? "P5" : "P6";
                frame_header << magic_number << "\n" << frame.width << " " << frame.height << "\n255\n";
                std::string header = frame_header.str();
                this->tarball->putMemWithHeader(header.c_str(), header.length(), reinterpret_cast<const char*>(&(frame.pixels[0])), frame.pixels.size(), frame_name.str().c_str());
                this->unzipped_byte_count += header.length() + frame.pixels.size();
            }
            else if (this->numpy_format)
            {
                // Save in numpy format, for ease of use with Python.
                // See here https://docs.scipy.org/doc/numpy/neps/npy-format.html for the specification.
                // There are essentially two parts - the header and the data. The header must be padded such that the data
                // begins on a 16 byte boundary.
                // The header is made up of a ten byte "preamble" followed by a literal string description of a python dictionary.
                // This dictionary contains everything numpy needs to know in order to read the data. The preamble contains everything numpy
                // needs to know in order to read the header.

                // Note that, in addFrame's typical use, each frame we are called with will have the same dimensions, so this header will not change
                // from call to call - it could be created ahead of time and reused for each frame.

                // First 8 bytes: magic number (0x93), the string "NUMPY", and the major and minor file format versions.
                frame_header << (char)0x93 << "NUMPY" << (char)0x01 << (char)0x00;

                // Construct the ASCII description of a python dictionary containing the details of this file. (Shape is rows x cols, so height x width.)
                std::stringstream pydict;
                pydict << "{'descr': '" << (this->bigendian ? ">" : "<") << "f4', 'fortran_order':False, 'shape':(" << frame.height << "," << frame.width << ") }";

                // The dictionary must be terminated with a newline character, but the padding is added before that, as spaces.
                uint16_t minimum_header_length = (uint16_t)(pydict.str().length()) + 11; // +1 for the newline terminator, +10 for the preamble.
                // Work out the padded length:
                uint16_t padded_header_length = 16 * int(ceil(minimum_header_length / 16.0));
                if (padded_header_length != minimum_header_length)
                    pydict << std::string("                ").substr(0, padded_header_length - minimum_header_length);
                pydict << "\n";
                // The length of the dictionary definition is encoded in the preamble (not the length of the *whole* header - ie subtract the premable size.)
                uint16_t header_length = padded_header_length - 10;
                // It must be stored in little-endian format:
                unsigned char lsb = header_length & 0xff;
                unsigned char msb = header_length >> 8;
                frame_header << lsb << msb;
                // Preamble is complete - now write out the dictionary:
                frame_header << pydict.str();
                std::string header = frame_header.str();
                // Now store the data, with this header:
                this->tarball->putMemWithHeader(header.c_str(), header.length(), reinterpret_cast<const char*>(&(frame.pixels[0])), frame.pixels.size(), frame_name.str().c_str());
                this->unzipped_byte_count += header.length() + frame.pixels.size();
            }
            else
            {
                // We get here if the user has requested the combined video/depthmap data - eg the depthmap is encoded in the alpha channel.
                // The ppm format doesn't support 32bpp RGBA data - and, arguably, it makes no sense to interpret the depth values as a alpha values
                // anyway, so we split the data into a 24bpp RGB image and an 8bpp greyscale image.

                // Rearrange RGBARGBA into RGBRGB...AA
                char *out_pixels = new char[frame.width * frame.height * 4];
                int offset_rgb = 0;
                int offset_a = frame.width * frame.height * 3;
                for (int i = 0; i < frame.width*frame.height; i++)
                {
                    out_pixels[offset_rgb] = frame.pixels[i * 4];
                    out_pixels[offset_rgb + 1] = frame.pixels[i * 4 + 1];
                    out_pixels[offset_rgb + 2] = frame.pixels[i * 4 + 2];
                    out_pixels[offset_a] = frame.pixels[i * 4 + 3];   // copy alpha value
                    offset_rgb += 3;
                    offset_a += 1;
                }

                // Save alpha channel:
                std::stringstream alpha_frame_name;
                alpha_frame_name << "frame_" << std::setfill('0') << std::setw(6) << this->frame_count << ".pgm";
                std::stringstream alpha_frame_header;
                alpha_frame_header << "P5" << "\n" << frame.width << " " << frame.height << "\n255\n";
                std::string header = alpha_frame_header.str();
                this->tarball->putMemWithHeader(header.c_str(), header.length(), reinterpret_cast<const char*>(&(out_pixels[frame.width * frame.height * 3])), frame.width * frame.height, alpha_frame_name.str().c_str());

                // Save RGB:
                std::stringstream rgb_frame_name;
                rgb_frame_name << "frame_" << std::setfill('0') << std::setw(6) << this->frame_count << ".ppm";
                std::stringstream rgb_frame_header;
                rgb_frame_header << "P6" << "\n" << frame.width << " " << frame.height << "\n255\n";
                header = rgb_frame_header.str();
                this->tarball->putMemWithHeader(header.c_str(), header.length(), reinterpret_cast<const char*>(&(out_pixels[0])), frame.width * frame.height * 3, rgb_frame_name.str().c_str());
                this->unzipped_byte_count += 2 * header.length() + frame.pixels.size();
                delete[] out_pixels;
            }
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
            LOGFINE(tarname.str(), LT(" created."));
        }

        int frame_count;
        int compression_level;  // gzip compression level - 1=best speed, 9=best compression (6 is currently default according to zlib docs)
        bool bigendian;
        bool numpy_format;
        std::size_t max_tar_size;
        std::size_t unzipped_byte_count;
        boost::filesystem::path path;
        boost::shared_ptr<lindenb::io::Tar> tarball;
        boost::shared_ptr<boost::iostreams::filtering_ostream> filter;
        boost::shared_ptr<std::ofstream> backing_file;
    };

    BmpFrameWriter::BmpFrameWriter(std::string path, std::string frame_info_filename, bool saveInNumpyFormat)
        : path(path)
        , is_open(false)
        , is_numpy_format(saveInNumpyFormat)
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
        LOGSECTION(LOG_FINE, "In BmpFrameWriter::close()...");

        if (this->is_open) {
            this->is_open = false;
            {
                boost::lock_guard<boost::mutex> frames_available_guard(this->frames_available_mutex);

                this->frames_available = true;
            }

            LOGFINE(LT("Notifying worker thread that frames are available, in order to close."));
            this->frames_available_cond.notify_one();
            LOGFINE(LT("Waiting for worker thread to join."));
            this->frame_writer_thread.join();
            this->frame_info_stream.close();
            LOGFINE(LT("Worker thread joined."));
            LOGFINE(LT("Frames received for writing: "), this->frame_index);
            LOGFINE(LT("Frames actually written: "), this->frames_actually_written);
        }
    }

    void BmpFrameWriter::writeFrames()
    {
        this->frames_actually_written = 0;
        TarHelper tarrer(boost::filesystem::path(this->path), this->is_numpy_format);
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
                    }
                    else {
                        boost::lock_guard<boost::mutex> frames_available_guard(this->frames_available_mutex);
                        this->frames_available = false;
                        break;
                    }
                }
                // Write frame into tarball:
                LOGTRACE(LT("Tarring frame "), tarrer.getFrameCount() + 1, LT(", "), frame.width, LT("x"), frame.height, LT("x"), frame.channels);
                tarrer.addFrame(frame);
                // And add details to index files:
                std::stringstream name;
                name << "frame_" << std::setfill('0') << std::setw(6) << tarrer.getFrameCount();
                std::stringstream posdata;
                posdata << "xyzyp: " << frame.xPos << " " << frame.yPos << " " << frame.zPos << " " << frame.yaw << " " << frame.pitch;
                this->frame_info_stream << boost::posix_time::to_iso_string(frame.timestamp) << " " << name.str() << " " << posdata.str() << std::endl;
                this->frames_actually_written++;
            }
        }
        LOGTRACE(LT("Flushing frame info stream"));
        this->frame_info_stream << "# EOF - frames written: " << this->frames_actually_written << std::endl;
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
                LOGTRACE(LT("Pushing frame "), this->frame_index, LT(", "), frame.width, LT("x"), frame.height, LT("x"), frame.channels, LT(" to write buffer."));
                this->frame_buffer.push(frame);
                this->frame_index++;
                addedFrame = true;
            }
            else {
                LOGWARNING(LT("BmpFrameWriter dropping frame - buffer is full - try reducing MALMO_BMP_COMPRESSION_LEVEL (1=best speed, 9=best compression, 6=default)"));
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

    std::unique_ptr<BmpFrameWriter> BmpFrameWriter::create(std::string path, std::string info_filename, bool saveInNumpyFormat)
    {
        std::unique_ptr<BmpFrameWriter> instance( new BmpFrameWriter(path, info_filename, saveInNumpyFormat) );
        return instance;
    }
}

#undef LOG_COMPONENT
