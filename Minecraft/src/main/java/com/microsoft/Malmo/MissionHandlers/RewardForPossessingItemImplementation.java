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
import com.microsoft.Malmo.MissionHandlers.RewardForDiscardingItemImplementation.LoseItemEvent;
import com.microsoft.Malmo.Schemas.RewardForPossessingItem;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Sends a reward when the agent possesses the specified item with specified amounts. 
 * The counter is relative, meaning it goes down if items are placed, lost, or destroyed.
 */
public class RewardForPossessingItemImplementation extends RewardForItemBase implements IRewardProducer {
    private RewardForPossessingItem params;
    private ArrayList<ItemMatcher> matchers;
    /**
     * A current mapping of strings to the amount of item we have
     */
    private HashMap<String, Integer> possessedItems;

    /**
     * A mapping of strings to the highest amount of an item we had at any single time
     */
    private HashMap<String, Integer> maxPossessedItems;

    @SubscribeEvent
    public void onGainItem(RewardForCollectingItemImplementation.GainItemEvent event) {
        if (event.stack != null)
            checkForMatch(event.stack);
    }

    @SubscribeEvent
    public void onPickupItem(EntityItemPickupEvent event) {
        if (event.getItem() != null && event.getEntityPlayer() instanceof EntityPlayerMP)
            checkForMatch(event.getItem().getEntityItem());
    }

