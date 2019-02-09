package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.microsoft.Malmo.Schemas.AgentQuitFromPossessingItem;
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

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Quits the mission when the agent has possessed the right amount of items. The count on the item collection is non-absolute.
 * <p>
 * In order to quit the mission, the agent must have the requisite items in its inventory all at one time.
 */
public class AgentQuitFromPossessingItemImplementation extends HandlerBase implements IWantToQuit {

	private AgentQuitFromPossessingItem params;
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
		collectedItems = new HashMap<String, Integer>();
	}

	@Override
	public void cleanup() {
		MinecraftForge.EVENT_BUS.unregister(this);
	}

	@SubscribeEvent
	public void onGainItem(GainItemEvent event) {
		checkForMatch(event.stack);
	}

	@SubscribeEvent
	public void onPickupItem(EntityItemPickupEvent event) {
		if (event.getItem() != null)
			checkForMatch(event.getItem().getEntityItem());
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