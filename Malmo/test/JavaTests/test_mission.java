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

public class test_mission
{
    static 
    {
        System.loadLibrary("MalmoJava");
    }  

    public static void main(String argv[]) 
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
        my_mission.startAt( 2.5f, 0.5f, 2.5f );
        my_mission.endAt( 19.5f, 0.5f, 19.5f, 1.0f );
        my_mission.requestVideo( 320, 240 );
        my_mission.setModeToCreative();
        my_mission.rewardForReachingPosition(19.5f,0.5f,19.5f,100.0f,1.1f);
        my_mission.observeRecentCommands();
        my_mission.observeHotBar();
        my_mission.observeFullInventory();
        my_mission.observeGrid(-2,0,-2,2,1,2, "Cells");
        my_mission.observeDistance(19.5f,0.5f,19.5f,"Goal");
        my_mission.allowAllDiscreteMovementCommands();

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
        String xml3 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><Mission xmlns=\"http://ProjectMalmo.microsoft.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://ProjectMalmo.microsoft.com Mission.xsd\">"
            +"<About><Summary>Run the maze!</Summary></About>"
            +"<ServerSection><ServerInitialConditions><AllowSpawning>true</AllowSpawning><Time><StartTime>1000</StartTime><AllowPassageOfTime>true</AllowPassageOfTime></Time><Weather>clear</Weather></ServerInitialConditions>"
            +"<ServerHandlers>"
            +"<FlatWorldGenerator generatorString=\"3;7,220*1,5*3,2;3;,biome_1\" />"
            +"<ServerQuitFromTimeUp timeLimitMs=\"20000\" />"
            +"<ServerQuitWhenAnyAgentFinishes />"
            +"</ServerHandlers></ServerSection>"
            +"<AgentSection><Name>Jason Bourne</Name><AgentStart><Placement x=\"-204\" y=\"81\" z=\"217\"/></AgentStart><AgentHandlers>"
            +"<VideoProducer want_depth=\"true\"><Width>320</Width><Height>240</Height></VideoProducer>"
            +"<ObservationFromMazeOptimalPath />"
            +"<RewardForReachingPosition><Marker reward=\"100\" tolerance=\"1.1\" x=\"-104\" y=\"81\" z=\"217\"/></RewardForReachingPosition>"
            +"<ContinuousMovementCommands />"
            +"<AgentQuitFromReachingPosition><Marker x=\"-104\" y=\"81\" z=\"217\"/></AgentQuitFromReachingPosition>"
            +"</AgentHandlers></AgentSection></Mission>";
        try 
        {
            boolean validate = true;
            MissionSpec my_mission3 = new MissionSpec( xml3, validate );
        } 
        catch( Exception e )
        {
            System.out.println( "Error validating known-good XML." );
            System.exit(1);
        }
    }
}
