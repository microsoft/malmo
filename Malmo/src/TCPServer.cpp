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
#include "TCPServer.h"
#include "Logger.h"

// Boost:
#include <boost/bind.hpp>
#include <boost/iterator/counting_iterator.hpp>
#include <boost/make_shared.hpp>
using boost::asio::ip::tcp;

// STL:
#include <algorithm>
#include <chrono>
#include <random>

namespace malmo
{
    TCPServer::TCPServer( boost::asio::io_service& io_service, int port, boost::function<void(const TimestampedUnsignedCharVector) > callback, const std::string& log_name )
        : onMessageReceived(callback)
        , confirm_with_fixed_reply(false)
        , expect_size_header(true)
        , log_name(log_name)
    {
        if (port == 0) {
            // attempt to assign a port from a predefined range
            const int port_min = 10000;
            const int port_max = 11000; // TODO: could be configurable
            this->bindToRandomPortInRange(io_service, port_min, port_max);
        }
        else {
            // attempt to use the port requested, throwing an error if not possible
            this->bindToPort(io_service, port);
        }
    }

    void TCPServer::start()
    {
        this->startAccept();
    }
    
    void TCPServer::confirmWithFixedReply(std::string reply)
    {
        this->confirm_with_fixed_reply = true;
        this->fixed_reply = reply;
    }
    
    void TCPServer::expectSizeHeader(bool expect_size_header)
    {
        this->expect_size_header = expect_size_header;
    }

    void TCPServer::startAccept()
    {
        boost::shared_ptr<TCPConnection> new_connection = TCPConnection::create(
            this->acceptor->get_io_service(),
            this->onMessageReceived,
            this->expect_size_header,
            this->log_name
        );
            
        if( this->confirm_with_fixed_reply )
            new_connection->confirmWithFixedReply( this->fixed_reply );

        this->acceptor->async_accept(new_connection->getSocket(),
            boost::bind(&TCPServer::handleAccept,
            this,
            new_connection,
            boost::asio::placeholders::error));
    }
    
    void TCPServer::handleAccept( 
        boost::shared_ptr<TCPConnection> new_connection,
        const boost::system::error_code& error)
    {
        if (!error)
        {
            new_connection->read();
            this->startAccept();
        }
        else
            LOGERROR(LT("TCPServer::handleAccept("), this->log_name, LT(") - "), error.message());
    }

    int TCPServer::getPort() const
    {
        return this->acceptor->local_endpoint().port();
    }
    
    void TCPServer::bindToRandomPortInRange(boost::asio::io_service& io_service, int port_min, int port_max)
    {
        LOGSECTION(LOG_FINE, "Choosing random port for " + this->log_name)

        auto a = boost::counting_iterator<int>( port_min );
        auto b = boost::counting_iterator<int>( port_max );
        std::vector<int> port_range( a, b );
                                     
        std::random_device rng;
        std::mt19937 urng(rng());
        std::shuffle(port_range.begin(), port_range.end(), urng);
        
        for( auto test_port : port_range )
        {
            try {
                LOGFINE(LT("Trying port "), test_port);
                this->bindToPort( io_service, test_port );
                return;
            } catch( const boost::system::system_error& ) {
                // port already in use, try another
                continue;
            }
        }
        LOGERROR(LT("Couldn't find an available port for "), this->log_name, LT(" - throwing!"));
        throw std::runtime_error( "All ports in range were busy!" );
    }
    
    void TCPServer::bindToPort(boost::asio::io_service& io_service, int port)
    {
        tcp::endpoint endpt = tcp::endpoint( tcp::v4(), port );
        try
        {
            this->acceptor = boost::make_shared< tcp::acceptor >(io_service, endpt, false);
        }
        catch (boost::system::system_error e)
        {
            LOGERROR(this->log_name, LT(" couldn't bind to "), endpt, LT(" - "), e.code().message());
            throw e;
        }
        LOGFINE(this->log_name, LT(" bound local endpoint "), this->acceptor->local_endpoint(), LT(" to "), endpt);
    }
}
