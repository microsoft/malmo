package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

/** ObservationProducer that returns a JSON array of all the commands acted on since the last observation message.<br>
 * Note that the commands returned might not yet have taken effect, depending on the command and the way in which Minecraft responds to it -
 * but they will have been processed by the command handling chain.
 */
public class ObservationFromRecentCommandsImplementation extends HandlerBase implements IObservationProducer
{
    private boolean hookedIntoCommandChain = false;
    private List<String> recentCommandList = new ArrayList<String>();

	@Override
	public void prepare(MissionInit missionInit) {}

	@Override
	public void cleanup() {}

    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        if (!hookedIntoCommandChain)
        {
            // We need to see the commands as they come in, so we can determine which ones to echo back in the observation message.
            // To do this we create our own command handler and insert it at the root of the command chain.
            // It's slightly dirty behaviour, but it saves
            //      a) adding special code into ProjectMalmo.java just to allow for this ObservationProducer to work, and
            //      b) requiring the user to add a special command handler themselves at the right point in the XML.
            MissionBehaviour mb = parentBehaviour();
            ICommandHandler oldch = mb.commandHandler;
            CommandGroup newch = new CommandGroup() {
                protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
                {
                    // See if this command gets handled by the legitimate handlers:
                    boolean handled = super.onExecute(verb, parameter, missionInit);
                    if (handled)    // Yes, so record it:
                        ObservationFromRecentCommandsImplementation.this.addHandledCommand(verb, parameter);
                    return handled;
                }
            };
            newch.setOverriding((oldch != null) ? oldch.isOverriding() : true);
            if (oldch != null)
                newch.addCommandHandler(oldch);
            mb.commandHandler = newch;
            this.hookedIntoCommandChain = true;
        }
        synchronized(this.recentCommandList)
        {
            // Have any commands been processed since we last sent a burst of observations?
            if (this.recentCommandList.size() != 0)
            {
                // Yes, so build up a JSON array:
                JsonArray commands = new JsonArray();
                for (String s : this.recentCommandList)
                {
                    commands.add(new JsonPrimitive(s));
                }
                json.add("CommandsSinceLastObservation", commands);
            }
            this.recentCommandList.clear();
        }
    }
    
    protected void addHandledCommand(String verb, String parameter)
    {
        // Must synchronise because command handling might happen on a different thread to observation sending.
        synchronized(this.recentCommandList)
        {
            this.recentCommandList.add(verb + " " + parameter);
        }
    }
}
