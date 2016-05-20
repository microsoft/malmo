package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

public class ObservationFromDiscreteCellImplementation extends HandlerBase implements IObservationProducer
{
	@Override
	public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
	{
		// Return a string that is unique for every cell on the x/z plane (ignores y)
		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		// getPosition() rounds to int.
		int x = player.getPosition().getX();
		int z = player.getPosition().getZ();
		json.addProperty("cell", "(" + x + "," + z + ")");
	}

	@Override
	public void prepare(MissionInit missionInit) {}

	@Override
	public void cleanup() {}

}
