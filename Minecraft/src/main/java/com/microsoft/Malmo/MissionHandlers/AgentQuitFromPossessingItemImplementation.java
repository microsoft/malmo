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
import java.util.List;

import com.microsoft.Malmo.Schemas.AgentQuitFromPossessingItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.MissionHandlers.RewardForCollectingItemImplementation.GainItemEvent;
import com.microsoft.Malmo.MissionHandlers.RewardForDiscardingItemImplementation.LoseItemEvent;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithDescription;
import com.microsoft.Malmo.Schemas.MissionInit;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Quits the mission when the agent has possessed the right amount of items. The count on the item collection is non-absolute.
 * <p>
 * In order to quit the mission, the agent must have the requisite items in its inventory all at one time.
 */
public class AgentQuitFromPossessingItemImplementation extends HandlerBase implements IWantToQuit {
    private AgentQuitFromPossessingItem params;
    private HashMap<String, Integer> possessedItems;
    private List<ItemQuitMatcher> matchers;
    private String quitCode = "";
    private boolean wantToQuit = false;

    public static class ItemQuitMatcher extends RewardForItemBase.ItemMatcher {
        String description;

        ItemQuitMatcher(BlockOrItemSpecWithDescription spec) {
            super(spec);
            this.description = spec.getDescription();
        }

        String description() {
            return this.description;
        }
    }

    @Override
    public boolean parseParameters(Object params) {
        if (!(params instanceof AgentQuitFromPossessingItem))
            return false;

        this.params = (AgentQuitFromPossessingItem) params;
        this.matchers = new ArrayList<ItemQuitMatcher>();
        for (BlockOrItemSpecWithDescription bs : this.params.getItem())
            this.matchers.add(new ItemQuitMatcher(bs));
        return true;
    }

    @Override
    public boolean doIWantToQuit(MissionInit missionInit) {
        return this.wantToQuit;
    }

    @Override
    public String getOutcome() {
        return this.quitCode;
    }

    @Override
    public void prepare(MissionInit missionInit) {
        MinecraftForge.EVENT_BUS.register(this);
        possessedItems = new HashMap<String, Integer>();
    }

    @Override
    public void cleanup() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onGainItem(GainItemEvent event) {
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
        for (ItemQuitMatcher matcher : matchers) {
            if (matcher.allowedItemTypes.contains(is.getItem().getUnlocalizedName())) {
                if (matcher.matchSpec.getColour() != null && matcher.matchSpec.getColour().size() > 0)
                    return true;
                if (matcher.matchSpec.getVariant() != null && matcher.matchSpec.getVariant().size() > 0)
                    return true;
            }
        }

        return false;
    }

    private void addCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant) {
            int prev = (possessedItems.get(is.getUnlocalizedName()) == null ? 0
                    : possessedItems.get(is.getUnlocalizedName()));
            possessedItems.put(is.getUnlocalizedName(), prev + is.getCount());
        } else {
            int prev = (possessedItems.get(is.getItem().getUnlocalizedName()) == null ? 0
                    : possessedItems.get(is.getItem().getUnlocalizedName()));
            possessedItems.put(is.getItem().getUnlocalizedName(), prev + is.getCount());
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

    private int getCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant)
            return (possessedItems.get(is.getUnlocalizedName()) == null) ? 0 : possessedItems.get(is.getUnlocalizedName());
        else
            return (possessedItems.get(is.getItem().getUnlocalizedName()) == null) ? 0
                    : possessedItems.get(is.getItem().getUnlocalizedName());
    }

    private void checkForMatch(ItemStack is) {
        int savedCollected = getCollectedItemCount(is);
        if (is != null) {
            for (ItemQuitMatcher matcher : this.matchers) {
                if (matcher.matches(is)) {
                    if (savedCollected != 0) {
                        if (is.getCount() + savedCollected >= matcher.matchSpec.getAmount()) {
                            this.quitCode = matcher.description();
                            this.wantToQuit = true;
                        }
                    } else if (is.getCount() >= matcher.matchSpec.getAmount()) {
                        this.quitCode = matcher.description();
                        this.wantToQuit = true;
                    }
                }
            }

            addCollectedItemCount(is);
        }
    }
}
