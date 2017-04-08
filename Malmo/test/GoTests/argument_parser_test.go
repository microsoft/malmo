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

package main

import (
	"malmo"
	"math"
	"testing"
)

func Test_argumentparser01(tst *testing.T) {

	parser := malmo.NewAgentHost()
	defer parser.Free()

	parser.AddOptionalIntArgument("runs", "how many runs to perform", 1)
	parser.AddOptionalFlag("verbose", "print lots of debugging information")
	parser.AddOptionalFloatArgument("q", "what is the q parameter", 0.01)
	parser.AddOptionalStringArgument("mission", "the mission filename", "")

	args1 := []string{"fileame", "--runs", "3", "--q", "0.2", "--mission", "test.xml", "--verbose"}

	err := parser.Parse(args1)
	if err != nil {
		tst.Errorf("cannot parse arguments:\n%v\n", err)
		return
	}

	if !parser.ReceivedArgument("runs") || parser.GetIntArgument("runs") != 3 {
		tst.Errorf("'runs' is incorrect")
		return
	}

	if !parser.ReceivedArgument("q") || math.Abs(0.2-parser.GetFloatArgument("q")) > 1e-06 {
		tst.Errorf("'q' is incorrect")
		return
	}

	if !parser.ReceivedArgument("mission") || parser.GetStringArgument("mission") != "test.xml" {
		tst.Errorf("'mission' is incorrect")
		return
	}

	if !parser.ReceivedArgument("verbose") {
		tst.Errorf("'verbose' is incorrect")
		return
	}

	if parser.ReceivedArgument("test.xml") {
		tst.Errorf("'test.xml' is incorrect")
		return
	}

	args2 := []string{"filename", "--runs"} // we expect this to give an error
	err = parser.Parse(args2)
	if err == nil {
		tst.Errorf("this test expects an error from Parse\n")
	}
}
