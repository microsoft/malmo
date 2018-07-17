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

#ifndef _TCPSERVER_H_
#define _TCPSERVER_H_

// Local:
#include "TCPConnection.h"

// Boost:
#include <boost/function.hpp>

namespace malmo
{
    class ServerScope {
    public:
        virtual void release() = 0;
    };

    //! A TCP server that calls a function you provide when a message is received.
    class TCPServer
    {
        public:

            //! Constructs a TCP server but doesn't start it.
            //! \param port The number of the port to connect to.
            //! \param callback The function to call when a message arrives.
            TCPServer(boost::asio::io_service& io_service, int port, boost::function<void(const TimestampedUnsignedCharVector) > callback, const std::string& log_name);
            
            void confirmWithFixedReply(std::string reply);
            void expectSizeHeader(bool expect_size_header);

            //! Starts the TCP server.
            void start(ServerScope* scope);

            //! Gets the port this server is listening on.
            //! \returns The port this server is listening on.
            int getPort() const;

            void close();

        private:

            virtual void startAccept();

            void handleAccept(
                boost::shared_ptr<TCPConnection> new_connection,
                const boost::system::error_code& error
            );

            void bindToPort(boost::asio::io_service& io_service, int port);
            void bindToRandomPortInRange(boost::asio::io_service& io_service, int port_min, int port_max);
            
            boost::shared_ptr< boost::asio::ip::tcp::acceptor > acceptor;
            
            boost::function<void(const TimestampedUnsignedCharVector) > onMessageReceived;
            
            bool confirm_with_fixed_reply;
            std::string fixed_reply;
            bool expect_size_header;
            std::string log_name;

            bool closing = false;
            ServerScope* scope = nullptr;
    };
}

#endif
