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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.Map;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.IMalmoMessageListener;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithReward;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForDiscardingItem;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class RewardForDiscardingItemImplementation extends RewardForItemBase implements IRewardProducer, IMalmoMessageListener
{
    private RewardForDiscardingItem params;

    @Override
    public void onMessage(MalmoMessageType messageType, Map<String, String> data) 
    {
        ByteBuf buf = Unpooled.copiedBuffer(data.get("message").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ItemStack itemStack = ByteBufUtils.readItemStack(buf);
        accumulateReward(this.params.getDimension(), itemStack);
    }
    
    public static class LoseItemEvent extends Event
    {
        public final ItemStack stack;

        public LoseItemEvent(ItemStack stack)
        {
            this.stack = stack;
        }
    }

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof RewardForDiscardingItem))
            return false;

        // Build up a map of rewards per item:
        this.params = (RewardForDiscardingItem)params;
        for (BlockOrItemSpecWithReward is : this.params.getItem())
            addItemSpecToRewardStructure(is);

        return true;
    }

    @SubscribeEvent
    public void onLoseItem(LoseItemEvent event)
    {
        if (event.stack != null)
        {
            accumulateReward(this.params.getDimension(), event.stack);
        }
    }

    @SubscribeEvent
    public void onTossItem(ItemTossEvent event)
    {
        if (event.entityItem != null)
        {
            ItemStack stack = event.entityItem.getEntityItem();
            accumulateReward(this.params.getDimension(),stack);
        }
    }

    @SubscribeEvent
    public void onPlaceBlock(BlockEvent.PlaceEvent event)
    {
        if (event.itemInHand != null && event.player instanceof EntityPlayerMP ) {
            // This event is received on the server side, so we need to pass it to the client.
            ByteBuf buf = Unpooled.buffer();
            ByteBufUtils.writeItemStack(buf, event.itemInHand);
            MalmoMod.MalmoMessage msg = new MalmoMod.MalmoMessage(
                    MalmoMessageType.SERVER_PLACEBLOCK,
                    buf.toString(java.nio.charset.StandardCharsets.UTF_8)
                    );
            MalmoMod.network.sendTo( msg, (EntityPlayerMP)event.player);
        }
    }

    @Override
    public void getReward(MissionInit missionInit,MultidimensionalReward reward)
    {
        // Return the rewards that have accumulated since last time we were asked:
        reward.add( this.accumulatedRewards );
        // And reset the count:
        this.accumulatedRewards.clear();
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        MinecraftForge.EVENT_BUS.register(this);
        MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_PLACEBLOCK);
    }

    @Override
    public void cleanup()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
        MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_PLACEBLOCK);
    }
}