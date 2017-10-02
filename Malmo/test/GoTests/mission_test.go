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
	"testing"
)

func wrong_list(trial, correct []string) bool {
	if len(trial) != len(correct) {
		return true
	}
	for i, x := range correct {
		if trial[i] != x {
			return true
		}
	}
	return false
}

func Test_mission01(tst *testing.T) {

	my_mission := malmo.NewMissionSpec()
	defer my_mission.Free()

	my_mission.SetSummary("example mission")
	my_mission.TimeLimitInSeconds(10)
	my_mission.DrawBlock(19, 0, 19, "redstone_block")
	my_mission.CreateDefaultTerrain()
	my_mission.SetTimeOfDay(6000, false)
	my_mission.DrawCuboid(50, 0, 50, 100, 10, 100, "redstone_block")
	my_mission.DrawItem(3, 0, 2, "diamond_pickaxe")
	my_mission.DrawSphere(50, 10, 50, 10, "ice")
	my_mission.DrawLine(50, 20, 50, 100, 20, 100, "redstone_block")
	my_mission.StartAt(2, 0, 2)
	my_mission.EndAt(19.5, 0.0, 19.5, 1.0)
	my_mission.RequestVideo(320, 240)
	my_mission.SetModeToCreative()
	my_mission.RewardForReachingPosition(19.5, 0.0, 19.5, 100.0, 1.1)
	my_mission.ObserveRecentCommands()
	my_mission.ObserveHotBar()
	my_mission.ObserveFullInventory()
	my_mission.ObserveGrid(-2, 0, -2, 2, 1, 2, "Cells")
	my_mission.ObserveDistance(19.5, 0.0, 19.5, "Goal")
	my_mission.RemoveAllCommandHandlers()
	my_mission.AllowContinuousMovementCommand("move")
	my_mission.AllowContinuousMovementCommand("strafe")
	my_mission.AllowDiscreteMovementCommand("movenorth")
	my_mission.AllowInventoryCommand("swapInventoryItems")

	if my_mission.GetSummary() != "example mission" {
		tst.Errorf("Unexpected summary\n")
		return
	}

	handlers := my_mission.GetListOfCommandHandlers(0)
	if wrong_list(handlers, []string{"ContinuousMovement", "DiscreteMovement", "Inventory"}) {
		tst.Errorf("Unexpected command handlers.\n")
		return
	}

	commands := my_mission.GetAllowedCommands(0, "ContinuousMovement")
	if wrong_list(commands, []string{"move", "strafe"}) {
		tst.Errorf("Unexpected commands for ContinuousMovement.\n")
		return
	}

	commands = my_mission.GetAllowedCommands(0, "DiscreteMovement")
	if wrong_list(commands, []string{"movenorth"}) {
		tst.Errorf("Unexpected commands for DiscreteMovement.\n")
		return
	}

	commands = my_mission.GetAllowedCommands(0, "Inventory")
	if wrong_list(commands, []string{"swapInventoryItems"}) {
		tst.Errorf("Unexpected commands for Inventory.\n")
		return
	}

	// check that the XML we produce validates
	pretty_print := false
	xml := my_mission.GetAsXML(pretty_print)

	// second mission variable
	validate := true
	my_mission2 := malmo.NewMissionSpecXML(xml, validate)
	defer my_mission2.Free()

	// check that we get the same XML if we go round again
	xml2 := my_mission2.GetAsXML(pretty_print)
	if xml2 != xml {
		tst.Errorf("Mismatch between first generation XML and the second:\n\n%v\n%v\n", xml, xml2)
		return
	}

	// check that known-good XML validates
	xml3 := `<?xml version="1.0" encoding="UTF-8" ?><Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<About><Summary>Run the maze!</Summary></About>
<ServerSection><ServerInitialConditions><AllowSpawning>true</AllowSpawning><Time><StartTime>1000</StartTime><AllowPassageOfTime>true</AllowPassageOfTime></Time><Weather>clear</Weather></ServerInitialConditions>
<ServerHandlers>
<FlatWorldGenerator generatorString="3;7,220*1,5*3,2;3;,biome_1" />
<ServerQuitFromTimeUp timeLimitMs="20000" />
<ServerQuitWhenAnyAgentFinishes />
</ServerHandlers></ServerSection>
<AgentSection><Name>Jason Bourne</Name><AgentStart><Placement x="-204" y="81" z="217"/></AgentStart><AgentHandlers>
<VideoProducer want_depth="true"><Width>320</Width><Height>240</Height></VideoProducer>
<RewardForReachingPosition><Marker reward="100" tolerance="1.1" x="-104" y="81" z="217"/></RewardForReachingPosition>
<ContinuousMovementCommands><ModifierList type="deny-list"><command>attack</command><command>crouch</command></ModifierList></ContinuousMovementCommands>
<AgentQuitFromReachingPosition><Marker x="-104" y="81" z="217"/></AgentQuitFromReachingPosition>
</AgentHandlers></AgentSection></Mission>`

	my_mission3 := malmo.NewMissionSpecXML(xml3, validate)
	defer my_mission3.Free()

	if my_mission3.GetSummary() != "Run the maze!" {
		tst.Errorf("Unexpected summary\n")
		return
	}

	//const vector< string > expected_command_handlers = ;
	commands = my_mission3.GetListOfCommandHandlers(0)
	if wrong_list(commands, []string{"ContinuousMovement"}) {
		tst.Errorf("Unexpected command handlers in xml3.\n")
		return
	}

	commands = my_mission3.GetAllowedCommands(0, "ContinuousMovement")
	if wrong_list(commands, []string{"jump", "move", "pitch", "strafe", "turn", "use"}) {
		tst.Errorf("Unexpected commands for ContinuousMovement in xml3.\n")
		return
	}
}
