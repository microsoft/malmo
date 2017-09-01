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
		{
			this.wantsToQuit = true;
			if (data.containsKey("quitcode"))
			{
			    if (this.quitCode != null && this.quitCode.length() > 0)
			        this.quitCode += "; " + data.get("quitcode");
			    else
			        this.quitCode = data.get("quitcode");
			}
		}
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
