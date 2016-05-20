// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "ClientConnection.h"

// Boost:
#include <boost/bind.hpp>

namespace malmo
{
    boost::shared_ptr< ClientConnection > ClientConnection::create( boost::asio::io_service& io_service, std::string address, int port )
    {
        return boost::shared_ptr< ClientConnection >(new ClientConnection( io_service, address, port ) );
    }
    
    void ClientConnection::send( std::string message )
    {
        this->io_service.post( boost::bind( &ClientConnection::writeImpl, this, message ) );
    }

    ClientConnection::ClientConnection( boost::asio::io_service& io_service, std::string address, int port )
        : io_service( io_service )
    {
        // connect the socket to the requested endpoint
        boost::asio::ip::tcp::resolver resolver( io_service );
        boost::asio::ip::tcp::resolver::query query( address, std::to_string( port ) );
        boost::asio::ip::tcp::resolver::iterator endpoint_iterator = resolver.resolve( query );
        this->socket = std::unique_ptr< boost::asio::ip::tcp::socket >( new boost::asio::ip::tcp::socket( io_service ) );
        boost::asio::connect( *this->socket, endpoint_iterator );            
    }
    
    ClientConnection::~ClientConnection()
    {        
    }
        
    void ClientConnection::writeImpl( std::string message )
    {
        if( message.back() != '\n' )
            message += '\n';
        this->outbox.push_back( message );
        if ( this->outbox.size() > 1 ) {
            // outstanding write
            return;
        }

        this->write();
    }
    
    void ClientConnection::write()
    {
        const std::string& message = this->outbox.front();
        boost::asio::async_write(
            *this->socket,
            boost::asio::buffer( message ),
            boost::bind(
                &ClientConnection::wrote,
                shared_from_this(),
                boost::asio::placeholders::error,
                boost::asio::placeholders::bytes_transferred
            ) 
        );
    }

    void ClientConnection::wrote( const boost::system::error_code& error,
                                  size_t bytes_transferred )
    {        
        this->outbox.pop_front();
        if( !this->outbox.empty() )
            this->write();  
    }
}
