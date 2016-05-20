package com.microsoft.Malmo.MissionHandlers;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.Utils.JSONWorldDataHelper;

/** Simple IObservationProducer object that pings out a whole bunch of data.<br>
 */
public class ObservationFromFullStatsImplementation extends ObservationFromServer
{
    public static class FullStatsRequestMessage extends ObservationFromServer.ObservationRequestMessage
    {
		@Override
		void restoreState(ByteBuf buf)
		{
			// Nothing to do - no context needed.
		}

		@Override
		void persistState(ByteBuf buf)
		{
			// Nothing to do - no context needed.
		}
    }
    
    public static class FullStatsRequestMessageHandler extends ObservationFromServer.ObservationRequestMessageHandler implements IMessageHandler<FullStatsRequestMessage, IMessage>
	{
		@Override
		void buildJson(JsonObject json, EntityPlayerMP player, ObservationRequestMessage message, MessageContext ctx)
		{
			JSONWorldDataHelper.buildAchievementStats(json, player);
			JSONWorldDataHelper.buildLifeStats(json, player);
			JSONWorldDataHelper.buildPositionStats(json, player);
		}

		@Override
		public IMessage onMessage(FullStatsRequestMessage message, MessageContext ctx)
		{
			return processMessage(message, ctx);
		}
    }

	@Override
	public ObservationRequestMessage createObservationRequestMessage()
	{
		return new FullStatsRequestMessage();
	}
}
