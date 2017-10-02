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
        TCPServer server( io_service, PORT, onMessageReceived, "test" );
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
