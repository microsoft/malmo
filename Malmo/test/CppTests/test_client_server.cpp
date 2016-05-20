// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Malmo:
#include <TCPServer.h>
#include <TCPClient.h>

// Boost:
#include <boost/thread.hpp>

// STL:
#include <atomic>
#include <cstdlib>
#include <iostream>
#include <string>

using namespace malmo;

std::atomic<int> num_messages_received(0);
const std::vector<unsigned char> expected_data = ToVector("hello from test_client_server.cpp!");

void onMessageReceived( TimestampedUnsignedCharVector message )
{
	if (message.data != expected_data)
        exit( EXIT_FAILURE );
    num_messages_received++;
}

int main()
{
    std::cout << "Starting server.." << std::endl;
    boost::asio::io_service io_service;
    TCPServer server( io_service, 0, onMessageReceived );
    server.start();
    boost::thread bt(boost::bind(&boost::asio::io_service::run, &io_service));
    
    const int num_messages_sent = 100;
    const boost::posix_time::milliseconds sleep_time(100);
    
    boost::this_thread::sleep( sleep_time ); // allow time for the thread and server to start
    
    std::cout << "Sending messages.." << std::endl;
    for( int i = 0; i < num_messages_sent; i++ ) {
        SendOverTCP(io_service, "127.0.0.1", server.getPort(), expected_data, true);
    }
    
    boost::this_thread::sleep( sleep_time ); // allow time for the messages to get through

    io_service.stop();
    bt.join();

    std::cout << "Exiting.." << std::endl;

    if( num_messages_received != num_messages_sent )
        return EXIT_FAILURE;

    return EXIT_SUCCESS;
}
