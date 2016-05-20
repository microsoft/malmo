// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Boost:
#include <boost/asio/io_service.hpp>

// STL:
#include <string>
#include <vector>

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
    
    //! Sends a string over TCP and reads a reply. Creates and closes a connection in the process.
    //! If sending many messages to the same place, use ClientConnection instead
    //! \param address The IP address of the remote endpoint.
    //! \param port The port of the remote endpoint.
    //! \param message The message to send.
    //! \param withSizeHeader If true, prepends a 4-byte integer containing the size of the message.
    //! \returns The reply as a string.
    //! \ref ClientConnection
    std::string SendStringAndGetShortReply(boost::asio::io_service& io_service, const std::string& ip_address, int port, const std::string& message, bool withSizeHeader);
}
