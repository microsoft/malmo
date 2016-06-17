// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// To compile:  javac -cp MalmoJavaJar.jar JavaExamples_run_mission.java
// To run:      java -cp MalmoJavaJar.jar:. JavaExamples_run_mission  (on Linux)
//              java -cp MalmoJavaJar.jar;. JavaExamples_run_mission  (on Windows)

// To run from the jar file without compiling:   java -cp MalmoJavaJar.jar:JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission (on Linux)
//                                               java -cp MalmoJavaJar.jar;JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission (on Windows)

public class JavaExamples_run_mission
{
    static
    {
        System.loadLibrary("MalmoJava"); // attempts to load MalmoJava.dll (on Windows) or libMalmoJava.so (on Linux)
    }

    public static void main(String argv[])
    {
        AgentHost agent_host = new AgentHost();
        try
        {
            StringVector args = new StringVector();
            args.add("JavaExamples_run_mission");
            for( String arg : argv )
                args.add( arg );
            agent_host.parse( args );
        }
        catch( Exception e )
        {
            System.out.println( "ERROR: " + e );
            System.out.println( agent_host.getUsage() );
            System.exit(1);
        }
        if( agent_host.receivedArgument("help") )
        {
            System.out.println( agent_host.getUsage() );
            System.exit(0);
        }

        MissionSpec my_mission = new MissionSpec();
        my_mission.timeLimitInSeconds(10);
        my_mission.requestVideo( 320, 240 );
        my_mission.rewardForReachingPosition(19,0,19,100.0f,1.1f);

        MissionRecordSpec my_mission_record = new MissionRecordSpec("./saved_data.tgz");
        my_mission_record.recordCommands();
        my_mission_record.recordMP4(20, 400000);
        my_mission_record.recordRewards();
        my_mission_record.recordObservations();

        try {
            agent_host.startMission( my_mission, my_mission_record );
        }
        catch (Exception e) {
            System.out.println( "Error starting mission: " + e );
            System.exit(1);
        }

        WorldState world_state;

        System.out.print( "Waiting for the mission to start" );
        do {
            System.out.print( "." );
            try {
                Thread.sleep(100);
            } catch(InterruptedException ex) {
                System.out.println( "User interrupted while waiting for mission to start." );
                return;
            }
            world_state = agent_host.getWorldState();
            for( int i = 0; i < world_state.getErrors().size(); i++ )
                System.out.println( "Error: " + world_state.getErrors().get(i).getText() );
        } while( !world_state.getIsMissionRunning() );
        System.out.println( "" );

        // main loop:
        do {
            agent_host.sendCommand( "move 1" );
            agent_host.sendCommand( "turn " + Math.random() );
            try {
                Thread.sleep(500);
            } catch(InterruptedException ex) {
                System.out.println( "User interrupted while mission was running." );
                return;
            }
            world_state = agent_host.getWorldState();
            System.out.print( "video,observations,rewards received: " );
            System.out.print( world_state.getNumberOfVideoFramesSinceLastState() + "," );
            System.out.print( world_state.getNumberOfObservationsSinceLastState() + "," );
            System.out.println( world_state.getNumberOfRewardsSinceLastState() );
            for( int i = 0; i < world_state.getRewards().size(); i++ ) {
                TimestampedFloat reward = world_state.getRewards().get(i);
                System.out.println( "Summed reward: " + reward.getValue() );
            }
            for( int i = 0; i < world_state.getErrors().size(); i++ ) {
                TimestampedString error = world_state.getErrors().get(i);
                System.out.println( "Error: " + error.getText() );
            }
        } while( world_state.getIsMissionRunning() );

        System.out.println( "Mission has stopped." );
    }
}
