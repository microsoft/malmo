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

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithReward;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForCollectingItem;

public class RewardForCollectingItemImplementation extends RewardForItemBase implements IRewardProducer
{
    private RewardForCollectingItem params;

    public static class GainItemEvent extends Event
    {
        public final ItemStack stack;

        public GainItemEvent(ItemStack stack)
        {
            this.stack = stack;
        }
    }

    @Override
    public boolean parseParameters(Object params) {
        if (params == null || !(params instanceof RewardForCollectingItem))
            return false;

        // Build up a map of rewards per item:
        this.params = (RewardForCollectingItem) params;
        for (BlockOrItemSpecWithReward is : this.params.getItem())
            addItemSpecToRewardStructure(is);

        return true;
    }

    @SubscribeEvent
    public void onGainItem(GainItemEvent event)
    {
        if (event.stack != null)
        {
            accumulateReward(this.params.getDimension(), event.stack);
        }
    }

    @SubscribeEvent
    public void onPickupItem(EntityItemPickupEvent event)
    {
        if (event.item != null && event.item.getEntityItem() != null)
        {
            ItemStack stack = event.item.getEntityItem();
            accumulateReward(this.params.getDimension(), stack);
        }
    }

    @Override
    public void prepare(MissionInit missionInit) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward) {
        // Return the rewards that have accumulated since last time we were
        // asked:
        reward.add(this.accumulatedRewards);
        // And reset the count:
        this.accumulatedRewards.clear();
    }

    @Override
    public void cleanup() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
