package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.MissionHandlers.RewardForCollectingItemImplementation.GainItemEvent;
import com.microsoft.Malmo.Schemas.AgentQuitFromCollectingItem;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithDescription;
import com.microsoft.Malmo.Schemas.MissionInit;

public class AgentQuitFromCollectingItemImplementation extends HandlerBase implements IWantToQuit
{
    AgentQuitFromCollectingItem params;
    List<ItemQuitMatcher> matchers;
    String quitCode = "";
    boolean wantToQuit = false;

    public static class ItemQuitMatcher extends RewardForItemBase.ItemMatcher
    {
        String description;

        ItemQuitMatcher(BlockOrItemSpecWithDescription spec)
        {
            super(spec);
            this.description = spec.getDescription();
        }

        String description()
        {
            return this.description;
        }
    }

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof AgentQuitFromCollectingItem))
            return false;
        
        this.params = (AgentQuitFromCollectingItem)params;
        this.matchers = new ArrayList<ItemQuitMatcher>();
        for (BlockOrItemSpecWithDescription bs : this.params.getItem())
            this.matchers.add(new ItemQuitMatcher(bs));
        return true;
    }

    @Override
    public boolean doIWantToQuit(MissionInit missionInit)
    {
        return this.wantToQuit;
    }

    @Override
    public String getOutcome()
    {
        return this.quitCode;
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void cleanup()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onGainItem(GainItemEvent event)
    {
        checkForMatch(event.stack);
    }

    @SubscribeEvent
    public void onPickupItem(EntityItemPickupEvent event)
    {
        if (event.getItem() != null && event.getItem().getEntityItem() != null)
        {
            ItemStack stack = event.getItem().getEntityItem();
            checkForMatch(stack);
        }
    }

    private void checkForMatch(ItemStack is)
    {
        if (is != null)
        {
            for (ItemQuitMatcher matcher : this.matchers)
            {
                if (matcher.matches(is))
                {
                    this.quitCode = matcher.description();
                    this.wantToQuit = true;
                }
            }
        }
    }
}
