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

// Malmo:
using System;
using System.Collections.Generic;
using Microsoft.Research.Malmo;

public class Program
{
    public static void Main(string[] args)
    {
        MissionSpec my_mission = new MissionSpec();
        my_mission.setSummary("example mission");        
        my_mission.timeLimitInSeconds( 10 );
        my_mission.drawBlock( 19, 0, 19, "redstone_block" );
        my_mission.createDefaultTerrain();
        my_mission.setTimeOfDay(6000,false);
        my_mission.drawCuboid(50,0,50,100,10,100,"redstone_block");
        my_mission.drawItem(3,0,2,"diamond_pickaxe");
        my_mission.drawSphere(50,10,50,10,"ice");
        my_mission.drawLine(50,20,50,100,20,100,"redstone_block");
        my_mission.startAt( 2.5f, 0.0f, 2.5f );
        my_mission.endAt( 19.5f, 0.0f, 19.5f, 1.0f );
        my_mission.requestVideo( 320, 240 );
        my_mission.setModeToCreative();
        my_mission.rewardForReachingPosition(19.5f,0.0f,19.5f,100.0f,1.1f);
        my_mission.observeRecentCommands();
        my_mission.observeHotBar();
        my_mission.observeFullInventory();
        my_mission.observeGrid(-2,0,-2,2,1,2,"Cells");
        my_mission.observeDistance(19.5f,0.0f,19.5f,"Goal");
        my_mission.removeAllCommandHandlers();
        my_mission.allowContinuousMovementCommand("move");
        my_mission.allowContinuousMovementCommand("strafe");
        my_mission.allowDiscreteMovementCommand("movenorth");
        my_mission.allowInventoryCommand("swapInventoryItems");

        if( my_mission.getSummary() != "example mission" ) {
            Console.WriteLine("Unexpected summary");
            Environment.Exit(1);
        }

        string[] expected_command_handlers = { "ContinuousMovement", "DiscreteMovement", "Inventory" };
        string[] actual_command_handlers = new List<string>(my_mission.getListOfCommandHandlers(0)).ToArray();
        if( actual_command_handlers.Length != expected_command_handlers.Length ) {
            Console.WriteLine("Number of command handlers mismatch");
            Environment.Exit(1);
        }
        for( int i = 0; i < actual_command_handlers.Length; i++ ) {
            if( !actual_command_handlers[i].Equals( expected_command_handlers[i] ) ) {
                Console.WriteLine("Unexpected command handler: {0}",actual_command_handlers[i] );
                Environment.Exit(1);
            }
        }

        string[] expected_continuous_commands = { "move", "strafe" };
        string[] actual_continuous_commands = new List<string>(my_mission.getAllowedCommands(0,"ContinuousMovement")).ToArray();
        if( actual_continuous_commands.Length != expected_continuous_commands.Length ) {
            Console.WriteLine("Number of continuous commands mismatch");
            Environment.Exit(1);
        }
        for( int i = 0; i < actual_continuous_commands.Length; i++ ) {
            if( !actual_continuous_commands[i].Equals( expected_continuous_commands[i] ) ) {
                Console.WriteLine("Unexpected continuous command: {0}",actual_continuous_commands[i] );
                Environment.Exit(1);
            }
        }

        string[] expected_discrete_commands = { "movenorth" };
        string[] actual_discrete_commands = new List<string>(my_mission.getAllowedCommands(0,"DiscreteMovement")).ToArray();
        if( actual_discrete_commands.Length != expected_discrete_commands.Length ) {
            Console.WriteLine("Number of discrete commands mismatch");
            Environment.Exit(1);
        }
        for( int i = 0; i < actual_discrete_commands.Length; i++ ) {
            if( !actual_discrete_commands[i].Equals( expected_discrete_commands[i] ) ) {
                Console.WriteLine("Unexpected discrete command: {0}",actual_discrete_commands[i] );
                Environment.Exit(1);
            }
        }

        string[] expected_inventory_commands = { "swapInventoryItems" };
        string[] actual_inventory_commands = new List<string>(my_mission.getAllowedCommands(0,"Inventory")).ToArray();
        if( actual_inventory_commands.Length != expected_inventory_commands.Length ) {
            Console.WriteLine("Number of commands mismatch");
            Environment.Exit(1);
        }
        for( int i = 0; i < actual_inventory_commands.Length; i++ ) {
            if( !actual_inventory_commands[i].Equals( expected_inventory_commands[i] ) ) {
                Console.WriteLine("Unexpected command: {0}",actual_inventory_commands[i] );
                Environment.Exit(1);
            }
        }

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
        
        // check that known-good XML validates
        const string xml3 = @"<?xml version=""1.0"" encoding=""UTF-8"" ?><Mission xmlns=""http://ProjectMalmo.microsoft.com"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"">
            <About><Summary>Run the maze!</Summary></About>
            <ServerSection><ServerInitialConditions><AllowSpawning>true</AllowSpawning><Time><StartTime>1000</StartTime><AllowPassageOfTime>true</AllowPassageOfTime></Time><Weather>clear</Weather></ServerInitialConditions>
            <ServerHandlers>
            <FlatWorldGenerator generatorString=""3;7,220*1,5*3,2;3;,biome_1"" />
            <ServerQuitFromTimeUp timeLimitMs=""20000"" />
            <ServerQuitWhenAnyAgentFinishes />
            </ServerHandlers></ServerSection>
            <AgentSection><Name>Jason Bourne</Name><AgentStart><Placement x=""-204"" y=""81"" z=""217""/></AgentStart><AgentHandlers>
            <VideoProducer want_depth=""true""><Width>320</Width><Height>240</Height></VideoProducer>
            <RewardForReachingPosition><Marker reward=""100"" tolerance=""1.1"" x=""-104"" y=""81"" z=""217""/></RewardForReachingPosition>
            <ContinuousMovementCommands><ModifierList type=""deny-list""><command>attack</command><command>crouch</command></ModifierList></ContinuousMovementCommands>
            <AgentQuitFromReachingPosition><Marker x=""-104"" y=""81"" z=""217""/></AgentQuitFromReachingPosition>
            </AgentHandlers></AgentSection></Mission>";
        try 
        {
            const bool validate = true;
            MissionSpec my_mission3 = new MissionSpec( xml3, validate );
            
            if( my_mission3.getSummary() != "Run the maze!" ) {
                Console.WriteLine("Unexpected summary");
                Environment.Exit(1);
            }

            string[] expected_command_handlers2 = { "ContinuousMovement" };
            string[] actual_command_handlers2 = new List<string>(my_mission3.getListOfCommandHandlers(0)).ToArray();
            if( actual_command_handlers2.Length != expected_command_handlers2.Length ) {
                Console.WriteLine("Number of command handlers mismatch");
                Environment.Exit(1);
            }
            for( int i = 0; i < actual_command_handlers2.Length; i++ ) {
                if( !actual_command_handlers2[i].Equals( expected_command_handlers2[i] ) ) {
                    Console.WriteLine("Unexpected command handler: {0}",actual_command_handlers2[i] );
                    Environment.Exit(1);
                }
            }

            string[] expected_continuous_commands2 = { "jump", "move", "pitch", "strafe", "turn", "use" };
            string[] actual_continuous_commands2 = new List<string>(my_mission3.getAllowedCommands(0,"ContinuousMovement")).ToArray();
            if( actual_continuous_commands2.Length != expected_continuous_commands2.Length ) {
                Console.WriteLine("Number of continuous commands mismatch");
                Environment.Exit(1);
            }
            for( int i = 0; i < actual_continuous_commands2.Length; i++ ) {
                if( !actual_continuous_commands2[i].Equals( expected_continuous_commands2[i] ) ) {
                    Console.WriteLine("Unexpected continuous command: {0}",actual_continuous_commands2[i] );
                    Environment.Exit(1);
                }
            }
        } 
        catch( Exception e )
        {
            Console.WriteLine("Error validating known-good XML: {0}", e );
            Environment.Exit(1);
        }
    }
}
