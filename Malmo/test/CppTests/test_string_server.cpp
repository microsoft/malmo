// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

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

bool testStringServer( bool withReply )
{
    num_messages_received = 0;
    
    std::cout << "Starting server.." << std::endl;
    boost::asio::io_service io_service;
    StringServer server( io_service, 0, onMessageReceived );
    if( withReply )
        server.confirmWithFixedReply( expected_reply );
    server.start();
    boost::thread bt(boost::bind(&boost::asio::io_service::run, &io_service));
    
    const int num_messages_sent = 100;
    const boost::posix_time::milliseconds sleep_time(100);
    
    boost::this_thread::sleep( sleep_time ); // allow time for the thread and server to start
    
    std::cout << "Sending messages.." << std::endl;
    for( int i = 0; i < num_messages_sent; i++ ) {
        if( withReply ) {
            std::string reply = SendStringAndGetShortReply(io_service, "127.0.0.1", server.getPort(), expected_message, true);
            if( reply != expected_reply ) {
                std::cout << "Unexpected reply." << std::endl;
                return false;
            }
        }
        else
            SendStringOverTCP(io_service, "127.0.0.1", server.getPort(), expected_message, true);
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
