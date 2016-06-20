package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Schemas.ContinuousMovementCommands;

/** Default set of command handlers for the Minecraft agent we all know and love.<br>
 * It contains the default key handlers for attack/use/hotbar, the standard robot-style navigation, and the mouse-suppression.
 */
public class ContinuousMovementCommandsImplementation extends CommandGroup
{
    public ContinuousMovementCommandsImplementation()
    {
        setShareParametersWithChildren(true);	// Pass our parameter block on to the following children:
        this.addCommandHandler(new CommandForAttackAndUseImplementation());
        this.addCommandHandler(new CommandForHotBarKeysImplementation());
        this.addCommandHandler(new CommandForWheeledRobotNavigationImplementation());
    }
    
    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof ContinuousMovementCommands))
            return false;

        ContinuousMovementCommands cmparams = (ContinuousMovementCommands)params;
        setUpAllowAndDenyLists(cmparams.getModifierList());
        return true;
    }
}
