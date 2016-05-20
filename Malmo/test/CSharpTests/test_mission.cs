// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Malmo:
using System;
using Microsoft.Research.Malmo;

public class Program
{
    public static void Main(string[] args)
    {
        MissionSpec my_mission = new MissionSpec();
        my_mission.timeLimitInSeconds( 10 );
        my_mission.drawBlock( 19, 0, 19, "redstone_block" );
        my_mission.createDefaultTerrain();
        my_mission.setTimeOfDay(6000,false);
        my_mission.drawCuboid(50,0,50,100,10,100,"redstone_block");
        my_mission.drawItem(3,0,2,"diamond_pickaxe");
        my_mission.drawSphere(50,10,50,10,"ice");
        my_mission.drawLine(50,20,50,100,20,100,"redstone_block");
        my_mission.startAt( 2, 0, 2 );
        my_mission.endAt( 19, 0, 19 );
        my_mission.requestVideo( 320, 240 );
        my_mission.setModeToCreative();
        my_mission.rewardForReachingPosition(19,0,19,100.0f,1.1f);
        my_mission.observeRecentCommands();
        my_mission.observeHotBar();
        my_mission.observeFullInventory();
        my_mission.observeGrid(-2,0,-2,2,1,2,"Cells");
        my_mission.observeDistance(19,0,19,"Goal");
        my_mission.allowAllDiscreteMovementCommands();

        // check that the XML we produce validates
        bool pretty_print = false;
        string xml = my_mission.getAsXML( pretty_print );
        try
        {
            bool validate = true;
            MissionSpec my_mission2 = new MissionSpec( xml, validate );

            // check that we get the same XML if we go round again
            string xml2 = my_mission2.getAsXML( pretty_print );
            if( xml2 != xml )
            {
                Console.WriteLine("Mismatch between first generation XML and the second.");
                Environment.Exit(1);
            }
        } 
        catch( Exception e )
        {
            Console.WriteLine("Error validating the XML we generated: {0}", e);
            Environment.Exit(1);
        }
    }
}
