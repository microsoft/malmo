package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Schemas.ContinuousMovementCommands;

public class HumanLevelCommandsImplementation extends CommandGroup
{
    public HumanLevelCommandsImplementation()
    {
        setShareParametersWithChildren(true);   // Pass our parameter block on to the following children:

        addCommandHandler(new CommandForKey("key.forward"));
        addCommandHandler(new CommandForKey("key.left"));
        addCommandHandler(new CommandForKey("key.back"));
        addCommandHandler(new CommandForKey("key.right"));
        addCommandHandler(new CommandForKey("key.jump"));
        addCommandHandler(new CommandForKey("key.sneak"));
        addCommandHandler(new CommandForKey("key.sprint"));
        addCommandHandler(new CommandForKey("key.inventory"));
        addCommandHandler(new CommandForKey("key.swapHands"));
        addCommandHandler(new CommandForKey("key.drop"));
        addCommandHandler(new CommandForKey("key.use"));
        addCommandHandler(new CommandForKey("key.attack"));
        addCommandHandler(new CommandForKey("key.pickItem"));
        for (int i = 1; i <= 9; i++)
        {
            addCommandHandler(new CommandForKey("key.hotbar." + i));
        }        
    }

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        
        if (params == null || !(params instanceof HumanLevelCommands))
            return false;
    
        HumanLevelCommands cmparams = (HumanLevelCommands)params;
        setUpAllowAndDenyLists(cmparams.getModifierList());
        return true;
    }
    
    @Override
    public boolean isFixed() { return true; }
}
