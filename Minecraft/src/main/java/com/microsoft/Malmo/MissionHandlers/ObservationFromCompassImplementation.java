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

/**
 * Creates observations from a compass in the agent's inventory.
 * 
 * @author Cayden Codel, Carnegie Mellon University
 */
public class ObservationFromCompassImplementation extends HandlerBase implements IObservationProducer {
	
	@Override
	public void writeObservationsToJSON(JsonObject json, MissionInit missionInit) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null)
			return;

		InventoryPlayer inventory = player.inventory;

		boolean flag = false;
		for (int i = 0; i < 41; i++)
			if (inventory.getStackInSlot(i).getItem() instanceof ItemCompass)
				flag = true;

		// If there isn't a compass in the player's inventory, can't do anything
		if (!flag)
			json.addProperty("set", false);
		else {
			json.addProperty("set", true);

			// Get world spawn, which is what the compass points at
			BlockPos spawn = player.world.getSpawnPoint();
			json.addProperty("compass-x", spawn.getX());
			json.addProperty("compass-y", spawn.getY());
			json.addProperty("compass-z", spawn.getZ());

			BlockPos playerLoc = player.getPosition();

			json.addProperty("relative-x", spawn.getX() - playerLoc.getX());
			json.addProperty("relative-y", spawn.getY() - playerLoc.getY());
			json.addProperty("relative-z", spawn.getZ() - playerLoc.getZ());

			double dx = (playerLoc.getX() - spawn.getX());
			double dz = (playerLoc.getZ() - spawn.getZ());
			double idealYaw = ((Math.atan2(dz, dx) + Math.PI) * 180.0 / Math.PI);
			double playerYaw = player.rotationYaw;
			double difference = idealYaw - playerYaw;
			
			if (difference < 0)
				difference += 360;
			if (difference > 360)
				difference -= 360;
			
			json.addProperty("offset", difference);
			json.addProperty("normalized-offset", difference - 180);
			json.addProperty("distance", playerLoc.getDistance(spawn.getX(), spawn.getY(), spawn.getZ()));
		}
	}

	@Override
	public void prepare(MissionInit missionInit) {
	}

	@Override
	public void cleanup() {
	}
}