    @SubscribeEvent
    public void onItemCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.player instanceof EntityPlayerMP && !event.crafting.isEmpty())
            checkForMatch(event.crafting);
    }

    @SubscribeEvent
    public void onItemSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (event.player instanceof EntityPlayerMP && !event.smelting.isEmpty())
            checkForMatch(event.smelting);
    }

    @SubscribeEvent
    public void onLoseItem(LoseItemEvent event) {
        if (event.stack != null && event.cause == 0)
            removeCollectedItemCount(event.stack);
    }

    @SubscribeEvent
    public void onDropItem(ItemTossEvent event) {
        if (event.getPlayer() instanceof EntityPlayerMP)
            removeCollectedItemCount(event.getEntityItem().getEntityItem());
    }

    @SubscribeEvent
    public void onDestroyItem(PlayerDestroyItemEvent event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP)
            removeCollectedItemCount(event.getOriginal());
    }

    @SubscribeEvent
    public void onBlockPlace(PlaceEvent event) {
        if (!event.isCanceled() && event.getPlacedBlock() != null && event.getPlayer() instanceof EntityPlayerMP)
            removeCollectedItemCount(new ItemStack(event.getPlacedBlock().getBlock()));
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

    /**
     * Since there are two counters, returns the current value of the items we have collected.
     * Logic regarding the difference between active and max counter of items done below.
     *
     * @param is The item stack to get the count from
     * @return The count, 0 if not encountered/collected before
     */
    private int getCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant)
            return (possessedItems.get(is.getUnlocalizedName()) == null) ? 0 : possessedItems.get(is.getUnlocalizedName());
        else
            return (possessedItems.get(is.getItem().getUnlocalizedName()) == null) ? 0
                    : possessedItems.get(is.getItem().getUnlocalizedName());
    }

    /**
     * Since there are two counters, returns the max value of the items we have collected.
     * Logic regarding the difference between active and max counter of items done below.
     *
     * @param is The item stack to get the count from
     * @return The count, 0 if not encountered/collected before
     */
    private int getMaxCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant)
            return (maxPossessedItems.get(is.getUnlocalizedName()) == null) ? 0 : maxPossessedItems.get(is.getUnlocalizedName());
        else
            return (maxPossessedItems.get(is.getItem().getUnlocalizedName()) == null) ? 0
                    : maxPossessedItems.get(is.getItem().getUnlocalizedName());
    }

    private void addCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant) {
            int prev = (possessedItems.get(is.getUnlocalizedName()) == null ? 0
                    : possessedItems.get(is.getUnlocalizedName()));
            int maxPrev = (maxPossessedItems.get(is.getUnlocalizedName()) == null) ? 0
                    : maxPossessedItems.get(is.getUnlocalizedName());
            possessedItems.put(is.getUnlocalizedName(), prev + is.getCount());

            if (prev + is.getCount() > maxPrev)
                maxPossessedItems.put(is.getUnlocalizedName(), prev + is.getCount());
        } else {
            int prev = (possessedItems.get(is.getItem().getUnlocalizedName()) == null ? 0
                    : possessedItems.get(is.getItem().getUnlocalizedName()));
            int maxPrev = (maxPossessedItems.get(is.getItem().getUnlocalizedName()) == null) ? 0
                    : maxPossessedItems.get(is.getItem().getUnlocalizedName());
            possessedItems.put(is.getItem().getUnlocalizedName(), prev + is.getCount());

            if (prev + is.getCount() > maxPrev)
                maxPossessedItems.put(is.getItem().getUnlocalizedName(), prev + is.getCount());
        }
    }

    private void removeCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant) {
            int prev = (possessedItems.get(is.getUnlocalizedName()) == null ? 0
                    : possessedItems.get(is.getUnlocalizedName()));
            possessedItems.put(is.getUnlocalizedName(), prev - is.getCount());
        } else {
            int prev = (possessedItems.get(is.getItem().getUnlocalizedName()) == null ? 0
                    : possessedItems.get(is.getItem().getUnlocalizedName()));
            possessedItems.put(is.getItem().getUnlocalizedName(), prev - is.getCount());
        }
    }

    private void checkForMatch(ItemStack is) {
        int savedCollected = getCollectedItemCount(is);
        int maxCollected = getMaxCollectedItemCount(is);
        if (is != null) {
            for (ItemMatcher matcher : this.matchers) {
                if (matcher.matches(is)) {
                    if (!params.isSparse()) {
                        if (savedCollected != 0 && savedCollected < matcher.matchSpec.getAmount()) {
                            for (int i = savedCollected; i < matcher.matchSpec.getAmount()
                                    && i - savedCollected < is.getCount(); i++) {
                                if (i >= maxCollected) {
                                    int dimension = params.getDimension();
                                    float adjusted_reward = this.adjustAndDistributeReward(
                                            ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                            params.getDimension(),
                                            ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                                    addCachedReward(dimension, adjusted_reward);
                                }
                            }
                        } else if (savedCollected == 0)
                            for (int i = 0; i < is.getCount() && i < matcher.matchSpec.getAmount(); i++) {
                                if (i >= maxCollected) {
                                    int dimension = params.getDimension();
                                    float adjusted_reward = this.adjustAndDistributeReward(
                                            ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                            params.getDimension(),
                                            ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                                    addCachedReward(dimension, adjusted_reward);
                                }
                            }
                    } else if (savedCollected < matcher.matchSpec.getAmount()
                            && savedCollected + is.getCount() >= matcher.matchSpec.getAmount()) {
                        int dimension = params.getDimension();
                        float adjusted_reward = this.adjustAndDistributeReward(
                                ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                params.getDimension(),
                                ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                        addCachedReward(dimension, adjusted_reward);
                    }
                }
            }

            addCollectedItemCount(is);
        }
    }

    @Override
    public boolean parseParameters(Object params) {
        if (!(params instanceof RewardForPossessingItem))
            return false;

        matchers = new ArrayList<ItemMatcher>();

        this.params = (RewardForPossessingItem) params;
        for (BlockOrItemSpecWithReward spec : this.params.getItem())
            this.matchers.add(new ItemMatcher(spec));

        return true;
    }

    @Override
    public void prepare(MissionInit missionInit) {
        super.prepare(missionInit);
        MinecraftForge.EVENT_BUS.register(this);
        possessedItems = new HashMap<String, Integer>();
        maxPossessedItems = new HashMap<String, Integer>();
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
