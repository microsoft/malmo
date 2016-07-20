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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithReward;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

public abstract class RewardForItemBase extends HandlerBase
{
    protected MultidimensionalReward accumulatedRewards = new MultidimensionalReward();
    List<ItemMatcher> rewardMatchers = new ArrayList<ItemMatcher>();

    class ItemMatcher
    {
        List<String> allowedItemTypes = new ArrayList<String>();
        BlockOrItemSpecWithReward rewardSpec;

        ItemMatcher(BlockOrItemSpecWithReward spec)
        {
            this.rewardSpec = spec;

            for (String itemType : spec.getType())
            {
                Item item = MinecraftTypeHelper.ParseItemType(itemType, true);
                if (item != null)
                    this.allowedItemTypes.add(item.getUnlocalizedName());
            }
        }

        float reward()
        {
            return this.rewardSpec.getReward().floatValue();
        }

        boolean matches(ItemStack stack)
        {
            String item = stack.getItem().getUnlocalizedName();
            if (!this.allowedItemTypes.contains(item))
                return false;
            // Our item type matches, but we may need to compare block attributes too:
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block != null)
            {
                // Might need to check colour and variant.
                if (this.rewardSpec.getColour() != null && !this.rewardSpec.getColour().isEmpty())
                {
                    if (!MinecraftTypeHelper.blockColourMatches(block.getDefaultState(), this.rewardSpec.getColour()))
                        return false;
                }
                
                // Matches type and colour, but does the variant match?
                if (this.rewardSpec.getVariant() != null && !this.rewardSpec.getVariant().isEmpty())
                {
                    if (!MinecraftTypeHelper.blockVariantMatches(block.getDefaultState(), this.rewardSpec.getVariant()))
                        return false;
                }
            }
            return true;
        }
    }

    protected void addItemSpecToRewardStructure(BlockOrItemSpecWithReward is)
    {
        this.rewardMatchers.add(new ItemMatcher(is));
    }

    protected void accumulateReward(int dimension, ItemStack stack)
    {
        for (ItemMatcher matcher : this.rewardMatchers)
        {
            if (matcher.matches(stack))
            {
                this.accumulatedRewards.add(dimension, stack.stackSize * matcher.reward());
            }
        }
    }
}
