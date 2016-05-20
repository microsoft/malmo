// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Header:
#include "ClientPool.h"

namespace malmo 
{
    void ClientPool::add(const ClientInfo& client_info)
    {
        this->clients.push_back(client_info);
    }
}
