// --------------------------------------------------------------------------------------------------
// //  Copyright (c) 2016 Microsoft Corporation
// //
// //  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// //  associated documentation files (the "Software"), to deal in the Software without restriction,
// //  including without limitation the rights to use, copy, modify, merge, publish, distribute,
// //  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// //  furnished to do so, subject to the following conditions:
// //
// //  The above copyright notice and this permission notice shall be included in all copies or
// //  substantial portions of the Software.
// //
// //  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// //  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// //  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// //  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// //  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// // --------------------------------------------------------------------------------------------------

package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.MissionHandlers.RewardForCollectingItemImplementation.GainItemEvent;
import com.microsoft.Malmo.Schemas.AgentQuitFromCollectingItem;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithDescription;
import com.microsoft.Malmo.Schemas.MissionInit;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Quits the mission when the agent has collected the right amount of items. The count on the item collection is absolute.
 */
public class AgentQuitFromCollectingItemQuantityImplementation extends HandlerBase implements IWantToQuit {
    private AgentQuitFromCollectingItem params;
    private HashMap<String, Integer> collectedItems;
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
        if (!(params instanceof AgentQuitFromCollectingItem))
            return false;

        this.params = (AgentQuitFromCollectingItem) params;
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
        collectedItems = new HashMap<String, Integer>();
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
            int prev = (collectedItems.get(is.getUnlocalizedName()) == null ? 0
                    : collectedItems.get(is.getUnlocalizedName()));
            collectedItems.put(is.getUnlocalizedName(), prev + is.getCount());
        } else {
            int prev = (collectedItems.get(is.getItem().getUnlocalizedName()) == null ? 0
                    : collectedItems.get(is.getItem().getUnlocalizedName()));
            collectedItems.put(is.getItem().getUnlocalizedName(), prev + is.getCount());
        }
    }

    private int getCollectedItemCount(ItemStack is) {
        boolean variant = getVariant(is);

        if (variant)
            return (collectedItems.get(is.getUnlocalizedName()) == null) ? 0 : collectedItems.get(is.getUnlocalizedName());
        else
            return (collectedItems.get(is.getItem().getUnlocalizedName()) == null) ? 0
                    : collectedItems.get(is.getItem().getUnlocalizedName());
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
