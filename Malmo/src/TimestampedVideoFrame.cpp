// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "TimestampedVideoFrame.h"
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/date_time/posix_time/posix_time_io.hpp>

namespace malmo
{
    TimestampedVideoFrame::TimestampedVideoFrame()
        : width(0)
        , height(0)
        , channels(0)        
    {

    }

    TimestampedVideoFrame::TimestampedVideoFrame(short width, short height, short channels, TimestampedUnsignedCharVector& message, Transform transform)
        : timestamp(message.timestamp)
        , width(width)
        , height(height)
        , channels(channels)
    {
        const int stride = width * channels;
        switch (transform){
        case IDENTITY:
            this->pixels = std::vector<unsigned char>(message.data);
            break;

        case REVERSE_SCANLINE:
            this->pixels = std::vector<unsigned char>();
            for (int i = 0, offset = (height - 1)*stride; i < height; i++, offset -= stride){
                auto it = message.data.begin() + offset;
                this->pixels.insert(this->pixels.end(), it, it + stride);
            }

            break;

        default:
            throw std::invalid_argument("Unknown transform");
        }
    }

    bool TimestampedVideoFrame::operator==(const TimestampedVideoFrame& other) const
    {
        return this->width == other.width && this->height == other.height && this->channels == other.channels && this->timestamp == other.timestamp && this->pixels == other.pixels;
    }

    std::ostream& operator<<(std::ostream& os, const TimestampedVideoFrame& tsvidframe)
    {
	os << "TimestampedVideoFrame: " << to_simple_string(tsvidframe.timestamp) << ", " << tsvidframe.width << " x " << tsvidframe.height << " x " << tsvidframe.channels;
	return os;
    }
}
