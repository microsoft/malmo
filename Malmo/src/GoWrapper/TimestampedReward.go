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

package malmo

/*
#include "x_timestamp.h"
*/
import "C"

import (
	"time"
	"unsafe"
)

const (
	REWARDS_MAX_NUMBER_DIMENSIONS = 4 // '4' allows for, e.g., 1,2,3 as 3D indices or 0,1,2
)

// TimestampedReward implements an array of float64 storing a value on each dimension, with an attached timestamp saying when it was collected.
type TimestampedReward struct {
	Timestamp time.Time // The timestamp
	values    []float64 // The rewards. size == ndim
}

// HasValueOnDimension returns whether a reward value is stored on the specified dimension.
func (o TimestampedReward) HasValueOnDimension(dimension int) bool {
	return dimension <= len(o.values)
}

// GetValueOnDimension returns the reward value stored on the specified dimension.
func (o TimestampedReward) GetValueOnDimension(dimension int) float64 {
	return o.values[dimension-1]
}

// GetValue returns the reward value stored on dimension zero. By default the reward producers store their output here.
func (o TimestampedReward) GetValue() float64 {
	return o.values[0]
}

// newTimestampedRewardFromCpp creates new TimestampedReward from C++ data
func newTimestampedRewardFromCpp(ts *C.timestamp_t, cndim C.int, ptValues *C.double) (o *TimestampedReward) {
	o = new(TimestampedReward)
	o.Timestamp = time.Date(
		int(ts.year),
		time.Month(ts.month),
		int(ts.day),
		int(ts.hours),
		int(ts.minutes),
		int(ts.seconds),
		int(ts.nanoseconds),
		time.Now().Location(),
	)

	ndim := int(cndim)

	values := (*[REWARDS_MAX_NUMBER_DIMENSIONS]C.double)(unsafe.Pointer(ptValues))
	if ndim > REWARDS_MAX_NUMBER_DIMENSIONS {
		ndim = REWARDS_MAX_NUMBER_DIMENSIONS
	}

	o.values = make([]float64, ndim)
	for i := 0; i < ndim; i++ {
		o.values[i] = float64(values[i])
	}
	return
}
