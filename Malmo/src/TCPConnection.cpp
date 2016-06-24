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
#include "TCPConnection.h"

// Boost:
#include <boost/bind.hpp>
using boost::asio::ip::tcp;

namespace malmo
{
    boost::shared_ptr<TCPConnection> TCPConnection::create(boost::asio::io_service& io_service, boost::function<void(const TimestampedUnsignedCharVector) > callback, bool expect_size_header)
    {
        return boost::shared_ptr<TCPConnection>(new TCPConnection(io_service, callback, expect_size_header) );
    }

    tcp::socket& TCPConnection::getSocket()
    {
        return this->socket;
    }

    void TCPConnection::confirmWithFixedReply(std::string reply)
    {
        this->confirm_with_fixed_reply = true;
        this->fixed_reply = reply;
    }

    void TCPConnection::read()
    {
        if( this->expect_size_header )
        {
            this->header_buffer.resize( SIZE_HEADER_LENGTH );
            boost::asio::async_read(
                this->socket,
                boost::asio::buffer( this->header_buffer ),
                boost::bind(
                    &TCPConnection::handle_read_header,
                    shared_from_this(),
                    boost::asio::placeholders::error,
                    boost::asio::placeholders::bytes_transferred
                )
            );
        }
        else {
            boost::asio::async_read_until(
                this->socket,
                this->delimited_buffer,
                '\n',
                boost::bind(
                    &TCPConnection::handle_read_line,
                    shared_from_this(),
                    boost::asio::placeholders::error,
                    boost::asio::placeholders::bytes_transferred
                )
            );
        }
    }
    
    void TCPConnection::handle_read_header(
        const boost::system::error_code& error,
        size_t bytes_transferred)
    {
        if( !error ) {
            const size_t body_size = getSizeFromHeader();
            this->body_buffer.resize( body_size );
            boost::asio::async_read(
                this->socket,
                boost::asio::buffer( this->body_buffer ),
                boost::bind(
                    &TCPConnection::handle_read_body,
                    shared_from_this(),
                    boost::asio::placeholders::error,
                    boost::asio::placeholders::bytes_transferred
                )
            );
        }
    }
    
    void TCPConnection::handle_read_body(
        const boost::system::error_code& error,
        size_t bytes_transferred)
    {
        if( !error ) 
        {
            this->processMessage();
        }
    }
    
    void TCPConnection::handle_read_line(
        const boost::system::error_code& error,
        size_t bytes_transferred)
    {
        if( !error ) {
            this->body_buffer.assign( 
                    boost::asio::buffers_begin( this->delimited_buffer.data() ),
                    boost::asio::buffers_begin( this->delimited_buffer.data() ) + bytes_transferred 
            );
            this->delimited_buffer.consume( bytes_transferred );
            this->processMessage();
        }
    }
    
    void TCPConnection::processMessage()
    {
        if( this->confirm_with_fixed_reply )
            sendReply();
        this->onMessageReceived( TimestampedUnsignedCharVector( boost::posix_time::microsec_clock::universal_time(), 
                                                                this->body_buffer ) );
        this->read();
    }
    
    void TCPConnection::sendReply() 
    {
        const int REPLY_SIZE_HEADER_LENGTH = 4;
        u_long reply_size_header = htonl( (u_long)this->fixed_reply.size() );
        boost::asio::write( this->socket, boost::asio::buffer(&reply_size_header, REPLY_SIZE_HEADER_LENGTH));
        boost::system::error_code ignored_error;
        boost::asio::write( this->socket, boost::asio::buffer(this->fixed_reply), boost::asio::transfer_all(), ignored_error );
    }

    TCPConnection::TCPConnection(boost::asio::io_service& io_service, boost::function<void(const TimestampedUnsignedCharVector) > callback, bool expect_size_header)
        : socket(io_service)
        , onMessageReceived(callback)
        , confirm_with_fixed_reply(false)
        , expect_size_header(expect_size_header)
    {
    }

    size_t TCPConnection::getSizeFromHeader()
    {
        u_long size_in_network_byte_order = *reinterpret_cast<u_long*>(&(this->header_buffer[0]));
        u_long size_in_host_byte_order = ntohl(size_in_network_byte_order);
        return size_in_host_byte_order;
    }
}
