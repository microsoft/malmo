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

const (
	DefaultClientMissionControlPort = 10000 // The default client mission control port
)

// ClientInfo contains information about a simulation client's address and port
type ClientInfo struct {
	IpAddress string // The IP address of the client.
	Port      int    // The port of the client.
}

// NewClientInfo ceates a ClientInfo
func NewClientInfo() *ClientInfo {
	return &ClientInfo{}
}

// NewClientInfoAddress Constructs a ClientInfo at the specified address listening on the default port.
// ip_address -- The IP address of the client
func NewClientInfoAddress(ip_address string) *ClientInfo {
	return &ClientInfo{IpAddress: ip_address, Port: DefaultClientMissionControlPort}
}

// NewClientInfoAddressAndPort Constructs a ClientInfo at the specified address listening on the specified port.
// ip_address -- The IP address of the client.
// port -- The number of the client port.
func NewClientInfoAddressAndPort(ip_address string, port int) *ClientInfo {
	return &ClientInfo{IpAddress: ip_address, Port: port}
}
