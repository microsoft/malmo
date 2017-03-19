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

func Test_mission01(tst *testing.T) {

	my_mission := NewMissionSpec()
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
}
