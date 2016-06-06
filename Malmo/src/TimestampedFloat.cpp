// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "TimestampedFloat.h"
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/date_time/posix_time/posix_time_io.hpp>

namespace malmo
{
    bool TimestampedFloat::operator==(const TimestampedFloat& other) const
    {
        return this->value == other.value && this->timestamp == other.timestamp;
    }

    std::ostream& operator<<(std::ostream& os, const TimestampedFloat& tsf)
    {
	os << "TimestampedFloat: " << to_simple_string(tsf.timestamp) << ", " << tsf.value;
	return os;
    }
}
