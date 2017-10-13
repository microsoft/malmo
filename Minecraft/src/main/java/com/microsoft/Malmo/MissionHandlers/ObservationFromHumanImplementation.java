package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.Malmo.Client.MalmoModClient;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

public class ObservationFromHumanImplementation extends HandlerBase implements IObservationProducer
{
    private abstract class ObservationEvent
    {
        public abstract JsonObject getJSON();
    }

    private class MouseObservationEvent extends ObservationEvent
    {
        private int deltaX;
        private int deltaY;

        public MouseObservationEvent(int deltaX, int deltaY)
        {
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }

        @Override
        public JsonObject getJSON()
        {
            JsonObject jsonEvent = new JsonObject();
            jsonEvent.addProperty("type", "mouse");
            jsonEvent.addProperty("deltaX", this.deltaX);
            jsonEvent.addProperty("deltaY", this.deltaY);
            return jsonEvent;
        }
    }

    private class MouseObserver implements MalmoModClient.MouseEventListener
    {
        @Override
        public void onXYChange(int deltaX, int deltaY)
        {
            queueEvent(new MouseObservationEvent(deltaX, deltaY));
        }
    }

    MouseObserver observer = new MouseObserver();
    List<ObservationEvent> events = new ArrayList<ObservationEvent>();

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
    }
}
