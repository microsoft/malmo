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

using System;
using Microsoft.Research.Malmo;

class Program
{
    public static void Main(string[] args)
    {
        using (ALEAgentHost agent_host = new ALEAgentHost())
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
