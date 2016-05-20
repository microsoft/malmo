// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _CLIENTPOOL_H_
#define _CLIENTPOOL_H_

// Local:
#include "ClientInfo.h"

// STL:
#include <vector>

namespace malmo 
{
    //! A pool of expected network locations of Mod clients.
    struct ClientPool 
    {
        //! Adds a client to the pool.
        //! \param client_info The client information.
        void add(const ClientInfo& client_info);

        std::vector< ClientInfo > clients; //!< The list of clients.
    };
}

#endif
