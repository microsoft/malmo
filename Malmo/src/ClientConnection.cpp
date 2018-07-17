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

// Local:
#include "ClientConnection.h"
#include "Logger.h"

// Boost:
#include <boost/bind.hpp>

#define LOG_COMPONENT Logger::LOG_TCP

namespace malmo
{
    boost::shared_ptr<ClientConnection> ClientConnection::create(boost::asio::io_service& io_service, std::string address, int port)
    {
        return boost::shared_ptr<ClientConnection>(new ClientConnection(io_service, address, port));
    }

    void ClientConnection::send(std::string message)
    {
        this->io_service.post(boost::bind(&ClientConnection::writeImpl, shared_from_this(), message));
    }

    ClientConnection::ClientConnection(boost::asio::io_service& io_service, std::string address, int port)
        : io_service(io_service)
    {
        LOGTRACE(LT("Creating ClientConnection to "), address, LT(":"), port);

        this->resolver = std::unique_ptr<boost::asio::ip::tcp::resolver>(new boost::asio::ip::tcp::resolver(io_service));
        this->query = std::unique_ptr<boost::asio::ip::tcp::resolver::query>(new boost::asio::ip::tcp::resolver::query(address, std::to_string(port)));

        this->socket = std::unique_ptr<boost::asio::ip::tcp::socket>(new boost::asio::ip::tcp::socket(io_service));
        this->deadline = std::unique_ptr<boost::asio::deadline_timer>(new boost::asio::deadline_timer(io_service, this->timeout));

        this->deadline->async_wait([&](const boost::system::error_code& ec) {
            if (!ec)
            {
                LOGERROR(LT("Client communication connect timeout."));
                boost::system::error_code ignored_ec;
                this->socket->close(ignored_ec);
            }
        });

        this->connect_error_code = boost::asio::error::would_block;

        this->resolver->async_resolve(*query, [&](const boost::system::error_code& ec, boost::asio::ip::tcp::resolver::iterator endpoint_iterator) {
            if (ec || endpoint_iterator == boost::asio::ip::tcp::resolver::iterator())
            {
                LOGERROR(LT("Failed to resolve "), address, LT(":"), port, LT(" - "), ec.message());
                process(ec);
            }
            else
            {
                this->socket->async_connect(endpoint_iterator->endpoint(), [&](const boost::system::error_code& ec) {
                    LOGTRACE(LT("ClientConnection connected to "), address, LT(":"), port);
                    process(ec);
                });
            }
        });
    }

    ClientConnection::~ClientConnection()
    {
    }

    void ClientConnection::process(const boost::system::error_code& error) {

        // Stop deadline timer and release resolve resources.
        deadline.reset();
        resolver.release();
        query.release();

        // Record error code and start processing writes.
        boost::lock_guard<boost::mutex> scope_guard(this->outbox_mutex);
        this->connect_error_code = error;
        this->write();
    }

    void ClientConnection::writeImpl(std::string message)
    {
        boost::lock_guard<boost::mutex> scope_guard(this->outbox_mutex);

        if (message.back() != '\n')
            message += '\n';
        this->outbox.push_back(message);

        // Initiate async write(s) if we are not waiting to be connected and queue was empty.
        if (connect_error_code != boost::asio::error::would_block && this->outbox.size() == 1) {
            this->write();
        }
    }

    void ClientConnection::write()
    {
        if (this->outbox.size() == 0)
            return;

        if (connect_error_code) {
            LOGERROR(LT("Client connection with error (on write): "), connect_error_code.message());
            this->outbox.clear();
            return;
        }

        const std::string& message = this->outbox.front();

        boost::asio::async_write(
            *this->socket,
            boost::asio::buffer(message),
            boost::bind(
                &ClientConnection::wrote,
                shared_from_this(),
                boost::asio::placeholders::error,
                boost::asio::placeholders::bytes_transferred
            )
        );
    }

    void ClientConnection::wrote(const boost::system::error_code& error, size_t bytes_transferred)
    {
        if (error)
        {
            boost::system::error_code ec;
            LOGERROR(LT("Failed to write to "), this->socket->remote_endpoint(ec), LT(" - transferred "), bytes_transferred, LT(" bytes - "), error.message());
            if (ec)
                LOGERROR(LT("Error resolving remote endpoint: "), ec.message());
        }
        else
        {
            boost::system::error_code ec;
            LOGTRACE(LT("Successfully wrote "), this->outbox.front(), LT(" to "), this->socket->remote_endpoint(ec));
            if (ec)
                LOGERROR(LT("Error resolving remote endpoint: "), ec.message());
            boost::lock_guard<boost::mutex> scope_guard(this->outbox_mutex);
            if (!this->outbox.empty())
                this->outbox.pop_front();
        }
        if (!this->outbox.empty())
            this->write();
    }
}

#undef LOG_COMPONENT
