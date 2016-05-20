// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "TimestampedFloat.h"

namespace malmo
{
    bool TimestampedFloat::operator==(const TimestampedFloat& other) const
    {
        return this->value == other.value && this->timestamp == other.timestamp;
    }
}
