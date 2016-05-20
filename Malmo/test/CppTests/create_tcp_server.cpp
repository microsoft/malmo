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
    const int PORT = 12312;
    try {
        boost::asio::io_service io_service;
        TCPServer server( io_service, PORT, onMessageReceived );
        if( server.getPort() != PORT )
            return EXIT_FAILURE;
    } catch( const std::exception& e ) {
        std::cout << "Error creating server: " << e.what() << std::endl;
        return EXIT_FAILURE;        
    }

    return EXIT_SUCCESS;
}
