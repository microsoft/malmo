-- --------------------------------------------------------------------------------------------------
--  Copyright (c) 2016 Microsoft Corporation
--  
--  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
--  associated documentation files (the "Software"), to deal in the Software without restriction,
--  including without limitation the rights to use, copy, modify, merge, publish, distribute,
--  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
--  furnished to do so, subject to the following conditions:
--  
--  The above copyright notice and this permission notice shall be included in all copies or
--  substantial portions of the Software.
--  
--  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
--  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
--  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
--  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
--  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-- --------------------------------------------------------------------------------------------------

require 'libMalmoLua'

local my_mission = MissionSpec()
my_mission:timeLimitInSeconds( 10 )
my_mission:drawBlock( 19, 0, 19, "redstone_block" )
my_mission:createDefaultTerrain()
my_mission:setTimeOfDay(6000,false)
my_mission:drawCuboid(50,0,50,100,10,100,"redstone_block")
my_mission:drawItem(3,0,2,"diamond_pickaxe")
my_mission:drawSphere(50,10,50,10,"ice")
my_mission:drawLine(50,20,50,100,20,100,"redstone_block")
my_mission:startAt( 2, 0, 2 )
my_mission:endAt( 19, 0, 19 )
my_mission:requestVideo( 320, 240 )
my_mission:setModeToCreative()
my_mission:rewardForReachingPosition(19,0,19,100,1.1)
my_mission:observeRecentCommands()
my_mission:observeHotBar()
my_mission:observeFullInventory()
my_mission:observeGrid(-2,0,-2,2,1,2,"Cells")
my_mission:observeDistance(19,0,19,"Goal")
my_mission:allowAllDiscreteMovementCommands()

local pretty_print = false
local xml = my_mission:getAsXML( pretty_print )

-- check that we can validate the generated XML:
local validate = true
local my_mission2 = MissionSpec( xml, validate )
    
-- check that we get the same XML if we go round again
local xml2 = my_mission2:getAsXML( pretty_print )
assert( xml2 == xml, 'Mismatch between first generation XML and the second:\n'..xml..'\n\n'..xml2 )
