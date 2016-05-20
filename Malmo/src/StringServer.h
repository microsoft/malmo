// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _STRINGSERVER_H_
#define _STRINGSERVER_H_

// Local:
#include "TCPServer.h"
#include "TimestampedString.h"

// Boost:
#include <boost/function.hpp>
#include <boost/thread.hpp>

// STL:
#include <fstream>
#include <memory>

namespace malmo
{
    //! A TCP server that receives strings and can optionally persist to file.
    class StringServer
    {
        public:

            //! Constructs a string server on a specified port.
            StringServer(boost::asio::io_service& io_service, int port, const boost::function<void(const TimestampedString string_message)> handle_string);
            
            StringServer& record(std::string path);
            
            StringServer& confirmWithFixedReply(std::string reply);
            
            StringServer& expectSizeHeader(bool expect_size_header);

            int getPort() const;

            //! Stop recording the data being received by the server.
            void stopRecording();

            void recordMessage(const TimestampedString message);

            //! Starts the string server.
            void start();

        private:

            void handleMessage(const TimestampedUnsignedCharVector message);

            boost::function<void(const TimestampedString string_message)> handle_string;
            TCPServer server;
            std::ofstream writer;
            boost::mutex write_mutex;
    };
}

#endif
