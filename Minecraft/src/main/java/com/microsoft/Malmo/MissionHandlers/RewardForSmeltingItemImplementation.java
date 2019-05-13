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
import java.util.HashMap;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithReward;
import com.microsoft.Malmo.Schemas.MissionInit;

import com.microsoft.Malmo.Schemas.RewardForSmeltingItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Sends a reward when the agent smelts the specified item with
 * specified amounts.
 */
public class RewardForSmeltingItemImplementation extends RewardForItemBase implements IRewardProducer {
    private RewardForSmeltingItem params;
    private ArrayList<ItemMatcher> matchers;
    private HashMap<String, Integer> smeltedItems;

    @SubscribeEvent
    public void onItemSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (event.player instanceof EntityPlayerMP && !event.smelting.isEmpty())
            checkForMatch(event.smelting);
    }

    /**
     * Checks whether the ItemStack matches a variant stored in the item list. If
     * so, returns true, else returns false.
     *
     * @param is The item stack
     * @return If the stack is allowed in the item matchers and has color or
     * variants enabled, returns true, else false.
     */
    private boolean getVariant(ItemStack is) {
        for (ItemMatcher matcher : matchers) {
            if (matcher.allowedItemTypes.contains(is.getItem().getUnlocalizedName())) {
                if (matcher.matchSpec.getColour() != null && matcher.matchSpec.getColour().size() > 0)
                    return true;
                if (matcher.matchSpec.getVariant() != null && matcher.matchSpec.getVariant().size() > 0)
                    return true;
            }
        }

        return false;
    }

    private int getSmeltedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant)
            return (smeltedItems.get(is.getUnlocalizedName()) == null) ? 0 : smeltedItems.get(is.getUnlocalizedName());
        else
            return (smeltedItems.get(is.getItem().getUnlocalizedName()) == null) ? 0
                    : smeltedItems.get(is.getItem().getUnlocalizedName());
    }

    private void addSmeltedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant) {
            int prev = (smeltedItems.get(is.getUnlocalizedName()) == null ? 0
                    : smeltedItems.get(is.getUnlocalizedName()));
            smeltedItems.put(is.getUnlocalizedName(), prev + is.getCount());
        } else {
            int prev = (smeltedItems.get(is.getItem().getUnlocalizedName()) == null ? 0
                    : smeltedItems.get(is.getItem().getUnlocalizedName()));
            smeltedItems.put(is.getItem().getUnlocalizedName(), prev + is.getCount());
        }
    }

    private void checkForMatch(ItemStack is) {
        int savedSmelted = getSmeltedItemCount(is);
        for (ItemMatcher matcher : this.matchers) {
            if (matcher.matches(is)) {
                if (!params.isSparse()) {
                    if (savedSmelted != 0 && savedSmelted < matcher.matchSpec.getAmount()) {
                        for (int i = savedSmelted; i < matcher.matchSpec.getAmount()
                                && i - savedSmelted < is.getCount(); i++) {
                            int dimension = params.getDimension();
                            float adjusted_reward = this.adjustAndDistributeReward(
                                    ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                    params.getDimension(),
                                    ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                            addCachedReward(dimension, adjusted_reward);
                        }
                    } else if (savedSmelted == 0)
                        for (int i = 0; i < is.getCount() && i < matcher.matchSpec.getAmount(); i++) {
                            int dimension = params.getDimension();
                            float adjusted_reward = this.adjustAndDistributeReward(
                                    ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                    params.getDimension(),
                                    ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                            addCachedReward(dimension, adjusted_reward);
                        }
                } else if (savedSmelted < matcher.matchSpec.getAmount()
                        && savedSmelted + is.getCount() >= matcher.matchSpec.getAmount()) {
                    int dimension = params.getDimension();
                    float adjusted_reward = this.adjustAndDistributeReward(
                            ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                            params.getDimension(),
                            ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                    addCachedReward(dimension, adjusted_reward);
                }
            }
        }

        addSmeltedItemCount(is);
    }

    @Override
    public boolean parseParameters(Object params) {
        if (!(params instanceof RewardForSmeltingItem))
            return false;

        matchers = new ArrayList<ItemMatcher>();

        this.params = (RewardForSmeltingItem) params;
        for (BlockOrItemSpecWithReward spec : this.params.getItem())
            this.matchers.add(new ItemMatcher(spec));

        return true;
    }

    @Override
    public void prepare(MissionInit missionInit) {
        super.prepare(missionInit);
        MinecraftForge.EVENT_BUS.register(this);
        smeltedItems = new HashMap<String, Integer>();
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward) {
        super.getReward(missionInit, reward);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
