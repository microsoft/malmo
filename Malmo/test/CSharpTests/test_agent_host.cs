// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

using System;
using Microsoft.Research.Malmo;

class Program
{
    public static void Main(string[] args)
    {
        using (AgentHost agent_host = new AgentHost())
        {
            agent_host.setVideoPolicy(AgentHost.VideoPolicy.LATEST_FRAME_ONLY);
            agent_host.setRewardsPolicy(AgentHost.RewardsPolicy.SUM_REWARDS);
            agent_host.setObservationsPolicy(AgentHost.ObservationsPolicy.LATEST_OBSERVATION_ONLY);

            WorldState world_state = agent_host.getWorldState();

            if (world_state.is_mission_running)
                Environment.Exit(1);

            if (world_state.number_of_observations_since_last_state != 0)
                Environment.Exit(1);

            if (world_state.number_of_rewards_since_last_state != 0)
                Environment.Exit(1);

            if (world_state.number_of_video_frames_since_last_state != 0)
                Environment.Exit(1);

            if (world_state.observations.Count != 0)
                Environment.Exit(1);

            if (world_state.rewards.Count != 0)
                Environment.Exit(1);

            if (world_state.video_frames.Count != 0)
                Environment.Exit(1);

            Console.WriteLine(agent_host.getUsage());
        }
    }
}
