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

package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

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
        EntityPlayerSP player = Minecraft.getMinecraft().player;
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
