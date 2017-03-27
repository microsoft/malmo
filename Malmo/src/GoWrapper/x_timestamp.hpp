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

#ifndef TIMESTAMP_HPP
#define TIMESTAMP_HPP

// Boost:
#include <boost/date_time/posix_time/posix_time_types.hpp>

// Local:
#include "x_definitions.h"

// converts ptime to TimestampData
static inline timestamp_t timestamp_from_ptime(boost::posix_time::ptime const& pt) {

    timestamp_t ts;

    boost::gregorian::date date = pt.date();
    boost::posix_time::time_duration td = pt.time_of_day();

    ts.year    = (int)date.year();
    ts.month   = (int)date.month();
    ts.day     = (int)date.day();
    ts.hours   = td.hours();
    ts.minutes = td.minutes();
    ts.seconds = td.seconds();

    static std::int64_t resolution = boost::posix_time::time_duration::ticks_per_second();
    std::int64_t fracsecs = td.fractional_seconds();
    std::int64_t usecs = (resolution > 1000000) ? fracsecs / (resolution / 1000000) : fracsecs * (1000000 / resolution);

    ts.nanoseconds = td.fractional_seconds() * 1000;

    return ts;
}

#endif // TIMESTAMP_HPP
