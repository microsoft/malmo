package malmo

import "testing"

func Test_agenthost01(tst *testing.T) {
	agenthost := NewAgentHost()
	defer agenthost.Free()
}
