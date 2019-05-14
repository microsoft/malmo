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

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemCompass;
import net.minecraft.util.math.BlockPos;
import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemCompass;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.NonNullList;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.util.ResourceLocation;


/**
 * Creates observations from a compass in the agent's inventory.
 * 
 * @author Cayden Codel, Carnegie Mellon University
 */
public class ObservationFromCompassImplementation extends HandlerBase implements IObservationProducer {

	@Override
	public void writeObservationsToJSON(JsonObject compassJson, MissionInit missionInit) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null)
			return;

		Minecraft mc = Minecraft.getMinecraft();
		ItemStack compassStack = null;
		boolean hasCompass = false, hasHotbarCompass = false, hasMainHandCompass = false, hasOffHandCompass = false;

		// If player has a compass use that one (there is randomness in compass needle)

		// Offhand compass
		for (ItemStack itemStack : mc.player.inventory.offHandInventory) {
			if (itemStack.getItem() instanceof ItemCompass) {
				compassStack = itemStack;
				hasCompass = true;
				hasOffHandCompass = true;
				break;
			}
		}
		// Main Inventory compass ( overrides offhand compass iff player is holding main hand compass)
		int invSlot = 0;
		for (ItemStack itemStack : mc.player.inventory.mainInventory) {
			if (itemStack.getItem() instanceof ItemCompass) {
				compassStack = compassStack == null ? itemStack : compassStack;
				hasCompass = true;
				if (invSlot < InventoryPlayer.getHotbarSize()) {
					hasHotbarCompass = true;
				}
				if (invSlot == mc.player.inventory.currentItem) {
					hasMainHandCompass = true;
					compassStack = itemStack;
				}
				invSlot += 1;
			}
		}
		if (!hasCompass) {
			compassStack = new ItemStack(new ItemCompass());
		}

		IItemPropertyGetter angleGetter = compassStack.getItem().getPropertyGetter(new ResourceLocation("angle"));
		float angle = angleGetter.apply(compassStack, mc.world, mc.player);
		angle = ((angle*360 + 180) % 360) - 180;

		compassJson.addProperty("compassAngle", angle); // Current compass angle [-180 - 180]
		compassJson.addProperty("hasCompass", hasCompass); // Player has compass in main inv or offhand
		compassJson.addProperty("hasHotbarCompass", hasHotbarCompass); // Player has compass in HOTBAR
		compassJson.addProperty("hasActiveCompass", hasMainHandCompass || hasOffHandCompass); // Player is holding a
																							  // visible compass
		compassJson.addProperty("hasMainHandCompass", hasMainHandCompass); // Player is holding a compass
		compassJson.addProperty("hasOffHandCompass", hasOffHandCompass); // Player is holding an offhand compass

		BlockPos spawn = mc.player.world.getSpawnPoint(); // Add distance observation in blocks (not vanilla!)
		compassJson.addProperty("distanceToCompassTarget",
				mc.player.getPosition().getDistance(spawn.getX(), spawn.getY(), spawn.getZ()));
	}

	@Override
	public void prepare(MissionInit missionInit) {
	}

	@Override
	public void cleanup() {
	}
}
