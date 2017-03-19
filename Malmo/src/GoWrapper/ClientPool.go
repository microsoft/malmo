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
#include "go_clientpool.h"
*/
import "C"

// ClientPool defines a pool of expected network locations of Mod clients.
type ClientPool struct {
	client_pool C.ptClientPool // pointer to C.ClientPool
}

// NewClientPool ceates a ClientPool
func NewClientPool() (o *ClientPool) {
	o = new(ClientPool)
	o.client_pool = C.new_client_pool()
	return
}

// Free deallocates ClientPool object
func (o *ClientPool) Free() {
	if o.client_pool != nil {
		C.free_client_pool(o.client_pool)
	}
}

// Add adds a client to the pool.
// client_info -- The client information.
func (o *ClientPool) Add(client_info *ClientInfo) {
	panic("TODO")
}