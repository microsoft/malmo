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
#include "x_definitions.h"
*/
import "C"

import (
	"errors"
	"unsafe"
)

// ClientPool defines a pool of expected network locations of Mod clients.
type ClientPool struct {
	Addresses []string // The IP addresses of the clients.
	Ports     []int    // The ports of the clients.
}

// Add adds a client to the pool.
func (o *ClientPool) Add(address string, port int) {
	o.Addresses = append(o.Addresses, address)
	o.Ports = append(o.Ports, port)
}

// toC converts Go data to C data
func (o ClientPool) toC() (cp C.client_pool_t, err error, free func()) {
	if len(o.Ports) != len(o.Addresses) {
		err = errors.New("The size of Ports and Addresses in ClientPool must be the same")
		return
	}
	cp.size, cp.addresses, free = makeArrayChar(o.Addresses)
	cp.ports = (*C.long)(unsafe.Pointer(&o.Ports[0]))
	return
}
