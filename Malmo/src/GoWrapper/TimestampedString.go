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

import "time"

// TimestampedString implements a string with an attached timestamp saying when it was collected.
type TimestampedString struct {
	Timestamp time.Time // The timestamp
	Text      string    // The string
}

// newTimestampedStringFromCpp creates new TimestampedString from C++ data
func newTimestampedStringFromCpp(ts *C.timestamp_t, text *C.char, text_size C.int) (o *TimestampedString) {
	o = new(TimestampedString)
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
	o.Text = C.GoStringN(text, text_size)
	return
}
