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

import com.microsoft.msr.malmo.*;

public class test_mission
{
    static 
    {
        System.loadLibrary("MalmoJava");
    }  

    public static void main(String argv[]) 
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
        my_mission.observeGrid(-2,0,-2,2,1,2, "Cells");
        my_mission.observeDistance(19.5f,0.0f,19.5f,"Goal");
        my_mission.removeAllCommandHandlers();
        my_mission.allowContinuousMovementCommand("move");
        my_mission.allowContinuousMovementCommand("strafe");
        my_mission.allowDiscreteMovementCommand("movenorth");
        my_mission.allowInventoryCommand("swapInventoryItems");

        if( !my_mission.getSummary().equals("example mission") ) {
            System.out.println("Unexpected summary");
            System.exit(1);
        }

        String[] expected_command_handlers = { "ContinuousMovement", "DiscreteMovement", "Inventory" };
        StringVector actual_command_handlers = my_mission.getListOfCommandHandlers(0);
        if( actual_command_handlers.size() != expected_command_handlers.length ) {
            System.out.println("Number of command handlers mismatch");
            System.exit(1);
        }
        for( int i = 0; i < actual_command_handlers.size(); i++ ) {
            if( !actual_command_handlers.get(i).equals( expected_command_handlers[i] ) ) {
                System.out.println("Unexpected command handler: " + actual_command_handlers.get(i) );
                System.exit(1);
            }
        }

        String[] expected_continuous_commands = { "move", "strafe" };
        StringVector actual_continuous_commands = my_mission.getAllowedCommands(0,"ContinuousMovement");
        if( actual_continuous_commands.size() != expected_continuous_commands.length ) {
            System.out.println("Number of continuous commands mismatch");
            System.exit(1);
        }
        for( int i = 0; i < actual_continuous_commands.size(); i++ ) {
            if( !actual_continuous_commands.get(i).equals( expected_continuous_commands[i] ) ) {
                System.out.println("Unexpected continuous command: " + actual_continuous_commands.get(i) );
                System.exit(1);
            }
        }

        String[] expected_discrete_commands = { "movenorth" };
        StringVector actual_discrete_commands = my_mission.getAllowedCommands(0,"DiscreteMovement");
        if( actual_discrete_commands.size() != expected_discrete_commands.length ) {
            System.out.println("Number of discrete commands mismatch");
            System.exit(1);
        }
        for( int i = 0; i < actual_discrete_commands.size(); i++ ) {
            if( !actual_discrete_commands.get(i).equals( expected_discrete_commands[i] ) ) {
               System.out.println("Unexpected discrete command: " + actual_discrete_commands.get(i) );
                System.exit(1);
            }
        }

        String[] expected_inventory_commands = { "swapInventoryItems" };
        StringVector actual_inventory_commands = my_mission.getAllowedCommands(0,"Inventory");
        if( actual_inventory_commands.size() != expected_inventory_commands.length ) {
            System.out.println("Number of commands mismatch");
            System.exit(1);
        }
        for( int i = 0; i < actual_inventory_commands.size(); i++ ) {
            if( !actual_inventory_commands.get(i).equals( expected_inventory_commands[i] ) ) {
                System.out.println("Unexpected command: " + actual_inventory_commands.get(i) );
                System.exit(1);
            }
        }

        // check that the XML we produce validates
        boolean pretty_print = false;
        String xml = my_mission.getAsXML( pretty_print );
        try
        {
            boolean validate = true;
            MissionSpec my_mission2 = new MissionSpec( xml, validate );

            // check that we get the same XML if we go round again
            String xml2 = my_mission2.getAsXML( pretty_print );
            if( !xml2.equals( xml ) )
            {
                System.out.println( "Mismatch between first generation XML and the second." );
                System.exit(1);
            }
        } 
        catch( Exception e )
        {
            System.out.println( "Error validating the XML we generated." );
            System.exit(1);
        }
        
        // check that known-good XML validates
        String xml3 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><Mission xmlns=\"http://ProjectMalmo.microsoft.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            +"<About><Summary>Run the maze!</Summary></About>"
            +"<ServerSection><ServerInitialConditions><AllowSpawning>true</AllowSpawning><Time><StartTime>1000</StartTime><AllowPassageOfTime>true</AllowPassageOfTime></Time><Weather>clear</Weather></ServerInitialConditions>"
            +"<ServerHandlers>"
            +"<FlatWorldGenerator generatorString=\"3;7,220*1,5*3,2;3;,biome_1\" />"
            +"<ServerQuitFromTimeUp timeLimitMs=\"20000\" />"
            +"<ServerQuitWhenAnyAgentFinishes />"
            +"</ServerHandlers></ServerSection>"
            +"<AgentSection><Name>Jason Bourne</Name><AgentStart><Placement x=\"-204\" y=\"81\" z=\"217\"/></AgentStart><AgentHandlers>"
            +"<VideoProducer want_depth=\"true\"><Width>320</Width><Height>240</Height></VideoProducer>"
            +"<RewardForReachingPosition><Marker reward=\"100\" tolerance=\"1.1\" x=\"-104\" y=\"81\" z=\"217\"/></RewardForReachingPosition>"
            +"<ContinuousMovementCommands><ModifierList type=\"deny-list\"><command>attack</command><command>crouch</command></ModifierList></ContinuousMovementCommands>"
            +"<AgentQuitFromReachingPosition><Marker x=\"-104\" y=\"81\" z=\"217\"/></AgentQuitFromReachingPosition>"
            +"</AgentHandlers></AgentSection></Mission>";
        try 
        {
            boolean validate = true;
            MissionSpec my_mission3 = new MissionSpec( xml3, validate );
            
            if( !my_mission3.getSummary().equals("Run the maze!") ) {
                System.out.println("Unexpected summary");
                System.exit(1);
            }

            String[] expected_command_handlers2 = { "ContinuousMovement" };
            StringVector actual_command_handlers2 = my_mission3.getListOfCommandHandlers(0);
            if( actual_command_handlers2.size() != expected_command_handlers2.length ) {
                System.out.println("Number of command handlers mismatch");
                System.exit(1);
            }
            for( int i = 0; i < actual_command_handlers2.size(); i++ ) {
                if( !actual_command_handlers2.get(i).equals( expected_command_handlers2[i] ) ) {
                    System.out.println("Unexpected command handler: " + actual_command_handlers2.get(i) );
                    System.exit(1);
                }
            }

            String[] expected_continuous_commands2 = { "jump", "move", "pitch", "strafe", "turn", "use" };
            StringVector actual_continuous_commands2 = my_mission3.getAllowedCommands(0,"ContinuousMovement");
            if( actual_continuous_commands2.size() != expected_continuous_commands2.length ) {
                System.out.println("Number of continuous commands mismatch");
                System.exit(1);
            }
            for( int i = 0; i < actual_continuous_commands2.size(); i++ ) {
                if( !actual_continuous_commands2.get(i).equals( expected_continuous_commands2[i] ) ) {
                    System.out.println("Unexpected continuous command: " + actual_continuous_commands2.get(i) );
                    System.exit(1);
                }
            }
        } 
        catch( Exception e )
        {
            System.out.println( "Error validating known-good XML." );
            System.exit(1);
        }
    }
}
