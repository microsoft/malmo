// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

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
