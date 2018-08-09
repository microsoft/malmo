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
#include "Logger.h"

// Boost:
#include <boost/bind.hpp>
using boost::asio::ip::tcp;

#define LOG_COMPONENT Logger::LOG_TCP

namespace malmo
{
    boost::shared_ptr<TCPConnection> TCPConnection::create(boost::asio::io_service& io_service, boost::function<void(const TimestampedUnsignedCharVector) > callback, bool expect_size_header, const std::string& log_name)
    {
        return boost::shared_ptr<TCPConnection>(new TCPConnection(io_service, callback, expect_size_header, log_name) );
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
            LOGTRACE(LT("TCPConnection("), this->log_name, LT(")::handle_read_header("), safe_local_endpoint(), LT("/"), safe_remote_endpoint(), LT(") - bytes_transferred: "), bytes_transferred);
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
        else
            LOGERROR(LT("TCPConnection("), this->log_name, LT(")::handle_read_header("), safe_local_endpoint(), LT("/"), safe_remote_endpoint(), LT(") - bytes_transferred: "), bytes_transferred, LT(" - ERROR: "), error.message());
    }

    std::string TCPConnection::safe_remote_endpoint() const
    {
        boost::system::error_code ec;
        auto rep = this->socket.remote_endpoint(ec).address();
        return ec ? ec.message() : rep.to_string();
    }

    std::string TCPConnection::safe_local_endpoint() const
    {
        boost::system::error_code ec;
        auto rep = this->socket.local_endpoint(ec).address();
        return ec ? ec.message() : rep.to_string();
    }

    void TCPConnection::handle_read_body(
        const boost::system::error_code& error,
        size_t bytes_transferred)
    {
        if (!error)
        {
            LOGTRACE(LT("TCPConnection("), this->log_name, LT(")::handle_read_body("), safe_local_endpoint(), LT("/"), safe_remote_endpoint(), LT(") - bytes_transferred: "), bytes_transferred);
            this->processMessage();
        }
        else
            LOGERROR(LT("TCPConnection("), this->log_name, LT(")::handle_read_body("), safe_local_endpoint(), LT("/"), safe_remote_endpoint(), LT(") - bytes_transferred: "), bytes_transferred, LT(" - ERROR: "), error.message());
    }

    void TCPConnection::handle_read_line(
        const boost::system::error_code& error,
        size_t bytes_transferred)
    {
        if (!error) {
            this->body_buffer.assign(
                boost::asio::buffers_begin(this->delimited_buffer.data()),
                boost::asio::buffers_begin(this->delimited_buffer.data()) + bytes_transferred
                );
            this->delimited_buffer.consume(bytes_transferred);
            this->processMessage();
        }
        else
            LOGERROR(LT("TCPConnection("), this->log_name, LT(")::handle_read_line("), safe_local_endpoint(), LT("/"), safe_remote_endpoint(), LT(") - bytes_transferred: "), bytes_transferred, LT(" - ERROR: "), error.message());
    }

    void TCPConnection::processMessage()
    {
        LOGFINE(LT("TCPConnection("), this->log_name, LT(")::processMessage("), safe_local_endpoint(), LT("/"), safe_remote_endpoint(), LT(") - bytes received: "), this->body_buffer.size());

        if (this->confirm_with_fixed_reply)
        {
            reply();
        }
        else
        {
            deliverMessage();
        }
    }

    void TCPConnection::reply()
    {
        const int REPLY_SIZE_HEADER_LENGTH = 4;
        this->reply_size_header = htonl((u_long)this->fixed_reply.size());

        // Send header and continue after with response body.
        boost::asio::async_write(this->socket, boost::asio::buffer(&this->reply_size_header, REPLY_SIZE_HEADER_LENGTH), boost::bind(&TCPConnection::transferredHeader, shared_from_this(), boost::asio::placeholders::error, boost::asio::placeholders::bytes_transferred));
    }

    void TCPConnection::transferredHeader(const boost::system::error_code& error, std::size_t bytes_transferred) {
        if (!error)
        {
            // Send body and continue after with message delivery.
            boost::asio::async_write(this->socket, boost::asio::buffer(this->fixed_reply), boost::bind(&TCPConnection::transferredBody, shared_from_this(), boost::asio::placeholders::error, boost::asio::placeholders::bytes_transferred));
        }
        else
        {
            LOGERROR(LT("TCPConnection("), this->log_name, LT(")::transferredHeader - failed to send header of message: "), error.message());
        }
    }

    void TCPConnection::transferredBody(const boost::system::error_code& error, std::size_t bytes_transferred) {
        if (!error)
        {
            LOGFINE(LT("TCPConnection("), this->log_name, LT(")::transferredBody sent "), bytes_transferred, LT(" bytes"));

            this->deliverMessage();
        }
        else
        {
            LOGERROR(LT("TCPConnection("), this->log_name, LT(")::transferredBody - failed to send body of message: "), error.message());
        }
    }

    void TCPConnection::deliverMessage()
    {
        this->onMessageReceived(TimestampedUnsignedCharVector(boost::posix_time::microsec_clock::universal_time(), this->body_buffer));
        this->read(); // Continue on with reading of next request message.
    }

    TCPConnection::TCPConnection(boost::asio::io_service& io_service, boost::function<void(const TimestampedUnsignedCharVector) > callback, bool expect_size_header, const std::string& log_name)
        : socket(io_service)
        , onMessageReceived(callback)
        , confirm_with_fixed_reply(false)
        , expect_size_header(expect_size_header)
        , log_name(log_name)
    {
    }

    size_t TCPConnection::getSizeFromHeader()
    {
        u_long size_in_network_byte_order = *reinterpret_cast<u_long*>(&(this->header_buffer[0]));
        u_long size_in_host_byte_order = ntohl(size_in_network_byte_order);
        return size_in_host_byte_order;
    }
}

#undef LOG_COMPONENT
