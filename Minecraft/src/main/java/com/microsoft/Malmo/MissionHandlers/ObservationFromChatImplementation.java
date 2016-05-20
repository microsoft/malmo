package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ObservationFromChatImplementation extends HandlerBase implements IObservationProducer 
{
	private ArrayList<String> chatMessagesReceived = new ArrayList<String>();

	@Override
	public void writeObservationsToJSON(JsonObject json, MissionInit missionInit) 
	{
		JsonArray arr = new JsonArray();
		for( String obs : this.chatMessagesReceived )
		{
			arr.add(new JsonPrimitive(obs) );
		}
		json.add( "Chat", arr );
		this.chatMessagesReceived.clear();
	}

	@Override
	public void prepare(MissionInit missionInit) 
	{
        MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public void cleanup() 
	{
        MinecraftForge.EVENT_BUS.unregister(this);
	}

    @SubscribeEvent
    public void onEvent(ClientChatReceivedEvent event)
    {
    	this.chatMessagesReceived.add( event.message.getUnformattedText() );
    }
}
