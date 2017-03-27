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

#ifndef CLIENTPOOL_H
#define CLIENTPOOL_H

#ifdef __cplusplus
extern "C" {
#endif

// max number of characters for error message from C++ to Go
#define CP_ERROR_BUFFER_SIZE 1024

// macro to help with calling ClientPool methods and handling exception errors
//   pt  -- pointer to ClientPool
//   err -- error message buffer
#define CP_CALL(command)                                          \
    ClientPool* client_pool = (ClientPool*)pt;                    \
    try {                                                         \
        command                                                   \
    } catch (const exception& e) {                                \
        std::string message = std::string("ERROR: ") + e.what();  \
        strncpy(err, message.c_str(), CP_ERROR_BUFFER_SIZE);      \
        return 1;                                                 \
    }                                                             \
    return 0;

// external structures
typedef void* ptClientInfo;

// pointer to ClientPool
typedef void* ptClientPool;

// constructor
ptClientPool new_client_pool();

// destructor
void free_client_pool(ptClientPool client_pool);

// All functions return:
//  0 = OK
//  1 = failed; e.g. exception happend => see ERROR_MESSAGE

int client_pool_add(ptClientPool pt, char* err, ptClientInfo client_info);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // CLIENTPOOL_H
