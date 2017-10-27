package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;

import com.microsoft.Malmo.Client.MalmoModClient;
import com.microsoft.Malmo.Schemas.HumanLevelCommand;
import com.microsoft.Malmo.Schemas.HumanLevelCommands;
import com.microsoft.Malmo.Schemas.MissionInit;

public class HumanLevelCommandsImplementation extends CommandGroup
{
    public HumanLevelCommandsImplementation()
    {
        super();
        setShareParametersWithChildren(true);   // Pass our parameter block on to the following children:
        List<CommandForKey> keys = getKeyOverrides();
        for (CommandForKey k : keys)
        {
            addCommandHandler(k);
        }
    }

    static public List<CommandForKey> getKeyOverrides()
    {
        List<CommandForKey> keys = new ArrayList<CommandForKey>();
        keys.add(new CommandForKey("key.forward"));
        keys.add(new CommandForKey("key.left"));
        keys.add(new CommandForKey("key.back"));
        keys.add(new CommandForKey("key.right"));
        keys.add(new CommandForKey("key.jump"));
        keys.add(new CommandForKey("key.sneak"));
        keys.add(new CommandForKey("key.sprint"));
        keys.add(new CommandForKey("key.inventory"));
        keys.add(new CommandForKey("key.swapHands"));
        keys.add(new CommandForKey("key.drop"));
        keys.add(new CommandForKey("key.use"));
        keys.add(new CommandForKey("key.attack"));
        keys.add(new CommandForKey("key.pickItem"));
        for (int i = 1; i <= 9; i++)
        {
            keys.add(new CommandForKey("key.hotbar." + i));
        }
        return keys;
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
