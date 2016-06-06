// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

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
        //! \param port The number of the client port.
        ClientInfo(const std::string& ip_address, int port);

        //! The IP address of the client.
        std::string ip_address;

        //! The port of the client.
        int port;

        friend std::ostream& operator<<(std::ostream& os, const ClientInfo& ci);
    };
}

#endif
