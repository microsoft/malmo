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
#include "TimestampedString.h"
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/date_time/posix_time/posix_time_io.hpp>

namespace malmo
{
    TimestampedString::TimestampedString(const TimestampedUnsignedCharVector& message)
        : timestamp(message.timestamp)
        , text(std::string(message.data.begin(), message.data.end()))
    {
    }

    TimestampedString::TimestampedString(const boost::posix_time::ptime& timestamp, const std::string& message)
        : timestamp(timestamp)
        , text(message)
    {
    }
    
    bool TimestampedString::operator==(const TimestampedString& other) const
    {
        return this->text == other.text && this->timestamp == other.timestamp;
    }

    std::ostream& operator<<(std::ostream& os, const TimestampedString& tss)
    {
        os << "TimestampedString: " << to_simple_string(tss.timestamp) << ", " << tss.text;
        return os;
    }
}
