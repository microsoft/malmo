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
#include "VideoServer.h"
#include "VideoFrameWriter.h"
#include "BmpFrameWriter.h"

// Boost:
#include <boost/bind.hpp>

namespace malmo 
{
    VideoServer::VideoServer( boost::asio::io_service& io_service, int port, short width, short height, short channels, TimestampedVideoFrame::FrameType frametype, const boost::function<void(TimestampedVideoFrame message)> handle_frame )
        : handle_frame( handle_frame )
        , width( width )
        , height( height )
        , channels( channels )
        , frametype( frametype )
        , received_frames(0)
        , written_frames(0)
        , queued_frames(0)
        , transform(TimestampedVideoFrame::REVERSE_SCANLINE)
        , io_service(io_service)
        , port(port)
    {
    }

    // Start the server within a scope that is used to control sharing from async io calls.
    void VideoServer::start(boost::shared_ptr<VideoServer>& scope)
    {
        this->server = boost::make_shared<TCPServer>(this->io_service, this->port, boost::bind(&VideoServer::handleMessage, scope, _1), "vid");
        this->scope = scope;
        this->written_frames = this->queued_frames = this->received_frames = 0;
        this->server->start(scope.get());
    }
    
    void VideoServer::close() {
        this->server->close();
    }

    void VideoServer::release() {
        this->scope = nullptr;
    }

    void VideoServer::startRecording()
    {
        this->written_frames = this->queued_frames = this->received_frames = 0;
        for (const auto& writer : this->writers){
            writer->open();
        }
    }   

    void VideoServer::stopRecording()
    {
        for (const auto& writer : this->writers){
            if (writer->isOpen()){
                writer->close();
                this->written_frames += writer->getFrameWriteCount();
            }
        }
        this->writers.clear();
    }

    VideoServer& VideoServer::recordMP4(std::string path, int frames_per_second, int64_t bit_rate, bool drop_input_frames)
    {
        int channels = 3;
        std::string filename;
        switch (this->frametype)
        {
        case TimestampedVideoFrame::COLOUR_MAP:
            filename = "colour_map_info.txt";
            break;
        case TimestampedVideoFrame::DEPTH_MAP:
            filename = "depth_frame_info.txt";
            break;
        case TimestampedVideoFrame::LUMINANCE:
            filename = "luminance_frame_info.txt";
            channels = 1;
            break;
        case TimestampedVideoFrame::VIDEO:
        default:
            filename = "frame_info.txt";
            break;
        }
        this->writers.push_back(VideoFrameWriter::create(path, filename, this->width, this->height, frames_per_second, bit_rate, channels, drop_input_frames));
        this->transform = TimestampedVideoFrame::REVERSE_SCANLINE;

        return *this;
    }

    VideoServer& VideoServer::recordBmps(std::string path)
    {
        std::string filename;
        std::string frame_dir;
        switch (this->frametype)
        {
        case TimestampedVideoFrame::COLOUR_MAP:
            filename = "colour_map_info.txt";
            frame_dir = "colour_map_frames";
            break;
        case TimestampedVideoFrame::DEPTH_MAP:
            filename = "depth_frame_info.txt";
            frame_dir = "depth_frames";
            break;
        case TimestampedVideoFrame::LUMINANCE:
            filename = "luminance_frame_info.txt";
            frame_dir = "luminance_frames";
            break;
        case TimestampedVideoFrame::VIDEO:
        default:
            filename = "frame_info.txt";
            frame_dir = "video_frames";
            break;
        }
        boost::filesystem::path fs_path(path);
        boost::filesystem::path frame_path = fs_path / frame_dir;
        this->writers.push_back(BmpFrameWriter::create(frame_path.string(), filename, this->frametype==TimestampedVideoFrame::DEPTH_MAP));
        this->transform = TimestampedVideoFrame::REVERSE_SCANLINE;
        return *this;
    }

    void VideoServer::handleMessage( TimestampedUnsignedCharVector message )
    {
        if (message.data.size() != TimestampedVideoFrame::FRAME_HEADER_SIZE + this->width * this->height * this->channels)
        {
            // Have seen this happen during stress testing when a reward packet from (I think) a previous mission arrives during the next
            // one when the same port has been reassigned. Could throw here but chose to silently ignore since very rare.
            return;
        }
        TimestampedVideoFrame frame(this->width, this->height, this->channels, message, this->transform, this->frametype);
        this->received_frames++;
        this->handle_frame(frame); 

        for (const auto& writer : this->writers){
            if (writer->isOpen()){
                if (writer->write(frame)){
                    this->queued_frames++;
                }
            }
        }
    }
    
    int VideoServer::getPort() const
    {
        return this->server->getPort();
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

    TimestampedVideoFrame::FrameType VideoServer::getFrameType() const
    {
        return this->frametype;
    }
}
