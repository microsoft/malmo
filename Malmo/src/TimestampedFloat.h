// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _TIMESTAMPEDFLOAT_H_
#define _TIMESTAMPEDFLOAT_H_

// Boost:
#include <boost/date_time/posix_time/posix_time_types.hpp>

namespace malmo
{
    //! A floating-point value with an attached timestamp saying when it was collected.
    struct TimestampedFloat 
    {
        //! The timestamp.
        boost::posix_time::ptime timestamp;
        
        //! The value.
        float value;
        
        bool operator==(const TimestampedFloat&) const;
        friend std::ostream& operator<<(std::ostream& os, const TimestampedFloat& tsf);
    };
}

#endif
