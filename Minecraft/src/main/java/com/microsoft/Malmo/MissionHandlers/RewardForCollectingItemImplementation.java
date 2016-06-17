package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.ItemSpec;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForCollectingItem;

public class RewardForCollectingItemImplementation extends RewardForItemBase implements IRewardProducer
{
    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof RewardForCollectingItem))
            return false;

        // Build up a map of rewards per item:
        RewardForCollectingItem rciparams = (RewardForCollectingItem)params;
        for (ItemSpec is : rciparams.getItem())
            addItemSpecToRewardStructure(is);

        return true;
    }

    @SubscribeEvent
    public void onPickupItem(EntityItemPickupEvent event)
    {
        if (event.item != null && event.item.getEntityItem() != null)
        {
            ItemStack stack = event.item.getEntityItem();
            accumulateReward(stack);
        }
    }

    @Override
    public float getReward(MissionInit missionInit)
    {
        return getReward();
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void cleanup()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}