// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

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
