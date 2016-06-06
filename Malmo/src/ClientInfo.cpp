// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Header:
#include "ClientInfo.h"
#include <sstream>

namespace malmo{

    ClientInfo::ClientInfo()
        : ClientInfo("", 0)
    {
    }

    ClientInfo::ClientInfo(const std::string& ip_address)
        : ClientInfo(ip_address, default_client_mission_control_port)
    {
    }

    ClientInfo::ClientInfo(const std::string& ip_address, int port)
        : ip_address(ip_address)
        , port(port)
    {
    }

    std::ostream& operator<<(std::ostream& os, const ClientInfo& ci)
    {
        os << "ClientInfo: " << ci.ip_address << ":" << ci.port;
        return os;
    }
}
