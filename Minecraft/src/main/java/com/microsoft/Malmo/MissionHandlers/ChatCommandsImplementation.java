package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.Schemas.ChatCommand;
import com.microsoft.Malmo.Schemas.ChatCommands;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Chat commands allow the players to broadcast text messages. */
public class ChatCommandsImplementation extends CommandBase implements ICommandHandler
{
    private boolean isOverriding;
    
    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null)
        {
            return false;
        }
        
        if (!verb.equalsIgnoreCase(ChatCommand.CHAT.value()))
        {
            return false;
        }
        
        player.sendChatMessage( parameter );
        return true;
    }

    @Override
    public boolean parseParameters(Object params)
    {
    	if (params == null || !(params instanceof ChatCommands))
    		return false;
    	
    	ChatCommands cparams = (ChatCommands)params;
    	setUpAllowAndDenyLists(cparams.getModifierList());
    	return true;
    }

    @Override
    public void install(MissionInit missionInit)
    {
    }

    @Override
    public void deinstall(MissionInit missionInit)
    {
    }

    @Override
    public boolean isOverriding()
    {
        return this.isOverriding;
    }

    @Override
    public void setOverriding(boolean b)
    {
        this.isOverriding = b;
    }
}
