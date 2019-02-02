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

// Malmo:
#include <StringServer.h>
#include <TCPClient.h>

// Boost:
#include <boost/thread.hpp>
#include <boost/utility/in_place_factory.hpp>

// STL:
#include <atomic>
#include <cstdlib>
#include <iostream>
#include <string>

using namespace malmo;

std::atomic<int> num_messages_received;
const std::string expected_message( "hello from testStringServer!" );
const std::string expected_reply( "hello from StringServer!" );

void onMessageReceived( TimestampedString message )
{
	if (message.text != expected_message)
    {
        std::cout << "Unexpected message." << std::endl;
        exit( EXIT_FAILURE );
    }
    num_messages_received++;
}

Rpc rpc;

bool testStringServer( bool withReply )
{
    num_messages_received = 0;
    
    std::cout << "Starting server.." << std::endl;
    boost::asio::io_service io_service;
    boost::shared_ptr<StringServer> server = boost::make_shared<StringServer>( io_service, 0, onMessageReceived, "test" );
    server->start(server);
    if( withReply )
        server->confirmWithFixedReply( expected_reply );
   
    boost::thread bt(boost::bind(&boost::asio::io_service::run, &io_service));
    
    const int num_messages_sent = 100;
    const boost::posix_time::milliseconds sleep_time(100);
    
    boost::this_thread::sleep( sleep_time ); // allow time for the thread and server to start
    
    std::cout << "Sending messages.." << std::endl;
    for( int i = 0; i < num_messages_sent; i++ ) {
        if( withReply ) {
            std::string reply = rpc.sendStringAndGetShortReply(io_service, "127.0.0.1", server->getPort(), expected_message, true);
            if( reply != expected_reply ) {
                std::cout << "Unexpected reply." << std::endl;
                return false;
            }
        }
        else
            SendStringOverTCP(io_service, "127.0.0.1", server->getPort(), expected_message, true);
    }
    
    boost::this_thread::sleep( sleep_time ); // allow time for the messages to get through

    io_service.stop();
    bt.join();

    std::cout << "Exiting.." << std::endl;

    if( num_messages_received != num_messages_sent )
        return false;

    return true;
}

int main()
{
	for( int i = 0; i < 10; i++ )
	{
		if( !testStringServer( false ) )
			return EXIT_FAILURE;

		if (!testStringServer(true))
			return EXIT_FAILURE;
	}

    return EXIT_SUCCESS;
}
