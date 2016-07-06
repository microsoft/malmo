package com.microsoft.Malmo.MissionHandlers;

import io.netty.buffer.ByteBuf;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
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
            List<IRecipe> matching_recipes = CraftingHelper.getRecipesForRequestedOutput(message.parameters);
            for (IRecipe recipe : matching_recipes)
            {
                if (CraftingHelper.attemptCrafting(player, recipe))
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
        if (params == null || !(params instanceof SimpleCraftCommands))
            return false;
        
        SimpleCraftCommands cparams = (SimpleCraftCommands)params;
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
