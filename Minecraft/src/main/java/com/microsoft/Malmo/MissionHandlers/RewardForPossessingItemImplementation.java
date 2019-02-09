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
import com.microsoft.Malmo.MissionHandlers.RewardForDiscardingItemImplementation.LoseItemEvent;
import com.microsoft.Malmo.Schemas.RewardForPossessingItem;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import javax.xml.bind.DatatypeConverter;

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Sends a reward when the agent possesses the specified item with
 * specified amounts. The counter is relative, meaning it goes down if items are placed, lost, or destroyed.
 */
public class RewardForPossessingItemImplementation extends RewardForItemBase
        implements IRewardProducer, IMalmoMessageListener {

    private RewardForPossessingItem params;
    private ArrayList<ItemMatcher> matchers;
    private HashMap<String, Integer> collectedItems;

    @SubscribeEvent
    public void onPickupItem(EntityItemPickupEvent event) {
        if (event.getItem() != null) {
            checkForMatch(event.getItem().getEntityItem());
            if (event.getEntityPlayer() instanceof EntityPlayerMP)
                sendItemStackToClient((EntityPlayerMP) event.getEntityPlayer(), MalmoMessageType.SERVER_COLLECTITEM, event.getItem().getEntityItem());
        }
    }

    @SubscribeEvent
    public void onGainItem(GainItemEvent event) {
        if (event.stack != null) {
            accumulateReward(this.params.getDimension(), event.stack);
        }
    }

    @SubscribeEvent
    public void onLoseItem(LoseItemEvent event) {
        if (event.stack != null)
            removeCollectedItemCount(event.stack);
    }

    @SubscribeEvent
    public void onDropItem(ItemTossEvent event) {
        removeCollectedItemCount(event.getEntityItem().getEntityItem());
    }

    @SubscribeEvent
    public void onDestroyItem(PlayerDestroyItemEvent event) {
        removeCollectedItemCount(event.getOriginal());
    }

    @SubscribeEvent
    public void onBlockPlace(PlaceEvent event) {
        if (!event.isCanceled() && event.getPlacedBlock() != null) {
            ItemStack stack = new ItemStack(event.getPlacedBlock().getBlock());
            removeCollectedItemCount(stack);
        }
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

        int prev = (collectedItems.get(is.getUnlocalizedName()) == null ? 0
                : collectedItems.get(is.getUnlocalizedName()));
        if (variant)
            collectedItems.put(is.getUnlocalizedName(), prev + is.getCount());
        else
            collectedItems.put(is.getItem().getUnlocalizedName(), prev + is.getCount());
    }

    private void removeCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        int prev = (collectedItems.get(is.getUnlocalizedName()) == null ? 0
                : collectedItems.get(is.getUnlocalizedName()));
        if (variant)
            collectedItems.put(is.getUnlocalizedName(), prev - is.getCount());
        else
            collectedItems.put(is.getItem().getUnlocalizedName(), prev - is.getCount());
    }

    private void checkForMatch(ItemStack is) {
        int savedCollected = getCollectedItemCount(is);
        if (is != null) {
            for (ItemMatcher matcher : this.matchers) {
                if (matcher.matches(is)) {
                    if (!params.isSparse()) {
                        if (savedCollected != 0 && savedCollected < matcher.matchSpec.getAmount()) {
                            for (int i = savedCollected; i < matcher.matchSpec.getAmount()
                                    && i - savedCollected < is.getCount(); i++) {
                                this.adjustAndDistributeReward(
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                        params.getDimension(),
                                        ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                            }

                        } else if (savedCollected != 0 && savedCollected >= matcher.matchSpec.getAmount()) {
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
                        if (savedCollected < matcher.matchSpec.getAmount()
                                && savedCollected + is.getCount() >= matcher.matchSpec.getAmount()) {
                            this.adjustAndDistributeReward(
                                    ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
                                    params.getDimension(),
                                    ((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
                        }
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
        MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_COLLECTITEM);
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
        MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_COLLECTITEM);
    }

    @Override
    public void onMessage(MalmoMessageType messageType, Map<String, String> data) {
        String buffString = data.get("message");
        ByteBuf buf = Unpooled.copiedBuffer(DatatypeConverter.parseBase64Binary(buffString));
        ItemStack itemStack = ByteBufUtils.readItemStack(buf);
        if (itemStack != null) {
            accumulateReward(this.params.getDimension(), itemStack);
        } else {
            System.out.println("Error - couldn't understand the itemstack we received.");
        }
    }

    public static class GainItemEvent extends Event {
        public final ItemStack stack;

        public GainItemEvent(ItemStack stack) {
            this.stack = stack;
        }
    }
}