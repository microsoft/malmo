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

#ifndef _STRINGSERVER_H_
#define _STRINGSERVER_H_

// Local:
#include "TCPServer.h"
#include "TimestampedString.h"

// Boost:
#include <boost/function.hpp>
#include <boost/thread.hpp>
#include <boost/shared_ptr.hpp>

// STL:
#include <fstream>
#include <memory>
#include <string>

namespace malmo
{
    //! A TCP server that receives strings and can optionally persist to file.
    class StringServer : ServerScope
    {
        public:

            //! Constructs a string server on a specified port.
            StringServer(boost::asio::io_service& io_service, int port, const boost::function<void(const TimestampedString string_message)> handle_string, const std::string& log_name);
            
            StringServer& record(std::string path);
            
            StringServer& confirmWithFixedReply(std::string reply);
            
            StringServer& expectSizeHeader(bool expect_size_header);

            int getPort() const;

            //! Stop recording the data being received by the server.
            void stopRecording();

            void recordMessage(const TimestampedString message);

            //! Starts the string server.

            void start(boost::shared_ptr<StringServer>& scope);

            virtual void release();

            void close();

        private:

            void handleMessage(const TimestampedUnsignedCharVector message);

            boost::function<void(const TimestampedString string_message)> handle_string;
            boost::asio::io_service& io_service;
            int port;
            const std::string log_name;
            boost::shared_ptr<TCPServer> server;
            std::ofstream writer;
            boost::mutex write_mutex;

            boost::shared_ptr<StringServer> scope = nullptr;
    };
}

#endif
