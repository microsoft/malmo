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

#ifndef _CLIENTCONNECTION_H_
#define _CLIENTCONNECTION_H_

// STL:
#include <deque>
#include <memory>
#include <string>

// Boost:
#include <boost/asio.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/thread.hpp>

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
            boost::mutex outbox_mutex;
    };
}

#endif
