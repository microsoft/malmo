// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

using System;
using System.Threading;
using Microsoft.Research.Malmo;

class Program
{
    public static void Main()
    {
        AgentHost agentHost = new AgentHost();
        try
        {
            agentHost.parse( new StringVector( Environment.GetCommandLineArgs() ) );
        }
        catch( Exception ex )
        {
            Console.Error.WriteLine("ERROR: {0}", ex);
            Console.Error.WriteLine(agentHost.getUsage());
            Environment.Exit(1);
        }
        if( agentHost.receivedArgument("help") )
        {
            Console.Error.WriteLine(agentHost.getUsage());
            Environment.Exit(0);
        }

        MissionSpec mission = new MissionSpec();
        mission.timeLimitInSeconds(10);
        mission.requestVideo( 320, 240 );
        mission.rewardForReachingPosition(19,0,19,100.0f,1.1f);

        MissionRecordSpec missionRecord = new MissionRecordSpec("./saved_data.tgz");
        missionRecord.recordCommands();
        missionRecord.recordMP4(20, 400000);
        missionRecord.recordRewards();
        missionRecord.recordObservations();

        try
        {
            agentHost.startMission(mission, missionRecord);
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine("Error starting mission: {0}", ex);
            Environment.Exit(1);
        }

        WorldState worldState;

        Console.WriteLine("Waiting for the mission to start");
        do
        {
            Console.Write(".");
            Thread.Sleep(100);
            worldState = agentHost.getWorldState();

            foreach (TimestampedString error in worldState.errors) Console.Error.WriteLine("Error: {0}", error.text);
        }
        while (!worldState.is_mission_running);
        
        Console.WriteLine();

        Random rand = new Random();
        // main loop:
        do
        {
            agentHost.sendCommand("move 1");
            agentHost.sendCommand(string.Format("turn {0}", rand.NextDouble()));
            Thread.Sleep(500);
            worldState = agentHost.getWorldState();
            Console.WriteLine(
                "video,observations,rewards received: {0}, {1}, {2}",
                worldState.number_of_video_frames_since_last_state,
                worldState.number_of_observations_since_last_state,
                worldState.number_of_rewards_since_last_state);
            foreach (TimestampedFloat reward in worldState.rewards) Console.Error.WriteLine("Summed reward: {0}", reward.value);
            foreach (TimestampedString error in worldState.errors) Console.Error.WriteLine("Error: {0}", error.text);
        }
        while (worldState.is_mission_running);

        Console.WriteLine("Mission has stopped.");
    }
}
