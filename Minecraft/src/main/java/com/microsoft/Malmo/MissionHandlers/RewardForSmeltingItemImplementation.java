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
import com.microsoft.Malmo.Schemas.RewardForSmeltingItem;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 
 * @author Cayden Codel, Carnegie Mellon University
 * 
 *         Sends a reward when the agent smelts the specified item with
 *         specified amounts.
 */
public class RewardForSmeltingItemImplementation extends RewardForItemBase
		implements IRewardProducer, IMalmoMessageListener {

	private RewardForSmeltingItem params;
	private ArrayList<ItemMatcher> matchers;
	private HashMap<String, Integer> smeltedItems;
	boolean callSmelt = true;

	@SubscribeEvent
	public void onItemSmelt(PlayerEvent.ItemSmeltedEvent event) {
		if (callSmelt)
			checkForMatch(event.smelting);

		callSmelt = !callSmelt;
	}

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
		//System.out.println("Checking for match on " + is.getItem().getUnlocalizedName());
		int savedCrafted = getCraftedItemCount(is);
		//System.out.println("Previous saved amount is " + savedCrafted);
		if (is != null && is.getItem() != null) {
			for (ItemMatcher matcher : this.matchers) {
				if (matcher.matches(is)) {
					if (!params.isSparse()) {
						if (savedCrafted != 0 && savedCrafted < matcher.matchSpec.getAmount()) {
							for (int i = savedCrafted; i < matcher.matchSpec.getAmount()
									&& i - savedCrafted < is.getCount(); i++) {
								//System.out.println("Sparse, had nonzero saved crafted amount, giving a reward of "
								//		+ ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue());
								this.adjustAndDistributeReward(
										((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
										params.getDimension(),
										((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
							}

						} else if (savedCrafted != 0 && savedCrafted >= matcher.matchSpec.getAmount()) {
							// Do nothing
							//System.out
							//		.println("Sparse, had nonzero saved crafted amount, but not enough new for reward");
						} else {
							for (int i = 0; i < is.getCount() && i < matcher.matchSpec.getAmount(); i++) {
								//System.out.println("Had zero saved crafted amount, giving a reward of "
								//		+ ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue());
								this.adjustAndDistributeReward(
										((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
										params.getDimension(),
										((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
							}

						}
					} else {
						if (savedCrafted < matcher.matchSpec.getAmount()
								&& savedCrafted + is.getCount() >= matcher.matchSpec.getAmount()) {
							//System.out.println("Not sparse, giving a reward of "
							//		+ ((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue());
							this.adjustAndDistributeReward(
									((BlockOrItemSpecWithReward) matcher.matchSpec).getReward().floatValue(),
									params.getDimension(),
									((BlockOrItemSpecWithReward) matcher.matchSpec).getDistribution());
						}
					}
				}
			}

			addSmeltedItemCount(is);
		}

	}

	@Override
	public boolean parseParameters(Object params) {
		if (params == null || !(params instanceof RewardForSmeltingItem))
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
		MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_COLLECTITEM);
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
		MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_COLLECTITEM);
	}

	@Override
	public void onMessage(MalmoMessageType messageType, Map<String, String> data) {
	}
}
