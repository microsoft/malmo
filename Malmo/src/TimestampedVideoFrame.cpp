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
#include "TimestampedVideoFrame.h"
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/date_time/posix_time/posix_time_io.hpp>

// Boost:
#include <boost/asio.hpp>

namespace malmo
{
    TimestampedVideoFrame::TimestampedVideoFrame()
        : width(0)
        , height(0)
        , channels(0)
        , xPos(0)
        , yPos(0)
        , zPos(0)
        , yaw(0)
        , pitch(0)
        , frametype(VIDEO)
    {

    }

    TimestampedVideoFrame::TimestampedVideoFrame(short width, short height, short channels, TimestampedUnsignedCharVector& message, Transform transform, FrameType frametype)
        : timestamp(message.timestamp)
        , width(width)
        , height(height)
        , channels(channels)
        , frametype(frametype)
        , xPos(0)
        , yPos(0)
        , zPos(0)
        , yaw(0)
        , pitch(0)
    {
        // First extract the positional information from the header:
        uint32_t * pInt = reinterpret_cast<uint32_t*>(&(message.data[0]));
        this->xPos = ntoh_float(*pInt); pInt++;
        this->yPos = ntoh_float(*pInt); pInt++;
        this->zPos = ntoh_float(*pInt); pInt++;
        this->yaw = ntoh_float(*pInt); pInt++;
        this->pitch = ntoh_float(*pInt);

        const int stride = width * channels;
        switch (transform){
        case IDENTITY:
            this->pixels = std::vector<unsigned char>(message.data.begin() + FRAME_HEADER_SIZE, message.data.end());
            break;

        case RAW_BMP:
            this->pixels = std::vector<unsigned char>(message.data.begin() + FRAME_HEADER_SIZE, message.data.end());
            if (channels == 3){
                // Swap BGR -> RGB:
                for (int i = 0; i < this->pixels.size(); i += 3){
                    char t = this->pixels[i];
                    this->pixels[i] = this->pixels[i + 2];
                    this->pixels[i + 2] = t;
                }
            }
            break;

        case REVERSE_SCANLINE:
            this->pixels = std::vector<unsigned char>();
            for (int i = 0, offset = (height - 1)*stride; i < height; i++, offset -= stride){
                auto it = message.data.begin() + offset + FRAME_HEADER_SIZE;
                this->pixels.insert(this->pixels.end(), it, it + stride);
            }

            break;

        default:
            throw std::invalid_argument("Unknown transform");
        }
    }

    bool TimestampedVideoFrame::operator==(const TimestampedVideoFrame& other) const
    {
        return this->frametype == other.frametype && this->width == other.width && this->height == other.height && this->channels == other.channels && this->timestamp == other.timestamp && this->pixels == other.pixels;
        // Not much point in comparing pos, pitch and yaw - covered by the pixel comparison.
    }

    std::ostream& operator<<(std::ostream& os, const TimestampedVideoFrame& tsvidframe)
    {
        os << "TimestampedVideoFrame: " << to_simple_string(tsvidframe.timestamp) << ", type " << tsvidframe.frametype << ", " << tsvidframe.width << " x " << tsvidframe.height << " x " << tsvidframe.channels << ", (" << tsvidframe.xPos << "," << tsvidframe.yPos << "," << tsvidframe.zPos << " - yaw:" << tsvidframe.yaw << ", pitch:" << tsvidframe.pitch << ")";
        return os;
    }

    std::ostream& operator<<(std::ostream& os, const TimestampedVideoFrame::FrameType& type)
    {
        switch (type)
        {
        case TimestampedVideoFrame::VIDEO:
            os << "video"; break;
        case TimestampedVideoFrame::DEPTH_MAP:
            os << "depth"; break;
        case TimestampedVideoFrame::LUMINANCE:
            os << "luminance"; break;
        case TimestampedVideoFrame::COLOUR_MAP:
            os << "colourmap"; break;
        default:
            break;
        }
        return os;
    }

    float TimestampedVideoFrame::ntoh_float(uint32_t value) const
    {
        uint32_t temp = ntohl(value);
        float ret;
        *((uint32_t*)&ret) = temp;
        return ret;
    }
}
