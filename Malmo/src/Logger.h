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

#pragma once

#include "AgentHost.h"

// Boost:
#include <boost/date_time/posix_time/posix_time_types.hpp>

// STL:
#include <string>

namespace malmo
{
    #define LOGINFO Logger::getLogger().print<AgentHost::LoggingSeverityLevel::LOG_INFO>

    class Logger
    {
    public:
        Logger() : line_number(0) {}
        ~Logger() {}

        static Logger& getLogger()
        {
            static Logger the_logger;
            return the_logger;
        }

        template<AgentHost::LoggingSeverityLevel level, typename...Args>
        void print(Args&&...args);

    private:
        template<typename First, typename... Others>
        void print_impl(std::stringstream&&, First&& param1, Others&&... params);
        void print_impl(std::stringstream&& message_stream);

        AgentHost::LoggingSeverityLevel severity_level;
        int line_number;
    };

    template<AgentHost::LoggingSeverityLevel level, typename...Args>void Logger::print(Args&&...args)
    {
        if (level > severity_level)
            return;
        std::stringstream message_stream;
        auto now = boost::posix_time::microsec_clock::universal_time();
        message_stream << line_number << " " << now << " ";
        switch (level)
        {
        case AgentHost::LoggingSeverityLevel::LOG_ALL:
        case AgentHost::LoggingSeverityLevel::LOG_ERRORS:
            message_stream << "ERROR   ";
            break;
        case AgentHost::LoggingSeverityLevel::LOG_WARNINGS:
            message_stream << "WARNING ";
            break;
        case AgentHost::LoggingSeverityLevel::LOG_INFO:
            message_stream << "INFO    ";
            break;
        case AgentHost::LoggingSeverityLevel::LOG_FINE:
        case AgentHost::LoggingSeverityLevel::LOG_OFF:
            message_stream << "FINE    ";
            break;
        }
        print_impl(std::forward<std::stringstream>(message_stream), std::move(args)...);
        this->line_number++;
    }

    template<typename First, typename... Others> void Logger::print_impl(std::stringstream&& message_stream, First&& param1, Others&&... params)
    {
        message_stream << param1;
        print_impl(std::forward<std::stringstream>(message_stream), std::move(params)...);
    }

    void Logger::print_impl(std::stringstream&& message_stream)
    {
        // do something.
        std::cout << message_stream.str() << std::endl;
    }
}
