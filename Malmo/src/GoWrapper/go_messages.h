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

#ifndef MESSAGES_H
#define MESSAGES_H

#ifdef __cplusplus
extern "C" {
#endif

// global variable to communicate exception errors from C++ to Go
#define ERROR_MESSAGE_SIZE 1024
char ERROR_MESSAGE[ERROR_MESSAGE_SIZE];

// global variable to communicate usage messages from C++ to Go
#define USAGE_MESSAGE_SIZE 512
char USAGE_MESSAGE[USAGE_MESSAGE_SIZE];

// global variable to communicate summary messages from C++ to Go
#define SUMMARY_MESSAGE_SIZE 2048
char SUMMARY_MESSAGE[SUMMARY_MESSAGE_SIZE];

#define MAX_COMMAND_HANDLERS 100
#define COMMAND_HANDLER_SIZE 128
char COMMAND_HANDLERS[MAX_COMMAND_HANDLERS][COMMAND_HANDLER_SIZE];

#define MAX_ACTIVE_COMMAND_HANDLERS 1000
char ACTIVE_COMMAND_HANDLERS[MAX_ACTIVE_COMMAND_HANDLERS][COMMAND_HANDLER_SIZE];

#define MAKE_ERROR_MESSAGE(the_exception)                                 \
    std::string message = std::string("ERROR: ") + the_exception.what();  \
    strncpy(ERROR_MESSAGE, message.c_str(), ERROR_MESSAGE_SIZE);

#define MAKE_ERROR_MESSAGE_AH(the_exception, agent_host)                  \
    std::string message = std::string("ERROR: ") + the_exception.what();  \
    message += "\n\n" + agent_host->getUsage();                           \
    strncpy(ERROR_MESSAGE, message.c_str(), ERROR_MESSAGE_SIZE);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // MESSAGES_H
