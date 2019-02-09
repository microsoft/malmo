package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.IMalmoMessageListener;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithReward;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForCraftingItem;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Sends a reward when the agent crafts the specified item with
 * specified amounts.
 */
public class RewardForCraftingItemImplementation extends RewardForItemBase
        implements IRewardProducer, IMalmoMessageListener {

    private RewardForCraftingItem params;
    private ArrayList<ItemMatcher> matchers;
    private HashMap<String, Integer> craftedItems;
    private boolean callCraft = true;

    @SubscribeEvent
    public void onItemCraft(PlayerEvent.ItemCraftedEvent event) {
        if (callCraft)
            checkForMatch(event.crafting);

        callCraft = !callCraft;
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

    private int getCraftedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant)
            return (craftedItems.get(is.getUnlocalizedName()) == null) ? 0 : craftedItems.get(is.getUnlocalizedName());
        else
            return (craftedItems.get(is.getItem().getUnlocalizedName()) == null) ? 0
                    : craftedItems.get(is.getItem().getUnlocalizedName());
    }

    private void addCraftedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        int prev = (craftedItems.get(is.getUnlocalizedName()) == null ? 0
                : craftedItems.get(is.getUnlocalizedName()));
        if (variant)
            craftedItems.put(is.getUnlocalizedName(), prev + is.getCount());
        else
            craftedItems.put(is.getItem().getUnlocalizedName(), prev + is.getCount());
    }

    private void checkForMatch(ItemStack is) {
        int savedCrafted = getCraftedItemCount(is);
        if (is != null) {
            for (ItemMatcher matcher : this.matchers) {
                if (matcher.matches(is)) {
                    if (!params.isSparse()) {
                        if (savedCrafted != 0 && savedCrafted < matcher.matchSpec.getAmount()) {
                            for (int i = savedCrafted; i < matcher.matchSpec.getAmount()
                                    && i - savedCrafted < is.getCount(); i++) {
                                this.adjustAndDistributeReward(
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                        params.getDimension(),
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                            }

                        } else if (savedCrafted != 0 && savedCrafted >= matcher.matchSpec.getAmount()) {
                            // Do nothing
                        } else {
                            for (int i = 0; i < is.getCount() && i < matcher.matchSpec.getAmount(); i++) {
                                this.adjustAndDistributeReward(
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                        params.getDimension(),
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                            }
                        }
                    } else {
                        if (savedCrafted < matcher.matchSpec.getAmount()
                                && savedCrafted + is.getCount() >= matcher.matchSpec.getAmount()) {
                            this.adjustAndDistributeReward(
                                    ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                    params.getDimension(),
                                    ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                        }
                    }
                }
            }

            addCraftedItemCount(is);
        }
    }

    @Override
    public boolean parseParameters(Object params) {
        if (!(params instanceof RewardForCraftingItem))
            return false;

        matchers = new ArrayList<ItemMatcher>();

        this.params = (RewardForCraftingItem) params;
        for (BlockOrItemSpecWithReward spec : this.params.getItem())
            this.matchers.add(new ItemMatcher(spec));

        return true;
    }

    @Override
    public void prepare(MissionInit missionInit) {
        super.prepare(missionInit);
        MinecraftForge.EVENT_BUS.register(this);
        MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_COLLECTITEM);
        craftedItems = new HashMap<String, Integer>();
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward) {
        super.getReward(missionInit, reward);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        MinecraftForge.EVENT_BUS.unregister(this);
        MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_COLLECTITEM);
    }

    @Override
    public void onMessage(MalmoMessageType messageType, Map<String, String> data) {
    }
}