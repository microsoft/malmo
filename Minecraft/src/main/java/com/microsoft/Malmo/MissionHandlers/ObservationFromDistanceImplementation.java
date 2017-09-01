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
			EntityPlayerSP player = Minecraft.getMinecraft().player;
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