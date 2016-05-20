// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _TCPSERVER_H_
#define _TCPSERVER_H_

// Local:
#include "TCPConnection.h"

// Boost:
#include <boost/function.hpp>

namespace malmo
{
    //! A TCP server that calls a function you provide when a message is received.
    class TCPServer
    {
        public:

            //! Constructs a TCP server but doesn't start it.
            //! \param port The number of the port to connect to.
            //! \param callback The function to call when a message arrives.
            TCPServer(boost::asio::io_service& io_service, int port, boost::function<void(const TimestampedUnsignedCharVector) > callback);
            
            void confirmWithFixedReply(std::string reply);
            void expectSizeHeader(bool expect_size_header);

            //! Starts the TCP server.
            void start();

            //! Gets the port this server is listening on.
            //! \returns The port this server is listening on.
            int getPort() const;

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
    };
}

#endif
