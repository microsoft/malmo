// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _TIMESTAMPEDSTRINGMESSAGE_H_
#define _TIMESTAMPEDSTRINGMESSAGE_H_

// Local:
#include "TimestampedUnsignedCharVector.h"

// Boost:
#include <boost/date_time/posix_time/posix_time_types.hpp>

// STL:
#include <string>

namespace malmo
{
    //! A string with an attached timestamp saying when it was collected.
    struct TimestampedString
    {
        //! The timestamp.
        boost::posix_time::ptime timestamp;
        
        //! The string.
        std::string text;

        TimestampedString(const TimestampedUnsignedCharVector& message);
        TimestampedString(const boost::posix_time::ptime& timestamp, const std::string& text);
        
        bool operator==(const TimestampedString& other) const;
    };
}

#endif
