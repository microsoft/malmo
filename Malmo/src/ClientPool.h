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

#ifndef _CLIENTPOOL_H_
#define _CLIENTPOOL_H_

// Local:
#include "ClientInfo.h"
#include "Logger.h"

// STL:
#include <vector>

namespace malmo 
{
    //! A pool of expected network locations of Mod clients.
    struct ClientPool 
    {
        MALMO_LOGGABLE_OBJECT(ClientPool)

        //! Adds a client to the pool.
        //! \param client_info The client information.
        void add(const ClientInfo& client_info);

        std::vector< boost::shared_ptr<ClientInfo> > clients; //!< The list of clients.
        friend std::ostream& operator<<(std::ostream& os, const ClientPool& cp);
    };
}

#endif
