// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "VideoServer.h"
#include "VideoFrameWriter.h"

// Boost:
#include <boost/bind.hpp>

namespace malmo 
{
    VideoServer::VideoServer( boost::asio::io_service& io_service, int port, short width, short height, short channels, const boost::function<void(TimestampedVideoFrame message)> handle_frame )
        : handle_frame( handle_frame )
        , width( width )
        , height( height )
        , channels( channels )
        , server( io_service, port, boost::bind( &VideoServer::handleMessage, this, _1 ) )
    {
    }

    void VideoServer::start()
    {
        this->server.start();
    }
    
    void VideoServer::startRecording()
    {
        for (const auto& writer : this->writers){
            writer->open();
        }
    }   

    void VideoServer::stopRecording()
    {
        for (const auto& writer : this->writers){
            if (writer->isOpen()){
                writer->close();
            }
        }
        this->writers.clear();
    }

    VideoServer& VideoServer::recordMP4(std::string path, int frames_per_second, int64_t bit_rate)
    {        
        this->writers.push_back(VideoFrameWriter::create(path, this->width, this->height, frames_per_second, bit_rate));

        return *this;
    }
    
    void VideoServer::handleMessage( TimestampedUnsignedCharVector message )
    {
        if (message.data.size() != this->width * this->height * this->channels) 
        {
            // Have seen this happen during stress testing when a reward packet from (I think) a previous mission arrives during the next
            // one when the same port has been reassigned. Could throw here but chose to silently ignore since very rare.
            return;
        }
        TimestampedVideoFrame frame(this->width, this->height, this->channels, message, TimestampedVideoFrame::REVERSE_SCANLINE);
        this->handle_frame(frame);
        
        for (const auto& writer : this->writers){
            if (writer->isOpen()){
                writer->write(frame);
            }
        }
    }
    
    int VideoServer::getPort() const
    {
        return this->server.getPort();
    }

    short VideoServer::getWidth() const
    {
        return this->width;
    }

    short VideoServer::getHeight() const
    {
        return this->height;
    }

    short VideoServer::getChannels() const
    {
        return this->channels;
    }
}
