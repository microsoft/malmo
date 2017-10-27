package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.Malmo.Client.MalmoModClient;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.MissionHandlers.CommandForKey.KeyEventListener;
import com.microsoft.Malmo.Schemas.MissionInit;

public class ObservationFromHumanImplementation extends HandlerBase implements IObservationProducer
{
    private abstract class ObservationEvent
    {
    	public long timestamp = 0;
        public abstract JsonObject getJSON();
        
        ObservationEvent()
        {
        	this.timestamp = Minecraft.getMinecraft().world.getWorldTime();
        }
    }

    private class MouseObservationEvent extends ObservationEvent
    {
        private int deltaX;
        private int deltaY;
        private int deltaZ;

        public MouseObservationEvent(int deltaX, int deltaY, int deltaZ)
        {
            super();
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.deltaZ = deltaZ;
        }

        @Override
        public JsonObject getJSON()
        {
            JsonObject jsonEvent = new JsonObject();
            jsonEvent.addProperty("time", this.timestamp);
            jsonEvent.addProperty("type", "mouse");
            jsonEvent.addProperty("deltaX", this.deltaX);
            jsonEvent.addProperty("deltaY", this.deltaY);
            jsonEvent.addProperty("deltaZ", this.deltaZ);
            return jsonEvent;
        }
    }
    
    private class KeyObservationEvent extends ObservationEvent
    {
        private String commandString;
        private boolean pressed;

        KeyObservationEvent(String commandString, boolean pressed)
        {
            super();
            this.commandString = commandString;
            this.pressed = pressed;
        }

        @Override
        public JsonObject getJSON()
        {
            JsonObject jsonEvent = new JsonObject();
            jsonEvent.addProperty("time", this.timestamp);
            jsonEvent.addProperty("type", "key");
            jsonEvent.addProperty("command", this.commandString);
            jsonEvent.addProperty("pressed", this.pressed);
            return jsonEvent;
        }
    }

    private class MouseObserver implements MalmoModClient.MouseEventListener, KeyEventListener
    {
        @Override
        public void onXYZChange(int deltaX, int deltaY, int deltaZ)
        {
            System.out.println("Mouse observed: " + deltaX + ", " + deltaY + ", " + deltaZ);
            if (deltaX != 0 || deltaY != 0 || deltaZ != 0)
                queueEvent(new MouseObservationEvent(deltaX, deltaY, deltaZ));
        }

        @Override
        public void onKeyChange(String commandString, boolean pressed)
        {
            queueEvent(new KeyObservationEvent(commandString, pressed));
        }
    }

    MouseObserver observer = new MouseObserver();
    List<ObservationEvent> events = new ArrayList<ObservationEvent>();
    List<CommandForKey> keys = null;

    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        synchronized(this.events)
        {
            if (this.events.size() > 0)
            {
                JsonArray jsonEvents = new JsonArray();
                for (ObservationEvent event : this.events)
                    jsonEvents.add(event.getJSON());
                this.events.clear();
                json.add("events", jsonEvents);
            }
        }
    }

    public void queueEvent(ObservationEvent event)
    {
        synchronized(this.events)
        {
            this.events.add(event);
        }
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        MouseHelper mhelp = Minecraft.getMinecraft().mouseHelper;
        if (!(mhelp instanceof MalmoModClient.MouseHook))
        {
            System.out.println("ERROR! MouseHook not installed - Malmo won't work correctly.");
            return;
        }
        ((MalmoModClient.MouseHook)mhelp).requestEvents(this.observer);
        this.keys = HumanLevelCommandsImplementation.getKeyOverrides();
        for (CommandForKey k : this.keys)
        {
            k.install(missionInit);
            k.setKeyEventObserver(this.observer);
        }
    }

    @Override
    public void cleanup()
    {
        MouseHelper mhelp = Minecraft.getMinecraft().mouseHelper;
        if (!(mhelp instanceof MalmoModClient.MouseHook))
        {
            System.out.println("ERROR! MouseHook not installed - Malmo won't work correctly.");
            return;
        }
        ((MalmoModClient.MouseHook)mhelp).requestEvents(null);
        for (CommandForKey k : this.keys)
        {
            k.setKeyEventObserver(null);
        }
    }
}
