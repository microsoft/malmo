// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

public class test_wrapping 
{
    static 
    {
        System.loadLibrary("MalmoJava");
    }  

    public static void main(String argv[]) 
    {
        AgentHost agent_host = new AgentHost();
        System.out.println( agent_host.getUsage() );
    }
}
