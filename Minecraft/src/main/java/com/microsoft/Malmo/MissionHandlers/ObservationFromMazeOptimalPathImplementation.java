package com.microsoft.Malmo.MissionHandlers;

import io.netty.buffer.ByteBuf;

import java.util.Hashtable;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Schemas.MissionInit;

public class ObservationFromMazeOptimalPathImplementation extends ObservationFromServer
{
	// TODO - maze was created by Server so subgoals are stored in the server properties... make this a server-side observation producer...
    private int subgoalIndex = 0;
    
	@Override
	public void prepare(MissionInit missionInit)
	{
		super.prepare(missionInit);
	}

	@Override
	public void cleanup()
	{
		super.cleanup();
	}

	@Override
	public ObservationRequestMessage createObservationRequestMessage()
	{
		return new OptimalPathRequestMessage(this.subgoalIndex);
	}
	
	@Override
    protected void onReturnedData(Map<String, String> data)
    {
    	String sgi = data.get("subgoalIndex");
    	if (sgi != null)
    	{
    		Integer i = Integer.valueOf(sgi);
    		if (i != null)
    			this.subgoalIndex = i;
    	}
	}

	public static class OptimalPathRequestMessage extends ObservationRequestMessage
	{
		private int subgoalIndex = 0;
		
		public OptimalPathRequestMessage()
		{
		}

		public OptimalPathRequestMessage(int sgi)
		{
			this.subgoalIndex = sgi;
		}
				
		@Override
		void restoreState(ByteBuf buf)
		{
			this.subgoalIndex = buf.readInt();
		}

		@Override
		void persistState(ByteBuf buf)
		{
			buf.writeInt(this.subgoalIndex);
		}
		
		@Override
		public void addReturnData(Map<String, String> returnData)
		{
			if (returnData != null)
				returnData.put("subgoalIndex", String.valueOf(this.subgoalIndex));
		}
	}

	public static class OptimalPathRequestMessageHandler extends ObservationRequestMessageHandler implements IMessageHandler<OptimalPathRequestMessage, IMessage>
	{
		public OptimalPathRequestMessageHandler()
		{
			
		}
		@Override
		void buildJson(JsonObject json, EntityPlayerMP player, ObservationRequestMessage message, MessageContext ctx)
		{
			if (!(message instanceof OptimalPathRequestMessage))
				return;
			
			OptimalPathRequestMessage oprm = (OptimalPathRequestMessage)message;
	    	Hashtable<String, Object> properties = null;
			try
			{
				properties = MalmoMod.getPropertiesForCurrentThread();
			}
			catch (Exception e)
			{
				// Can't get the properties - no data.
				return;
			}
			
	        double[] xTargets = (double[])properties.get("OptPathXCoords");
	        double[] zTargets = (double[])properties.get("OptPathZCoords");
	        if (xTargets == null || zTargets == null)
	            return; // No data.

	        int numTargets = Math.min(xTargets.length,  zTargets.length);   // Should be no difference between them but check anyway.
	        if (oprm.subgoalIndex >= numTargets)
	            return; // Finished.
	        
	        double targetx = xTargets[oprm.subgoalIndex];
	        double targetz = zTargets[oprm.subgoalIndex];
	        double sourcex = player.posX;
	        double sourcez = player.posZ;

	        if (Math.abs(targetx-sourcex) + Math.abs(targetz-sourcez) < 1)
	        	oprm.subgoalIndex++;
	        
	        if (oprm.subgoalIndex >= numTargets)
	            return; // Finished.

	        // Calculate which way we need to turn in order to point towards the target:
	        double dx = (targetx - sourcex);
	        double dz = (targetz - sourcez);
	        double targetYaw = (Math.atan2(dz, dx) * 180.0/Math.PI) - 90;
	        // System.out.println("I:" + oprm.subgoalIndex + "; D:(" + (targetx-sourcex) + "," + (targetz-sourcez) + "); Y:" + targetYaw);
	        double sourceYaw = player.rotationYaw;
	        // Find shortest angular distance between the two yaws, preserving sign:
	        double difference = targetYaw - sourceYaw;
	        while (difference < -180)
	            difference += 360;
	        while (difference > 180)
	            difference -= 360;
	        // Normalise:
	        difference /= 180.0;
	        json.addProperty("yawDelta",  difference);
	    }

		@Override
		public IMessage onMessage(OptimalPathRequestMessage message, MessageContext ctx)
		{
			processMessage(message, ctx);
			return null;
		}
	}
}
