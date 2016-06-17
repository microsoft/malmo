package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.ItemSpec;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForCollectingItem;
import com.microsoft.Malmo.Schemas.RewardForDiscardingItem;

public class RewardForDiscardingItemImplementation extends RewardForItemBase implements IRewardProducer
{
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof RewardForDiscardingItem))
			return false;

		// Build up a map of rewards per item:
		RewardForDiscardingItem rdiparams = (RewardForDiscardingItem)params;
		for (ItemSpec is : rdiparams.getItem())
		    addItemSpecToRewardStructure(is);

		return true;
	}

	@SubscribeEvent
	public void onTossItem(ItemTossEvent event)
	{
	    if (event.entityItem != null)
	    {
	        ItemStack stack = event.entityItem.getEntityItem();
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