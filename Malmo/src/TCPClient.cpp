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
#include "TCPClient.h"
#include "Logger.h"

// Boost:
#include <boost/thread.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/asio.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/asio/deadline_timer.hpp>
#include <boost/lambda/lambda.hpp>


using boost::asio::ip::tcp;

#define LOG_COMPONENT Logger::LOG_TCP

namespace malmo
{
    void SendOverTCP(boost::asio::io_service& io_service, std::string address, int port, std::vector<unsigned char> message, bool withSizeHeader)
    {
        tcp::resolver resolver(io_service);
        tcp::resolver::query query(address, boost::lexical_cast<std::string>(port));
        tcp::resolver::iterator endpoint_iterator;
        try
        {
            endpoint_iterator = resolver.resolve(query);
        }
        catch (boost::system::system_error e)
        {
            LOGERROR(LT("Failed to resolve endpoint for "), address, LT(":"), port, LT(" - "), e.code().message());
            throw e;
        }

        tcp::socket socket(io_service);
        try
        {
            boost::asio::connect(socket, endpoint_iterator);
        }
        catch (boost::system::system_error e)
        {
            LOGERROR(LT("Failed to connect to "), address, LT(":"), port, LT(" - "), e.code().message());
            throw e;
        }

        if (withSizeHeader)
        {
            // the size header is 4 bytes containing the size of the body of the message as a network byte order integer
            const int SIZE_HEADER_LENGTH = 4;
            u_long size_header = htonl((u_long)message.size());
            try
            {
                boost::asio::write(socket, boost::asio::buffer(&size_header, SIZE_HEADER_LENGTH));
            }
            catch (boost::system::system_error e)
            {
                LOGERROR(LT("Failed to write header to "), address, LT(":"), port, LT(" - "), e.code().message());
                throw e;
            }
        }
        else if (message.back() != '\n')
        {
            // if not sending with a size header then the message must be newline-terminated
            message.push_back('\n');
        }
        
        try
        {
            boost::asio::write(socket, boost::asio::buffer(message));
        }
        catch (boost::system::system_error e)
        {
            LOGERROR(LT("Failed to write message to "), address, LT(":"), port, LT(" - "), e.code().message());
            throw e;
        }
    }

    void SendStringOverTCP(boost::asio::io_service& io_service, std::string address, int port, std::string message, bool withSizeHeader)
    {
        SendOverTCP(io_service,address, port, std::vector<unsigned char>(message.begin(), message.end()), withSizeHeader);
    }

    std::string Rpc::sendStringAndGetShortReply(boost::asio::io_service& io_service, const std::string& ip_address, int port, const std::string& message, bool withSizeHeader)
    {
        const u_long MAX_PACKET_LENGTH = 1024;
        unsigned char data[MAX_PACKET_LENGTH + 1];
        std::vector<unsigned char> message_vector(message.begin(), message.end());

        tcp::resolver resolver(io_service);
        tcp::resolver::query query(ip_address, boost::lexical_cast<std::string>(port));
        tcp::socket socket(io_service);

        // Set up a deadline timer to close the socket on timeout, aborting any async io.

        boost::asio::deadline_timer deadline(io_service, timeout);

        deadline.async_wait([&](const boost::system::error_code& ec) {
            if (!ec)
            {
                LOGERROR(LT("Request/Reply communication timeout."));
                boost::system::error_code ignored_ec;
                socket.close(ignored_ec);
            }
        });

        // Attempt the resolve & connect. Relying on background worker thread to progress the async io.

        error_code_sync.init_error_code();

        resolver.async_resolve(query, [&](const boost::system::error_code& ec, tcp::resolver::iterator endpoint_iterator) {
            if (ec)
            {
                LOGERROR(LT("Failed to resolve "), ip_address, LT(":"), port, LT(" - "), ec.message());
                error_code_sync.signal_error_code(ec);
            }
            else
            {
                if (endpoint_iterator == tcp::resolver::iterator()) {
                    error_code_sync.signal_error_code(boost::asio::error::fault);
                }
                else {
                    socket.async_connect(endpoint_iterator->endpoint(), boost::bind(&ErrorCodeSync::signal_error_code, &this->error_code_sync, boost::asio::placeholders::error));
                }
            }
        });

        auto error_code = error_code_sync.await_error_code();

        if (error_code || !socket.is_open())
        {
            LOGERROR(LT("Failed to connect to "), ip_address, LT(":"), port, LT(" - "), error_code.message());
            throw std::runtime_error("Could not connect  " + (error_code ? error_code : boost::asio::error::operation_aborted).message());
        }

        error_code_sync.init_error_code();

        // The size header is 4 bytes containing the size of the body of the message as a network byte order integer.
        const int SIZE_HEADER_LENGTH = 4;
        u_long size_header;

        if (withSizeHeader)
        {
            u_long size_header = htonl((u_long)message.size());

            boost::asio::async_write(socket, boost::asio::buffer(&size_header, SIZE_HEADER_LENGTH), [&](const boost::system::error_code& ec, std::size_t transferred) {
                if (!ec)
                {
                    boost::asio::async_write(socket, boost::asio::buffer(message_vector), boost::bind(&Rpc::transfer_handler, this, boost::asio::placeholders::error, boost::asio::placeholders::bytes_transferred));
                }
                else
                {
                    transfer_handler(ec, transferred);
                }
            });
        }
        else 
        {
            boost::asio::async_write(socket, boost::asio::buffer(message_vector), boost::bind(&Rpc::transfer_handler, this, boost::asio::placeholders::error, boost::asio::placeholders::bytes_transferred));
        }

        error_code = error_code_sync.await_error_code();

        if (error_code)
            throw std::runtime_error("Request write failed " + error_code.message());

        error_code_sync.init_error_code();

        boost::asio::async_read(socket, boost::asio::buffer(&size_header, SIZE_HEADER_LENGTH), boost::asio::transfer_exactly(SIZE_HEADER_LENGTH), 
            [&](const boost::system::error_code& ec, std::size_t transferred) {
                if (!ec) {
                    size_header = ntohl(size_header);

                    if (size_header > MAX_PACKET_LENGTH)
                    {
                        LOGERROR(LT("Packet length of "), size_header, LT(" received from "), ip_address, LT(":"), port, LT(" exceeds maximum allowed."));
                        transfer_handler(boost::asio::error::fault, transferred);
                    }
                    else
                    {
                        boost::asio::async_read(socket, boost::asio::buffer(data, size_header), boost::asio::transfer_exactly(size_header), boost::bind(&Rpc::transfer_handler, this, boost::asio::placeholders::error, boost::asio::placeholders::bytes_transferred));
                    }
                }
                else
                {
                    transfer_handler(ec, transferred);
                }
        });

        error_code = error_code_sync.await_error_code();

        if (error_code)
            throw std::runtime_error("Response read failed " + error_code.message());

        std::string reply(data, data + size_header);

        if (!error_code)
            LOGFINE(LT("Received reply from  "), ip_address, LT(":"), port, LT(" - "), reply);

        return reply;
    }

    void Rpc::transfer_handler(const boost::system::error_code& ec, std::size_t transferred) {
        error_code_sync.signal_error_code(ec);
    }
}

#undef LOG_COMPONENT
