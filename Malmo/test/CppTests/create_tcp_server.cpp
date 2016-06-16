// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Malmo:
#include <TCPServer.h>

// STL:
#include <cstdlib>
#include <iostream>
#include <string>

using namespace malmo;

void onMessageReceived( TimestampedUnsignedCharVector message )
{
}

int main() 
{
    const int PORT = 0;
    try {
        boost::asio::io_service io_service;
        TCPServer server( io_service, PORT, onMessageReceived );
        int assigned_port = server.getPort();
        std::cout << "Server listening on port: " << assigned_port << std::endl;
        if( assigned_port < 10000 || assigned_port > 11000 )
            return EXIT_FAILURE;
    } catch( const std::exception& e ) {
        std::cout << "Error creating server: " << e.what() << std::endl;
        return EXIT_FAILURE;        
    }

    return EXIT_SUCCESS;
}
