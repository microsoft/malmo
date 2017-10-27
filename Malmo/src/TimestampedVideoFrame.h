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
            , RAW_BMP               //!< Layout bytes as raw BMP data (bottom-to-top RGB)
            , REVERSE_SCANLINE      //!< Interpret input bytes as reverse scanline BGR
        };
        enum FrameType {
            _MIN_FRAME_TYPE = 0
            , VIDEO = _MIN_FRAME_TYPE //!< Normal video, either 24bpp RGB or 32bpp RGBD
            , DEPTH_MAP               //!< 32bpp float depthmap
            , LUMINANCE               //!< 8bpp greyscale bitmap
            , COLOUR_MAP              //!< 24bpp colour map
            , _MAX_FRAME_TYPE
        };
        static const int FRAME_HEADER_SIZE = 20;

        //! The timestamp.
        boost::posix_time::ptime timestamp;
        
        //! The width of the image in pixels.
        short width;
        
        //! The height of the image in pixels.
        short height;
        
        //! The number of channels. e.g. 3 for RGB data, 4 for RGBD
        short channels;

        //! The type of video data - eg 24bpp RGB, or 32bpp float depth
        FrameType frametype;

        //! The pitch of the player at render time
        float pitch;

        //! The yaw of the player at render time
        float yaw;

        //! The x pos of the player at render time
        float xPos;

        //! The y pos of the player at render time
        float yPos;

        //! The z pos of the player at render time
        float zPos;

        //! The pixels, stored as channels then columns then rows. Length should be width*height*channels.
        std::vector<unsigned char> pixels;

        TimestampedVideoFrame();
        TimestampedVideoFrame(short width, short height, short channels, TimestampedUnsignedCharVector& message, Transform transform = IDENTITY, FrameType frametype = VIDEO);
        
        bool operator==(const TimestampedVideoFrame& other) const;
        friend std::ostream& operator<<(std::ostream& os, const TimestampedVideoFrame& tsvidframe);
        friend std::ostream& operator<<(std::ostream& os, const TimestampedVideoFrame::FrameType& frametype);
        float ntoh_float(uint32_t value) const;
    };
}

#endif
