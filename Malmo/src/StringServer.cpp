// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "StringServer.h"

// Boost:
#include <boost/bind.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

namespace malmo {
    StringServer::StringServer(boost::asio::io_service& io_service, int port, const boost::function<void(const TimestampedString string_message)> handle_string)
        : handle_string(handle_string)
        , server(io_service, port, boost::bind(&StringServer::handleMessage, this, _1))
    {
    }

    void StringServer::start()
    {
        this->server.start();
    }

    StringServer& StringServer::record(std::string path)
    {
        if (this->writer.is_open()){
            this->writer.close();
        }

        this->writer.open(path, std::ofstream::out | std::ofstream::app);

        return *this;
    }
    
    StringServer& StringServer::confirmWithFixedReply(std::string reply)
    {
        this->server.confirmWithFixedReply(reply);
        return *this;
    }
    
    StringServer& StringServer::expectSizeHeader(bool expect_size_header)
    {
        this->server.expectSizeHeader(expect_size_header);
        return *this;
    }

    void StringServer::handleMessage(const TimestampedUnsignedCharVector message)
    {
        TimestampedString string_message(message);

        this->handle_string(string_message);

        this->recordMessage(string_message);
    }

    void StringServer::stopRecording()
    {
        if (this->writer.is_open())
        {
            boost::lock_guard<boost::mutex> scope_guard(this->write_mutex);
            
            this->writer.close();
        }
    }

    int StringServer::getPort() const
    {
        return this->server.getPort();
    }

    void StringServer::recordMessage(const TimestampedString string_message)
    {
        if (this->writer.is_open())
        {
            boost::lock_guard<boost::mutex> scope_guard(this->write_mutex);

            this->writer << boost::posix_time::to_iso_string(string_message.timestamp) << " " << string_message.text << std::endl;
        }
    }
}
