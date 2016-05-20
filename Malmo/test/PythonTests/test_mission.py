# --------------------------------------------------------------------------------------------------------------------
# Copyright (C) Microsoft Corporation.  All rights reserved.
# --------------------------------------------------------------------------------------------------------------------

import MalmoPython

my_mission = MalmoPython.MissionSpec()
my_mission.timeLimitInSeconds( 10 )
my_mission.drawBlock( 19, 0, 19, "redstone_block" )
my_mission.createDefaultTerrain()
my_mission.setTimeOfDay(6000,False)
my_mission.drawCuboid(50,0,50,100,10,100,"redstone_block")
my_mission.drawItem(3,0,2,"diamond_pickaxe")
my_mission.drawSphere(50,10,50,10,"ice")
my_mission.drawLine(50,20,50,100,20,100,"redstone_block")
my_mission.startAt( 2, 0, 2 )
my_mission.endAt( 19, 0, 19 )
my_mission.requestVideo( 320, 240 )
my_mission.setModeToCreative()
my_mission.rewardForReachingPosition(19,0,19,100,1.1)
my_mission.observeRecentCommands()
my_mission.observeHotBar()
my_mission.observeFullInventory()
my_mission.observeGrid(-2,0,-2,2,1,2,"Cells")
my_mission.observeDistance(19,0,19,"Goal")
my_mission.allowAllDiscreteMovementCommands()

pretty_print = False
xml = my_mission.getAsXML( pretty_print )

# check that we can validate the generated XML:
validate = True
my_mission2 = MalmoPython.MissionSpec( xml, validate )
    
# check that we get the same XML if we go round again
xml2 = my_mission2.getAsXML( pretty_print )
if not xml2 == xml:
    print 'Mismatch between first generation XML and the second:\n', xml, '\n\n', xml2
    exit(1)
