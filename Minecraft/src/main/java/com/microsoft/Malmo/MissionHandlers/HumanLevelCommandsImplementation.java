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
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        if (verb.equalsIgnoreCase(HumanLevelCommand.MOVE_MOUSE.value()))
        {
            if (parameter != null && parameter.length() != 0)
            {
                String[] params = parameter.split(" ");
                if (params.length != 2 && params.length != 3)
                {
                    System.out.println("Malformed parameter string (" + parameter + ") - expected <x> <y>, or <x> <y> <z>");
                    return false;   // Error - incorrect number of parameters.
                }
                Integer x, y, z;
                try
                {
                    x = Integer.valueOf(params[0]);
                    y = Integer.valueOf(params[1]);
                    z = params.length == 3 ? Integer.valueOf(params[2]) : 0;
                }
                catch (NumberFormatException e)
                {
                    System.out.println("Malformed parameter string (" + parameter + ") - " + e.getMessage());
                    return false;
                }
                if (x == null || y == null)
                {
                    System.out.println("Malformed parameter string (" + parameter + ")");
                    return false;   // Error - incorrect parameters.
                }
                if (x != 0 || y != 0)
                {
                    // Code based on EntityRenderer.updateCameraAndRender:
                    Minecraft mc = Minecraft.getMinecraft();
                    float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
                    float f1 = f * f * f * 8.0F;
                    float f2 = (float)x * f1;
                    float f3 = (float)y * f1;
                    int i = 1;

                    if (mc.gameSettings.invertMouse)
                        i = -1;

                    mc.player.turn(f2, f3 * (float)i);
                }
                if (z != 0)
                {
                    // Code based on Minecraft.runTickMouse
                    if (!Minecraft.getMinecraft().player.isSpectator())
                    {
                        Minecraft.getMinecraft().player.inventory.changeCurrentItem(z);
                    }
                }
            }
        }
        return super.onExecute(verb, parameter, missionInit);
    }

    @Override
    public boolean isFixed() { return true; }
}
