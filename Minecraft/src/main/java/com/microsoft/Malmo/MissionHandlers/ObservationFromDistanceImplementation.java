package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.NamedPoint;
import com.microsoft.Malmo.Schemas.ObservationFromDistance;
import com.microsoft.Malmo.Utils.PositionHelper;

/** Simple IObservationProducer class that creates a single "distanceFromTarget" observation, using the target specified in the MissionInit XML.<br>
 * (Yes, we know this is cheating for RL, but it's a useful test example.)
 */
public class ObservationFromDistanceImplementation extends HandlerBase implements IObservationProducer
{
	private ObservationFromDistance odparams;
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof ObservationFromDistance))
			return false;
		
		this.odparams = (ObservationFromDistance)params;
		return true;
	}

	@Override
	public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
	{
		for (NamedPoint marker : odparams.getMarker())
		{
			EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
			json.addProperty("distanceFrom" + makeSafe(marker.getName()), PositionHelper.calcDistanceFromPlayerToPosition(player, marker));
    	}
	}

	private String makeSafe(String raw)
	{
		// Hopefully the string won't be too crazy, since the XSD:Name type will disallow bonkers characters.
		// Just trim any whitespace.
		return raw.trim();
	}
	@Override
	public void prepare(MissionInit missionInit) {}

	@Override
	public void cleanup() {}
}