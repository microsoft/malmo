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
#include "x_auxiliary.h"
*/
import "C"

import "unsafe"

// makeArrayChar allocates C array of chars
// Note: make sure to call free() to deallocate memory
func makeArrayChar(array []string) (csize C.int, carray **C.char, free func()) {

	size := len(array)
	csize = C.int(size)

	carray = C.make_argv(csize)
	pointers := make([]*C.char, size)
	for i, s := range array {
		pointers[i] = C.CString(s)
		C.set_arg(carray, C.int(i), pointers[i])
	}

	free = func() {
		for i := 0; i < size; i++ {
			C.free(unsafe.Pointer(pointers[i]))
		}
		C.free(unsafe.Pointer(carray))
	}
	return
}
