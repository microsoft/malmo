package com.microsoft.Malmo.MissionHandlers;

import java.util.HashMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.ItemSpec;
import com.microsoft.Malmo.Schemas.ItemType;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForCollectingItem;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

public class RewardForCollectingItemImplementation extends HandlerBase implements IRewardProducer
{
	float accumulatedRewards = 0;
	HashMap<String, Float> rewardMap = new HashMap<String, Float>();
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof RewardForCollectingItem))
			return false;

		// Build up a map of rewards per item:
		RewardForCollectingItem rciparams = (RewardForCollectingItem)params;
		for (ItemSpec is : rciparams.getItem())
		{
			for (ItemType it : is.getType())
			{
				Item item = MinecraftTypeHelper.ParseItemType(it.value());
				if (item != null)
				{
					String itemName = item.getUnlocalizedName();
					if (!this.rewardMap.containsKey(itemName))
						this.rewardMap.put(itemName, is.getReward().floatValue());
					else
						this.rewardMap.put(itemName, this.rewardMap.get(itemName) + is.getReward().floatValue());
				}
			}
		}

		return true;
	}

	@SubscribeEvent
	public void onPickupItem(EntityItemPickupEvent event)
	{
		if (event.item != null && event.item.getEntityItem() != null)
		{
			ItemStack stack = event.item.getEntityItem();
			String item = stack.getItem().getUnlocalizedName();
			Float f = rewardMap.get(item);
			if (f != null)
				this.accumulatedRewards += f * stack.stackSize;
		}
	}

	@Override
	public float getReward(MissionInit missionInit)
	{
		// Return the rewards that have accumulated since last time we were asked:
		float f = this.accumulatedRewards;
		// And reset the count:
		this.accumulatedRewards = 0;
        return f;
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