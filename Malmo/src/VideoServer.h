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

#ifndef _VIDEOSERVER_H_
#define _VIDEOSERVER_H_

// Local:
#include "TCPServer.h"
#include "TimestampedVideoFrame.h"
#include "VideoFrameWriter.h"

// Boost:
#include <boost/function.hpp>

// STL:
#include <vector>
#include <memory>

namespace malmo
{
    //! A TCP server that receives video frames of a size specified beforehand and can optionally persist to file.
    class VideoServer 
    {
        public:

            VideoServer( boost::asio::io_service& io_service, int port, short width, short height, short channels, const boost::function<void(const TimestampedVideoFrame message)> handle_frame );
            
            //! Request that the video is saved in an mp4 file. Call before either startInBackground() or startRecording().
            VideoServer& recordMP4(std::string path, int frames_per_second, int64_t bit_rate);

            //! Gets the port this server is listening on.
            //! \returns The port this server is listening on.
            int getPort() const;
            
            //! Gets the width of the video.
            //! \returns The width of the video in pixels.
            short getWidth() const;
            
            //! Gets the height of the video.
            //! \returns The height of the video in pixels.
            short getHeight() const;
            
            //! Gets the number of channels in the video. e.g. 3 for RGB, 4 for RGBA.
            //! \returns The number of channels in the video.
            short getChannels() const;

            //! Stop recording the data being received by the server.
            void stopRecording();

            //! Start recording the data being received by the server.
            void startRecording();

            //! Starts the video server.
            void start();

        private:

            void handleMessage( const TimestampedUnsignedCharVector message );
            
            boost::function<void(const TimestampedVideoFrame message)> handle_frame;
            short width;
            short height;
            short channels;
            TCPServer server;
            std::vector<std::unique_ptr<VideoFrameWriter>> writers;
    };
}

#endif
