package com.microsoft.Malmo.MissionHandlers;

public class CommandForHotBarKeysImplementation extends CommandGroup
{
    public CommandForHotBarKeysImplementation()
    {
        setShareParametersWithChildren(true);	// Pass our parameter block on to the following children:
        for (int i = 1; i <= 9; i++)
        {
            addCommandHandler(new CommandForKey("key.hotbar." + i));
        }        
    }
}