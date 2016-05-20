// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _TIMESTAMPEDUNSIGNEDCHARVECTOR_H_
#define _TIMESTAMPEDUNSIGNEDCHARVECTOR_H_

// Boost:
#include <boost/date_time/posix_time/posix_time_types.hpp>

// STL:
#include <vector>

namespace malmo
{
    //! An array of unsigned chars with an attached timestamp saying when it was collected.
    struct TimestampedUnsignedCharVector
    {
        //! The timestamp.
        boost::posix_time::ptime timestamp;
        
        //! The array of unsigned char values.
        std::vector<unsigned char> data;

        TimestampedUnsignedCharVector(boost::posix_time::ptime timestamp, std::vector<unsigned char> data)
            : timestamp(timestamp)
            , data(data)
        {
        }

        TimestampedUnsignedCharVector()
        {
        }
    };
}

#endif
