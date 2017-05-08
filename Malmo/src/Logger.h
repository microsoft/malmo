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

#ifndef _MALMO_LOGGER_H_
#define _MALMO_LOGGER_H_

// Ideally we'd use boost::log rather than reinventing the wheel here, but due to our requirements to be able
// to use boost statically, boost::log is not an option:
// "If your application consists of more than one module (e.g. an exe and one or several dll's) that use Boost.Log,
// the library must be built as a shared object. If you have a single executable or a single module that works
// with Boost.Log, you may build the library as a static library."
// (from http://www.boost.org/doc/libs/1_64_0/libs/log/doc/html/log/installation/config.html )

// The logger can act straight away (when Logger::print is called), but for performance reasons it is better to
// buffer the log messages and dump them en masse periodically, preferably in a background thread.
// Unfortuantely there are some hairy object lifetime difficulties involved in keeping our *own* thread, since the
// logger is a static singleton, which will be destroyed after main() has exited, which, with VS2013 at least,
// will cause a deadlock our thread hasn't already joined() - 
// see https://connect.microsoft.com/VisualStudio/feedback/details/747145 for example.

// To get around this, we use the small LoggerLifetimeTracker class, and tie the creation/destruction of the worker
// thread to this. When the first LoggerLiftetimeTracker class is created, it will create the worker thread;
// when the last one is deleted, it will join() the thread and delete it. Each user-instantiable class (eg AgentHost,
// MissionRecordSpec etc) contains an instance of a LoggerLifetimeTracker, so, roughly speaking, the thread will get
// created when the user creates any Malmo objects, and will get deleted when they are no longer needed - eg when
// the python script exits, or they all go out of scope. After this has happened, the next Malmo object to get
// created will simply generate a new thread - the Logger should be able to cope with threads coming and going
// in this way.

// Of course, this can give rise to pathalogical cases where, for example, a user creates Python code that repeatedly
// creates and destroys Malmo objects, leading to repeated creation/destruction of worker threads.
// In practice this case should be rare, and shouldn't create any major performance problems
// since the time-critical part of the code is during the running of missions, where there should always
// be at least one AgentHost object persisting throughout.

// This logger partly owes its genesis to this Dr Dobbs article:
// http://www.drdobbs.com/parallel/more-memory-for-dos-exec/parallel/a-lightweight-logger-for-c/240147505
// and the associated github project by Filip Janiszewski - https://github.com/fjanisze/logger
// Note however that the code in github will not work on Windows (under VS2013 at least) for several reasons -
// firstly for the thread lifetime issue above, secondly because of the way VS treats std::move of string literals -
// see http://stackoverflow.com/questions/34160614/stdmove-of-string-literal-which-compiler-is-correct for an example.
// Thirdly VS2013 doesn't support the use of the ATOMIC_FLAG_INIT initialisation macro (eg see
// https://connect.microsoft.com/VisualStudio/feedback/details/800243/visual-studio-2013-rc-std-atomic-flag-regression etc.)

// Boost:
#include <boost/date_time/posix_time/posix_time_types.hpp>
#include <boost/date_time/posix_time/posix_time_io.hpp>

// STL:
#include <string>
#include <mutex>
#include <thread>
#include <atomic>
#include <vector>
#include <sstream>
#include <iostream>
#include <fstream>
#include <algorithm>

namespace malmo
{
    #define LOGERROR(...) Logger::getLogger().print<Logger::LOG_ERRORS>(__VA_ARGS__)
    #define LOGINFO(...) Logger::getLogger().print<Logger::LOG_INFO>(__VA_ARGS__)
    #define LOGFINE(...) Logger::getLogger().print<Logger::LOG_FINE>(__VA_ARGS__)
    #define LOGTRACE(...) Logger::getLogger().print<Logger::LOG_TRACE>(__VA_ARGS__)
    #define LOGSIMPLE(level, message) Logger::getLogger().print<Logger:: level >(std::string(message))
    #define LOGSECTION(level, message) LogSection<Logger:: level > log_section(message);
    #define LT(x) std::string(x)
    #define MALMO_LOGGABLE_OBJECT(name) LoggerLifetimeTracker log_tracker{ #name };

    class Logger
    {
    public:
        //! Specifies the detail that will be logged, if logging is enabled.
        enum LoggingSeverityLevel {
            LOG_OFF
            , LOG_ERRORS
            , LOG_WARNINGS
            , LOG_INFO
            , LOG_FINE
            , LOG_TRACE
            , LOG_ALL
        };

        Logger() : line_number(0), indentation(0)
        {
            this->has_backend = false;
            this->is_spooling.clear();
        }
        ~Logger()
        {
            if (this->has_backend)
            {
                // Ideally, our backend thread would have been joined and deleted by now.
                // If users have created static objects which outlive the logger this may not be the case.
                // We need to switch off logging now, so that when they are finally destructed, they
                // won't cause havoc by attempting to access the logger again.
                this->severity_level = LOG_OFF;
                this->is_spooling.clear();
            }

            // Clear whatever is left in our buffer:
            std::lock_guard< std::timed_mutex > lock(write_guard);
            clear_backlog();
            // And close our file, if we have one:
            if (this->writer.is_open())
            {
                this->writer.close();
            }
        }
        Logger(const malmo::Logger &) = delete;

        static Logger& getLogger()
        {
            static Logger the_logger;
            return the_logger;
        }

        template<LoggingSeverityLevel level, typename...Args>
        void print(Args&&...args);
        void setSeverityLevel(LoggingSeverityLevel level) { severity_level = level; }
        void setFilename(const std::string& file)
        {
            if (this->writer.is_open())
                this->writer.close();
            this->writer.open(file, std::ofstream::out | std::ofstream::app);
        }

