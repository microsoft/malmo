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
#include <ClientPool.h>
using namespace malmo;

// STL:
#include <cstddef>
#include <cstring>
#include <string>
#include <vector>
#include <exception>
using namespace std;

// Local:
#include "x_client_pool.h"

ptClientPool new_client_pool() {
    ClientPool * pt = new ClientPool;
    return (void*)pt;
}

void free_client_pool(ptClientPool client_pool) {
    if (client_pool != NULL) {
        ClientPool * pt = (ClientPool*)client_pool;
        delete pt;
    }
}

int client_pool_add(ptClientPool pt, char* err, ptClientInfo ptclient_info) {
    CP_CALL(
        ClientInfo* client_info = (ClientInfo*)ptclient_info;
        client_pool->add(*client_info);
    )
}
