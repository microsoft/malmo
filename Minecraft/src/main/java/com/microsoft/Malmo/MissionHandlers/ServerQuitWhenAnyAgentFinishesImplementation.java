package com.microsoft.Malmo.MissionHandlers;

import java.util.Map;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.MalmoMod.IMalmoMessageListener;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.ServerQuitWhenAnyAgentFinishes;

/** Simple quit handler for server that signals a quit as soon as any agent has finished their mission.*/
public class ServerQuitWhenAnyAgentFinishesImplementation extends HandlerBase implements IWantToQuit, IMalmoMessageListener
{
	boolean wantsToQuit = false;
	String quitCode = "";
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof ServerQuitWhenAnyAgentFinishes))
			return false;
		
		ServerQuitWhenAnyAgentFinishes sqafparams = (ServerQuitWhenAnyAgentFinishes)params;
		this.quitCode = sqafparams.getDescription();
		return true;
	}

	@Override
	public boolean doIWantToQuit(MissionInit missionInit)
	{
		return this.wantsToQuit;
	}

	@Override
	public void onMessage(MalmoMessageType messageType, Map<String, String> data)
	{
		if (messageType == MalmoMessageType.CLIENT_AGENTFINISHEDMISSION)
			this.wantsToQuit = true;
	}
	
	@Override
    public void prepare(MissionInit missionInit)
	{
		MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.CLIENT_AGENTFINISHEDMISSION);
	}

	@Override
    public void cleanup()
	{
		MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.CLIENT_AGENTFINISHEDMISSION);
	}
	
	@Override
	public String getOutcome()
	{
		return this.quitCode;
	}
}
