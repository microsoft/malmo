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

// Header:
#include "ClientInfo.h"
#include <sstream>

namespace malmo{

    ClientInfo::ClientInfo()
        : ClientInfo("", 0, 0)
    {
    }

    ClientInfo::ClientInfo(const std::string& ip_address)
        : ClientInfo(ip_address, default_client_mission_control_port, 0)
    {
    }

    ClientInfo::ClientInfo(const std::string& ip_address, int control_port)
        : ip_address(ip_address)
        , control_port(control_port)
        , command_port(0)
    {
    }

    ClientInfo::ClientInfo(const std::string& ip_address, int control_port, int command_port)
        : ip_address(ip_address)
        , control_port(control_port)
        , command_port(command_port)
    {
    }

    std::ostream& operator<<(std::ostream& os, const ClientInfo& ci)
    {
        os << "ClientInfo: " << ci.ip_address << ":" << ci.control_port << ":" << ci.command_port;
        return os;
    }
}
