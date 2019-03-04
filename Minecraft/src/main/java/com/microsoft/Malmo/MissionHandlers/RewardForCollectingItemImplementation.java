package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.HashMap;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithReward;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForCollectingItem;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Sends a reward when the agent collected the specified item with
 * specified amounts. Counter is absolute.
 */
public class RewardForCollectingItemImplementation extends RewardForItemBase implements IRewardProducer {
    private RewardForCollectingItem params;
    private ArrayList<ItemMatcher> matchers;
    private HashMap<String, Integer> collectedItems;

    @SubscribeEvent
    public void onGainItem(RewardForCollectingItemImplementation.GainItemEvent event) {
        if (event.stack != null && event.cause == 0)
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

    private int getCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant)
            return (collectedItems.get(is.getUnlocalizedName()) == null) ? 0 : collectedItems.get(is.getUnlocalizedName());
        else
            return (collectedItems.get(is.getItem().getUnlocalizedName()) == null) ? 0
                    : collectedItems.get(is.getItem().getUnlocalizedName());
    }

    private void addCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant) {
            int prev = (collectedItems.get(is.getUnlocalizedName()) == null ? 0
                    : collectedItems.get(is.getUnlocalizedName()));
            collectedItems.put(is.getUnlocalizedName(), prev + is.getCount());
        } else {
            int prev = (collectedItems.get(is.getItem().getUnlocalizedName()) == null ? 0
                    : collectedItems.get(is.getItem().getUnlocalizedName()));
            collectedItems.put(is.getItem().getUnlocalizedName(), prev + is.getCount());
        }
    }

    private void checkForMatch(ItemStack is) {
        int savedCollected = getCollectedItemCount(is);
        if (is != null) {
            for (ItemMatcher matcher : this.matchers) {
                if (matcher.matches(is)) {
                    if (!params.isSparse()) {
                        if (savedCollected != 0 && savedCollected < matcher.matchSpec.getAmount())
                            for (int i = savedCollected; i < matcher.matchSpec.getAmount()
                                    && i - savedCollected < is.getCount(); i++)
                                this.adjustAndDistributeReward(
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                        params.getDimension(),
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                        else if (savedCollected == 0)
                            for (int i = 0; i < is.getCount() && i < matcher.matchSpec.getAmount(); i++)
                                this.adjustAndDistributeReward(
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                        params.getDimension(),
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                    } else if (savedCollected < matcher.matchSpec.getAmount()
                            && savedCollected + is.getCount() >= matcher.matchSpec.getAmount())
                        this.adjustAndDistributeReward(
                                ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                params.getDimension(),
                                ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                }
            }

            addCollectedItemCount(is);
        }
    }

    @Override
    public boolean parseParameters(Object params) {
        if (!(params instanceof RewardForCollectingItem))
            return false;

        matchers = new ArrayList<ItemMatcher>();

        this.params = (RewardForCollectingItem) params;
        for (BlockOrItemSpecWithReward spec : this.params.getItem())
            this.matchers.add(new ItemMatcher(spec));

        return true;
    }

    @Override
    public void prepare(MissionInit missionInit) {
        super.prepare(missionInit);
        MinecraftForge.EVENT_BUS.register(this);
        collectedItems = new HashMap<String, Integer>();
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

    public static class GainItemEvent extends Event {
        public final ItemStack stack;

        /**
         * Sets the cause of the GainItemEvent. By default, is 0. If it is from auto-crafting, then is 1. If it is from auto-smelting, then is 2.
         */
        public int cause = 0;

        public GainItemEvent(ItemStack stack) {
            this.stack = stack;
        }

        public void setCause(int cause) {
            this.cause = cause;
        }
    }
}