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

import io.netty.buffer.ByteBuf;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.SimpleCraftCommand;
import com.microsoft.Malmo.Schemas.SimpleCraftCommands;
import com.microsoft.Malmo.Utils.CraftingHelper;

public class SimpleCraftCommandsImplementation extends CommandBase
{
    private boolean isOverriding;

    public static class CraftMessage implements IMessage
    {
        String parameters;
        public CraftMessage()
        {
        }
    
        public CraftMessage(String parameters)
        {
            this.parameters = parameters;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            this.parameters = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            ByteBufUtils.writeUTF8String(buf, this.parameters);
        }
    }

    public static class CraftMessageHandler implements IMessageHandler<CraftMessage, IMessage>
    {
        @Override
        public IMessage onMessage(CraftMessage message, MessageContext ctx)
        {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            // Try crafting recipes first:
            List<IRecipe> matching_recipes;
            String[] split = message.parameters.split(" ");
            matching_recipes = CraftingHelper.getRecipesForRequestedOutput(message.parameters, split.length > 1);

            for (IRecipe recipe : matching_recipes)
            {
                if (CraftingHelper.attemptCrafting(player, recipe))
                    return null;
            }
            // Now try furnace recipes:
            ItemStack input = CraftingHelper.getSmeltingRecipeForRequestedOutput(message.parameters);
            if (input != null)
            {
                if (CraftingHelper.attemptSmelting(player, input))
                    return null;
            }
            return null;
        }
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        if (verb.equalsIgnoreCase(SimpleCraftCommand.CRAFT.value()))
        {
            MalmoMod.network.sendToServer(new CraftMessage(parameter));
            return true;
        }
        return false;
    }

    @Override
    public boolean parseParameters(Object params)
    {
        if (!(params instanceof SimpleCraftCommands))
            return false;
        
        SimpleCraftCommands cparams = (SimpleCraftCommands)params;
        setUpAllowAndDenyLists(cparams.getModifierList());
        return true;
    }

    @Override
    public void install(MissionInit missionInit)
    {
        CraftingHelper.reset();
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
