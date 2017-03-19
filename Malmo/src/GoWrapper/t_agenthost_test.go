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

import "testing"

func Test_agenthost01(tst *testing.T) {

	agent_host := NewAgentHost()
	defer agent_host.Free()

	args := []string{"filename", "--run", "3", "--remote"} // we expect this to give an error

	err := agent_host.Parse(args)
	if err == nil {
		tst.Errorf("this test expects an error from Parse\n")
	}
}

func Test_agenthost02(tst *testing.T) {

	agent_host := NewAgentHost()
	defer agent_host.Free()

	err := agent_host.Parse([]string{"filename", "--help"})
	if err != nil {
		tst.Errorf("Parse failed:\n%v\n", err)
		return
	}

	response := agent_host.ReceivedArgument("help")
	if !response {
		tst.Errorf("ReceivedArgument failed: '--help' argument should be in there")
	}
}

func Test_agenthost03(tst *testing.T) {

	agent_host := NewAgentHost()
	defer agent_host.Free()

	usage := agent_host.GetUsage()
	if len(usage) == 0 {
		tst.Errorf("GetUsage failed")
	}
}

func Test_agenthost04(tst *testing.T) {

	agent_host := NewAgentHost()
	defer agent_host.Free()

	/*
		agent_host.SetVideoPolicy(LATEST_FRAME_ONLY)
		agent_host.SetRewardsPolicy(SUM_REWARDS)
		agent_host.SetObservationsPolicy(LATEST_OBSERVATION_ONLY)
	*/
}
