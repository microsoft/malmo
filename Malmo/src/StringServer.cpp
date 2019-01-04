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
#include "StringServer.h"

// Boost:
#include <boost/bind.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <iostream>

namespace malmo {
    StringServer::StringServer(boost::asio::io_service& io_service, int port, const boost::function<void(const TimestampedString string_message)> handle_string, const std::string& log_name)
        : handle_string(handle_string)
        , io_service(io_service)
        , port(port)
        , log_name(log_name)
    {
    }

    void StringServer::start(boost::shared_ptr<StringServer>& scope)
    {
        this->server = boost::make_shared<TCPServer>(this->io_service, this->port, boost::bind(&StringServer::handleMessage, scope, _1), this->log_name);
        this->scope = scope;
        this->server->start(scope.get());
    }

    void StringServer::close() {
        this->server->close();
    }

    void StringServer::release() {
        this->scope = 0;
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
        this->server->confirmWithFixedReply(reply);
        return *this;
    }
    
    StringServer& StringServer::expectSizeHeader(bool expect_size_header)
    {
        this->server->expectSizeHeader(expect_size_header);
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
        return this->server->getPort();
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