        //! Sets logging options for debugging.
        //! \param filename A filename to output log messages to. Will use the console if this is empty / can't be written to.
        //! \param severity_level Determine how verbose the log will be.
        static void setLogging(const std::string& filename, LoggingSeverityLevel severity_level)
        {
            Logger::getLogger().setFilename(filename);
            Logger::getLogger().setSeverityLevel(severity_level);
        }

    protected:
        template<LoggingSeverityLevel level> friend class LogSection;
        friend class LoggerLifetimeTracker;

        void indent()
        {
            std::lock_guard< std::timed_mutex > lock(write_guard);
            indentation++;
        }
        void unindent()
        {
            std::lock_guard< std::timed_mutex > lock(write_guard);
            indentation--;
        }
        void releaseBackend()
        {
            this->is_spooling.clear();
        }

        static void log_spooler(Logger* logger)
        {
            logger->has_backend = true;
            logger->is_spooling.test_and_set();
            std::unique_lock<std::timed_mutex> writing_lock{ logger->write_guard, std::defer_lock };
            do
            {
                std::this_thread::sleep_for(std::chrono::milliseconds{ 100 });
                if (logger->log_buffer.size())
                {
                    writing_lock.lock();
                    logger->clear_backlog();
                    writing_lock.unlock();
                }
            } while (logger->is_spooling.test_and_set());
            logger->has_backend = false;
        }

        void clear_backlog()
        {
            for (auto& item : this->log_buffer)
            {
                performWrite(item);
            }
            this->log_buffer.clear();
        }

        void performWrite(const std::string& logline)
        {
            std::string str(logline);
            str.erase(std::remove(str.begin(), str.end(), '\n'), str.end());
            if (this->writer.is_open())
                this->writer << str << std::endl;
            else
                std::cout << str << std::endl;
        }

    private:
        friend void log_spooler(Logger* logger);

        template<typename First, typename... Others> void print_impl(std::stringstream&& message_stream, First&& param1, Others&&... params)
        {
            message_stream << param1;
            print_impl(std::forward<std::stringstream>(message_stream), std::move(params)...);
        }

        void print_impl(std::stringstream&& message_stream)
        {
            std::lock_guard< std::timed_mutex > lock(this->write_guard);
            if (this->has_backend)
                this->log_buffer.push_back(message_stream.str());
            else
            {
                clear_backlog();
                performWrite(message_stream.str());
            }
        }

        LoggingSeverityLevel severity_level{ LOG_OFF };
        int line_number;
        int indentation;
        std::timed_mutex write_guard;
        std::vector<std::string> log_buffer;
        std::thread spooler;
        std::atomic_bool has_backend;
        std::atomic_flag is_spooling;
        bool terminated;
        std::ofstream writer;
    };

    template<Logger::LoggingSeverityLevel level, typename...Args>void Logger::print(Args&&...args)
    {
        if (level > severity_level)
            return;
        std::stringstream message_stream;
        auto now = boost::posix_time::microsec_clock::universal_time();
        message_stream << line_number << " " << now << " ";
        switch (level)
        {
        case LoggingSeverityLevel::LOG_ALL:
        case LoggingSeverityLevel::LOG_ERRORS:
            message_stream << "ERROR   ";
            break;
        case LoggingSeverityLevel::LOG_WARNINGS:
            message_stream << "WARNING ";
            break;
        case LoggingSeverityLevel::LOG_INFO:
            message_stream << "INFO    ";
            break;
        case LoggingSeverityLevel::LOG_FINE:
            message_stream << "FINE    ";
            break;
        case LoggingSeverityLevel::LOG_TRACE:
        case LoggingSeverityLevel::LOG_OFF:
            message_stream << "TRACE   ";
            break;
        }
        for (int i = 0; i < this->indentation; i++)
            message_stream << "    ";

        print_impl(std::forward<std::stringstream>(message_stream), std::move(args)...);
        this->line_number++;
    }

    template<Logger::LoggingSeverityLevel level>
    class LogSection
    {
    public:
        LogSection(const std::string& title)
        {
            Logger::getLogger().print<level>(title);
            Logger::getLogger().print<level>(std::string("{"));
            Logger::getLogger().indent();
        }
        ~LogSection()
        {
            Logger::getLogger().unindent();
            Logger::getLogger().print<level>(std::string("}"));
        }
    };

    class LoggerLifetimeTracker
    {
    public:
        LoggerLifetimeTracker(const std::string& _name) : name(_name)
        {
            addref();
        }
        LoggerLifetimeTracker(const LoggerLifetimeTracker& rhs) : name(rhs.name)
        {
            addref();
        }
        ~LoggerLifetimeTracker()
        {
            int prev_val = object_count.fetch_add(-1);
            LOGFINE(LT("Destructing "), this->name, LT(" (object count now "), prev_val - 1, LT(")"));
            if (prev_val == 1)
            {
                LOGFINE(LT("Losing backend for logger"));
                Logger::getLogger().releaseBackend();
                logger_backend->join();
                delete logger_backend;
                logger_backend = 0;
            }
        }

    private:
        void addref()
        {
            int prev_val = object_count.fetch_add(1);
            LOGFINE(LT("Constructing "), this->name, LT(" (object count now "), prev_val + 1, LT(")"));
            if (prev_val == 0)
            {
                LOGFINE(LT("Creating new backend for logger"));
                logger_backend = new std::thread{ Logger::log_spooler, &(Logger::getLogger()) };
            }
        }
        static std::atomic<int> object_count;
        static std::thread *logger_backend;
        std::string name;
    };
}

#endif //_MALMO_LOGGER_H_
