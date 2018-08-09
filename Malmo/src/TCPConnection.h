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

#ifndef _TCPCONNECTION_H_
#define _TCPCONNECTION_H_

// Local:
#include "TimestampedUnsignedCharVector.h"

// Boost:
#include <boost/asio.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/function.hpp>

// STL:
#include <string>

namespace malmo
{
    //! Handles an individual TCP connection.
    class TCPConnection : public boost::enable_shared_from_this< TCPConnection >
    {
        public:

            static boost::shared_ptr<TCPConnection> create(
                boost::asio::io_service& io_service, 
                boost::function<void(const TimestampedUnsignedCharVector message) > callback,
                bool expect_size_header,
                const std::string& log_name);

            boost::asio::ip::tcp::socket& getSocket();

            void read();
            
            void confirmWithFixedReply(std::string reply);

        private:

            TCPConnection(boost::asio::io_service& io_service, boost::function<void(const TimestampedUnsignedCharVector) > callback, bool expect_size_header, const std::string& log_name);

            // called when we have successfully read the size header
            void handle_read_header( const boost::system::error_code& error, size_t bytes_transferred );

            // called when we have successfully read the body of the message, of known length
            void handle_read_body( const boost::system::error_code& error, size_t bytes_transferred );

            // called when we have successfully read the body of the message terminated by a newline
            void handle_read_line( const boost::system::error_code& error, size_t bytes_transferred );

            size_t getSizeFromHeader();

            void processMessage();
            void reply();
            void deliverMessage();

            void transferredHeader(const boost::system::error_code& ec, std::size_t transferred);
            void transferredBody(const boost::system::error_code& ec, std::size_t transferred);

        private:

            boost::asio::ip::tcp::socket socket;
            std::string safe_local_endpoint() const;
            std::string safe_remote_endpoint() const;

            static const int SIZE_HEADER_LENGTH = 4;
            boost::asio::streambuf delimited_buffer;
            std::vector<unsigned char> header_buffer;
            std::vector<unsigned char> body_buffer;

            boost::function<void(const TimestampedUnsignedCharVector message) > onMessageReceived;
            
            bool confirm_with_fixed_reply;
            std::string fixed_reply;
            bool expect_size_header;
            std::string log_name;
            u_long reply_size_header;
    };
}

#endif
