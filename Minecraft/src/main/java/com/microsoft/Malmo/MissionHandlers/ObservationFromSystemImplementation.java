package com.microsoft.Malmo.MissionHandlers;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.TimeHelper;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class ObservationFromSystemImplementation extends ObservationFromServer
{
    TimeHelper.TickRateMonitor renderTickMonitor = new TimeHelper.TickRateMonitor();
    TimeHelper.TickRateMonitor clientTickMonitor = new TimeHelper.TickRateMonitor();
   
    public static class SystemRequestMessage extends ObservationFromServer.ObservationRequestMessage
    {
        public SystemRequestMessage()
        {
        }

        @Override
        void restoreState(ByteBuf buf)
        {
        }

        @Override
        void persistState(ByteBuf buf)
        {
        }
    }

    public static class SystemRequestMessageHandler extends ObservationFromServer.ObservationRequestMessageHandler implements IMessageHandler<SystemRequestMessage, IMessage>
    {
        @Override
        void buildJson(JsonObject json, EntityPlayerMP player, ObservationRequestMessage message)
        {
            try
            {
                json.addProperty("serverTicksPerSecond", MalmoMod.instance.getServerTickRate());
            }
            catch (Exception e)
            {
                System.out.println("Warning: server tick rate not available.");
            }
        }

        @Override
        public IMessage onMessage(SystemRequestMessage message, MessageContext ctx)
        {
            return processMessage(message, ctx);
        }
    }

    @Override
	public ObservationRequestMessage createObservationRequestMessage()
	{
		return new SystemRequestMessage();
	}

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

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev)
    {
        super.onClientTick(ev);
        if (ev.side == Side.CLIENT && ev.phase == Phase.START)
            this.clientTickMonitor.beat();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev)
    {
        if (ev.side == Side.CLIENT && ev.phase == Phase.START)
            this.renderTickMonitor.beat();
    }
    
    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        super.writeObservationsToJSON(json, missionInit);
        json.addProperty("clientTicksPerSecond", this.clientTickMonitor.getEventsPerSecond());
        json.addProperty("renderTicksPerSecond",  this.renderTickMonitor.getEventsPerSecond());
    }
}
