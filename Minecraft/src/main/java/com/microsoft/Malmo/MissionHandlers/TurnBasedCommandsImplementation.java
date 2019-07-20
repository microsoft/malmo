package com.microsoft.Malmo.MissionHandlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.IMalmoMessageListener;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.TurnBasedCommands;
import com.microsoft.Malmo.Utils.SeedHelper;

public class TurnBasedCommandsImplementation extends CommandGroup implements IMalmoMessageListener
{
    private ObservationFromTurnSchedulerImplementation observationProducer;
    private int requestedPosition;
    private Random rng;
    
    String agentName;

    public TurnBasedCommandsImplementation(){
        super();
        if (rng == null)
            rng = SeedHelper.getRandom();
    }

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
            
        if (params instanceof TurnBasedCommands)
        {
            TurnBasedCommands tbcparams = (TurnBasedCommands)params;
            this.requestedPosition = tbcparams.getRequestedPosition().intValue();
            // Create our partner observation producer, and manually add it to the mission behaviour:
            this.observationProducer = new ObservationFromTurnSchedulerImplementation();
            // this.parentBehaviour().addObservationProducer(this.observationProducer);
            // Now instantiate our child handlers. We can use a new MissionBehaviour object to take care of this.
            List<Object> handlers = tbcparams.getTurnBasedApplicableCommandHandlers();
            if (!handlers.isEmpty())
            {
                MissionBehaviour subHandlers = new MissionBehaviour();
                subHandlers.addExtraHandlers(handlers);
                if (subHandlers.commandHandler != null)
                {
                    if (subHandlers.commandHandler instanceof HandlerBase)
                        ((HandlerBase)subHandlers.commandHandler).setParentBehaviour(this.parentBehaviour());
                    this.addCommandHandler(subHandlers.commandHandler);
                }
            }
        }
        return true;
    }

    @Override
    public void install(MissionInit missionInit)
    {
        super.install(missionInit);
        this.parentBehaviour().addObservationProducer(this.observationProducer);
        this.agentName = missionInit.getMission().getAgentSection().get(missionInit.getClientRole()).getName();
        MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_YOUR_TURN);
    }

    @Override
    public void deinstall(MissionInit missionInit)
    {
        super.deinstall(missionInit);
        MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_YOUR_TURN);
    }

    @Override
    public boolean isOverriding()
    {
        return super.isOverriding();
    }

    @Override
    public void setOverriding(boolean b)
    {
        super.setOverriding(b);
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        // Firstly, reject any command that doesn't match the single-use turn key provided to the agent.
        if (!this.observationProducer.matchesKey(verb))
        {
            // Incorrect key. For the moment, we just let this command disappear.
            // In the future, we should return an error to the agent.
            return false;
        }
        // Now pass the command on to our sub-handlers.
        // The key will have been stripped off as the first parameter;
        // we assume the parameter string consists of the *real* command, plus parameters.
        // CommandGroup.onExecute rejoins the verb and parameter before passing to the children,
        // so we can pass an empty verb, and the right thing will happen.
        boolean processed = super.onExecute(parameter, "", missionInit);
        if (processed)
        {
            // We have taken our turn:
            this.observationProducer.turnUsed();
            // Let the server know that we need to be rescheduled:
            Map<String, String> data = new HashMap<String, String>();
            data.put("agentname", this.agentName);
            MalmoMod.network.sendToServer(new MalmoMod.MalmoMessage(MalmoMessageType.CLIENT_TURN_TAKEN, 0, data));
        }
        return processed;
    }

    @Override
    public void onMessage(MalmoMessageType messageType, Map<String, String> data)
    {
        if (messageType == MalmoMessageType.SERVER_YOUR_TURN)
        {
            // It's our go.
            // Update our observation producer accordingly.
            String newkey = generateKey();
            this.observationProducer.setKeyAndIncrement(newkey);
        }
    }

    public String generateKey()
    {
        // A whole GUID is unwieldy and costly - do something small and simple.
        String letters = "abcdefghijklmnopqrstuvwxyz";
        String key = "";
        for (int i = 0; i < 5; i ++)
        {
            int pos = this.rng.nextInt(26);
            key += letters.subSequence(pos, pos + 1);
        }
        return key;
    }

    @Override
    public void appendExtraServerInformation(HashMap<String, String> map)
    {
        super.appendExtraServerInformation(map);
        // Tell the server that we want to be part of the turn schedule.
        map.put("turnPosition", String.valueOf(this.requestedPosition));
    }
}
