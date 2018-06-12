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

#ifndef _TCPCLIENT_H_
#define _TCPCLIENT_H_

// Boost:
#include <boost/asio/io_service.hpp>
#include <boost/date_time/posix_time/posix_time_types.hpp>

// STL:
#include <string>
#include <vector>

// Local:
#include "ErrorCodeSync.h"

namespace malmo
{
    inline std::vector<unsigned char> ToVector(std::string input)
    {
        return std::vector<unsigned char>(input.begin(), input.end());
    }

    //! Sends a string over TCP. Creates and closes a connection in the process.
    //! If sending many messages to the same place, use ClientConnection instead
    //! \param address The IP address of the remote endpoint.
    //! \param port The port of the remote endpoint.
    //! \param message The message to send.
    //! \param withSizeHeader If true, prepends a 4-byte integer containing the size of the message.
    //! \ref ClientConnection
    void SendOverTCP(boost::asio::io_service& io_service, std::string address, int port, std::vector<unsigned char> message, bool withSizeHeader);

    //! Sends a string over TCP. Creates and closes a connection in the process.
    //! If sending many messages to the same place, use ClientConnection instead
    //! \param address The IP address of the remote endpoint.
    //! \param port The port of the remote endpoint.
    //! \param message The message to send.
    //! \param withSizeHeader If true, prepends a 4-byte integer containing the size of the message.
    //! \ref ClientConnection
    void SendStringOverTCP(boost::asio::io_service& io_service, std::string address, int port, std::string message, bool withSizeHeader);
    
    class Rpc {
    public:
        //! Sends a string over TCP and reads a reply. Creates and closes a connection in the process.
        //! If sending many messages to the same place, use ClientConnection instead
        //! \param address The IP address of the remote endpoint.
        //! \param port The port of the remote endpoint.
        //! \param message The message to send.
        //! \param withSizeHeader If true, prepends a 4-byte integer containing the size of the message.
        //! \returns The reply as a string.
        //! \ref ClientConnection
        std::string sendStringAndGetShortReply(boost::asio::io_service& io_service, const std::string& ip_address, int port, const std::string& message, bool withSizeHeader);
    
        //! Set the request/reply timeout.
        //! \param seconds The timeout delay in seconds.
        void setTimeout(long seconds) {
            timeout = boost::posix_time::seconds(seconds);
        }

        //! Get the request/reply timeout.
        //! \returns The timeout delay in seconds.
        int64_t getTimeout() { return timeout.total_seconds();  }
    private:
        void transfer_handler(const boost::system::error_code& operation_ec, std::size_t transferred);

        boost::posix_time::time_duration timeout = boost::posix_time::seconds(60);
        ErrorCodeSync error_code_sync;
    };
}
#endif
