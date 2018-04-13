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

#ifndef _CLIENTINFO_H_
#define _CLIENTINFO_H_

// STL:
#include <string>

namespace malmo 
{
    enum { 
        default_client_mission_control_port = 10000 //!< The default client mission control port
    };

    //! Structure containing information about a simulation client's address and port
    struct ClientInfo 
    {
        //! Constructs an empty ClientInfo struct.
        ClientInfo();

        //! Constructs a ClientInfo at the specified address listening on the default port.
        //! \param ip_address The IP address of the client
        ClientInfo(const std::string& ip_address);

        //! Constructs a ClientInfo at the specified address listening on the specified port.
        //! \param ip_address The IP address of the client.
        //! \param control_port The number of the client mission control port.
        ClientInfo(const std::string& ip_address, int control_port);

        //! Constructs a ClientInfo at the specified address listening on the specified port.
        //! \param ip_address The IP address of the client.
        //! \param control_port The number of the client mission control port.
        //! \param command_port The number of the client mission command port.
        ClientInfo(const std::string& ip_address, int control_port, int command_port);

        //! The IP address of the client.
        std::string ip_address;

        //! The control port of the client. Defaults to the default client mission control port.
        int control_port;

        //! The command port of the client. Default of 0 causes client to dynamically allocate one.
        int command_port;

        friend std::ostream& operator<<(std::ostream& os, const ClientInfo& ci);
    };
}

#endif
