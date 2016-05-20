package com.microsoft.Malmo.MissionHandlers;

public class CommandForAttackAndUseImplementation extends CommandGroup
{
    public CommandForAttackAndUseImplementation()
    {
        super();
        setShareParametersWithChildren(true);	// Pass our parameter block on to the following children:
        addCommandHandler(new CommandForKey("key.attack"));
        addCommandHandler(new CommandForKey("key.use"));
    }
}
