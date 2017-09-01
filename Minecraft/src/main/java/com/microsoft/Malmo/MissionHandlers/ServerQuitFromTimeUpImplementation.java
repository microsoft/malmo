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

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.ServerQuitFromTimeUp;

/** IWantToQuit object that returns true when a certain amount of time has elapsed.<br>
 * This object also draws a cheeky countdown on the Minecraft Chat HUD.
 */

public class ServerQuitFromTimeUpImplementation extends QuitFromTimeUpBase
{
	private String quitCode = "";
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof ServerQuitFromTimeUp))
			return false;
		
		ServerQuitFromTimeUp qtuparams = (ServerQuitFromTimeUp)params;
		setTimeLimitMs(qtuparams.getTimeLimitMs().intValue());
		this.quitCode = qtuparams.getDescription();
		return true;
	}

	@Override
	protected long getWorldTime()
	{
	   	World world = null;
	   	MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		world = server.getEntityWorld();
		return (world != null) ? world.getTotalWorldTime() : 0;
	}
	
	@Override
	protected void drawCountDown(int secondsRemaining)
	{
		Map<String, String> data = new HashMap<String, String>();
		
        String text = TextFormatting.BOLD + "" + secondsRemaining + "...";
        if (secondsRemaining <= 5)
            text = TextFormatting.RED + text;

		data.put("chat", text);
		MalmoMod.safeSendToAll(MalmoMessageType.SERVER_TEXT, data);
	}

	@Override
    public void prepare(MissionInit missionInit) {}

	@Override
    public void cleanup() {}
	
	@Override
	public String getOutcome()
	{
		return this.quitCode;
	}
}