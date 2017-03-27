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
#include <ClientInfo.h>
using namespace malmo;

// STL:
#include <cstddef>
#include <cstring>
#include <string>
#include <vector>
#include <exception>
using namespace std;

// Local:
#include "x_client_info.h"

ptClientInfo new_client_info() {
    ClientInfo * pt = new ClientInfo;
    return (void*)pt;
}

ptClientInfo new_client_info_address(const char* address) {
    ClientInfo * pt = new ClientInfo(address);
    return (void*)pt;
}

ptClientInfo new_client_info_address_and_port(const char* address, int port) {
    ClientInfo * pt = new ClientInfo(address, port);
    return (void*)pt;
}

void free_client_info(ptClientInfo client_info) {
    if (client_info != NULL) {
        ClientInfo * pt = (ClientInfo*)client_info;
        delete pt;
    }
}
