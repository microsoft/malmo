// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "TCPClient.h"

// Boost:
#include <boost/asio.hpp>
#include <boost/lexical_cast.hpp>
using boost::asio::ip::tcp;

namespace malmo
{
    void SendOverTCP(boost::asio::io_service& io_service, std::string address, int port, std::vector<unsigned char> message, bool withSizeHeader)
    {
        tcp::resolver resolver(io_service);
        tcp::resolver::query query(address, boost::lexical_cast<std::string>(port));
        tcp::resolver::iterator endpoint_iterator = resolver.resolve(query);

        tcp::socket socket(io_service);
        boost::asio::connect(socket, endpoint_iterator);

        if (withSizeHeader)
        {
            // the size header is 4 bytes containing the size of the body of the message as a network byte order integer
            const int SIZE_HEADER_LENGTH = 4;
            u_long size_header = htonl((u_long)message.size());
            boost::asio::write(socket, boost::asio::buffer(&size_header, SIZE_HEADER_LENGTH));
        }
        else if (message.back() != '\n')
        {
            // if not sending with a size header then the message must be newline-terminated
            message.push_back('\n');
        }
        
        boost::asio::write(socket, boost::asio::buffer(message));
    }

    void SendStringOverTCP(boost::asio::io_service& io_service, std::string address, int port, std::string message, bool withSizeHeader)
    {
        SendOverTCP(io_service,address, port, std::vector<unsigned char>(message.begin(), message.end()), withSizeHeader);
    }
    
    std::string SendStringAndGetShortReply(boost::asio::io_service& io_service, const std::string& ip_address, int port, const std::string& message, bool withSizeHeader)
    {
        const int MAX_PACKET_LENGTH = 1024;
        unsigned char data[MAX_PACKET_LENGTH + 1];
        
        std::vector<unsigned char> message_vector(message.begin(), message.end());
        
        tcp::resolver resolver(io_service);
        tcp::resolver::query query(ip_address, boost::lexical_cast<std::string>(port));
        tcp::resolver::iterator endpoint_iterator = resolver.resolve(query);

        tcp::socket socket(io_service);
        boost::asio::connect(socket, endpoint_iterator);

        const int SIZE_HEADER_LENGTH = 4;
        u_long size_header;

        if (withSizeHeader)
        {
            // the size header is 4 bytes containing the size of the body of the message as a network byte order integer
            const int SIZE_HEADER_LENGTH = 4;
            u_long size_header = htonl((u_long)message.size());
            boost::asio::write(socket, boost::asio::buffer(&size_header, SIZE_HEADER_LENGTH));
        }

        boost::asio::write(socket, boost::asio::buffer(message_vector));

        boost::asio::read(socket, boost::asio::buffer(&size_header, SIZE_HEADER_LENGTH), boost::asio::transfer_exactly(SIZE_HEADER_LENGTH));

        size_header = ntohl(size_header);
        
        if( size_header > MAX_PACKET_LENGTH )
            throw std::runtime_error( "Packet length exceeds maximum allowed." );

        boost::asio::read(socket, boost::asio::buffer(data, size_header), boost::asio::transfer_exactly(size_header) );

        std::string reply(data, data + size_header);

        return reply;
    }
}
