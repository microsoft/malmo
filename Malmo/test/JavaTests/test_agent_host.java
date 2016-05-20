// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

public class test_agent_host
{
    static 
    {
        System.loadLibrary("MalmoJava");
    }  

    public static void main(String argv[]) 
    {
        AgentHost agent_host = new AgentHost();
        agent_host.setVideoPolicy( AgentHost.VideoPolicy.LATEST_FRAME_ONLY );
        agent_host.setRewardsPolicy( AgentHost.RewardsPolicy.SUM_REWARDS );
        agent_host.setObservationsPolicy( AgentHost.ObservationsPolicy.LATEST_OBSERVATION_ONLY );
        
        WorldState world_state = agent_host.getWorldState();

        if( world_state.getIsMissionRunning() )
            System.exit(1);
        
        if( world_state.getNumberOfObservationsSinceLastState() != 0 )
            System.exit(1);
        
        if( world_state.getNumberOfRewardsSinceLastState() != 0 )
            System.exit(1);
        
        if( world_state.getNumberOfVideoFramesSinceLastState() != 0 )
            System.exit(1);
        
        if( world_state.getObservations().size() != 0 )
            System.exit(1);
        
        if( world_state.getRewards().size() != 0 )
            System.exit(1);
        
        if( world_state.getVideoFrames().size() != 0 )
            System.exit(1);
        
        System.out.println( agent_host.getUsage() );
    }
}
