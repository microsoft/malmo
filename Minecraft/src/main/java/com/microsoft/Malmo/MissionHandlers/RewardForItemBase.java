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

import java.util.HashMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.microsoft.Malmo.Schemas.ItemSpec;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

public abstract class RewardForItemBase extends HandlerBase {
    protected MultidimensionalReward accumulatedRewards = new MultidimensionalReward();
    protected HashMap<String, Float> rewardMap = new HashMap<String, Float>();

    protected void addItemSpecToRewardStructure(ItemSpec is) {
        for (String it : is.getType()) {
            Item item = MinecraftTypeHelper.ParseItemType(it);
            if (item != null) {
                String itemName = item.getUnlocalizedName();
                if (!this.rewardMap.containsKey(itemName))
                    this.rewardMap.put(itemName, is.getReward().floatValue());
                else
                    this.rewardMap.put(itemName, this.rewardMap.get(itemName) + is.getReward().floatValue());
            }
        }
    }

    protected void accumulateReward(int dimension, ItemStack stack) {
        String item = stack.getItem().getUnlocalizedName();
        Float f = this.rewardMap.get(item);
        if (f != null) {
            this.accumulatedRewards.add(dimension, f * stack.stackSize);
        }
    }
}
