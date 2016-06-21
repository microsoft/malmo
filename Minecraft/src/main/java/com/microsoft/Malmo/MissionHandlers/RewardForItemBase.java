package com.microsoft.Malmo.MissionHandlers;

import java.util.HashMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.microsoft.Malmo.Schemas.ItemSpec;
import com.microsoft.Malmo.Schemas.ItemType;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

public abstract class RewardForItemBase extends HandlerBase
{
    protected float accumulatedRewards = 0;
    protected HashMap<String, Float> rewardMap = new HashMap<String, Float>();

    protected void addItemSpecToRewardStructure(ItemSpec is)
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
    
    protected void accumulateReward(ItemStack stack)
    {
        String item = stack.getItem().getUnlocalizedName();
        Float f = this.rewardMap.get(item);
        if (f != null)
            this.accumulatedRewards += f * stack.stackSize;
    }
    
    protected float getReward()
    {
        // Return the rewards that have accumulated since last time we were asked:
        float f = this.accumulatedRewards;
        // And reset the count:
        this.accumulatedRewards = 0;
        return f;
    }
}