// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Header:
#include "ClientPool.h"
#include <sstream>

namespace malmo 
{
    void ClientPool::add(const ClientInfo& client_info)
    {
        this->clients.push_back(client_info);
    }
    std::ostream& operator<<(std::ostream& os, const ClientPool& cp)
    {
        os << "ClientPool";
        if (cp.clients.size())
            os << ":";
        else
            os << " (empty)";
        for (auto ci : cp.clients)
        {
            os << "\n    " << ci;
        }
        return os;
    }
}
