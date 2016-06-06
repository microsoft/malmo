// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _TIMESTAMPEDVIDEOFRAME_H_
#define _TIMESTAMPEDVIDEOFRAME_H_

// Local:
#include "TimestampedUnsignedCharVector.h"

// Boost:
#include <boost/date_time/posix_time/posix_time_types.hpp>

// STL:
#include <vector>

namespace malmo 
{
    //! An image with an attached timestamp saying when it was collected.
    struct TimestampedVideoFrame 
    {
        enum Transform {
            IDENTITY                //!< Don't alter the incoming bytes in any way
            , REVERSE_SCANLINE      //!< Interpret input bytes as reverse scanline BGR
        };

        //! The timestamp.
        boost::posix_time::ptime timestamp;
        
        //! The width of the image in pixels.
        short width;
        
        //! The height of the image in pixels.
        short height;
        
        //! The number of channels. e.g. 3 for RGB data, 4 for RGBA
        short channels;
        
        //! The pixels, stored as channels then columns then rows. Length should be width*height*channels.
        std::vector<unsigned char> pixels;

        TimestampedVideoFrame();
        TimestampedVideoFrame(short width, short height, short channels, TimestampedUnsignedCharVector& message, Transform transform = IDENTITY);
        
        bool operator==(const TimestampedVideoFrame& other) const;
        friend std::ostream& operator<<(std::ostream& os, const TimestampedVideoFrame& tsvidframe);
    };
}

#endif
