// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _CLIENTCONNECTION_H_
#define _CLIENTCONNECTION_H_

// STL:
#include <deque>
#include <memory>
#include <string>

// Boost:
#include <boost/asio.hpp>
#include <boost/enable_shared_from_this.hpp>

namespace malmo
{
    //! Opens a TCP connection and sends newline-terminated strings on request.
    //! If you only need to send one-off messages, use SendStringOverTCP instead.
    //! \ref SendStringOverTCP
    class ClientConnection : public boost::enable_shared_from_this< ClientConnection >
    {
        public:
        
            //! Opens a TCP connection.
            //! \param address The IP address of the remote endpoint.
            //! \param port The port of the remote endpoint.
            //! \returns The connection as a shared pointer.
            static boost::shared_ptr< ClientConnection > create( boost::asio::io_service& io_service, std::string address, int port );        
            
            //! Sends a string over the open connection.
            //! \param message The string to send. Will have newline appended if needed.
            void send( std::string message );
        
            ~ClientConnection();
            
        private:
        
            ClientConnection( boost::asio::io_service& io_service, std::string address, int port );
            
            void writeImpl( std::string message );

            void write();

            void wrote( const boost::system::error_code& error,
                        size_t bytes_transferred );
            
            void read();
            
            boost::asio::io_service& io_service;
            boost::shared_ptr< boost::asio::ip::tcp::socket > socket;
            std::deque< std::string > outbox;
    };
}

#endif
