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

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import com.microsoft.Malmo.Schemas.AgentQuitFromTimeUp;
import com.microsoft.Malmo.Schemas.MissionInit;

/** IWantToQuit object that returns true when a certain amount of time has elapsed.<br>
 * This object also draws a cheeky countdown on the Minecraft Chat HUD.
 */

public class AgentQuitFromTimeUpImplementation extends QuitFromTimeUpBase
{
	private String quitCode = "";
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof AgentQuitFromTimeUp))
			return false;
		
		AgentQuitFromTimeUp qtuparams = (AgentQuitFromTimeUp)params;
		setTimeLimitMs(qtuparams.getTimeLimitMs().intValue());
		this.quitCode = qtuparams.getDescription();
		return true;
	}

	@Override
	protected long getWorldTime()
	{
		return Minecraft.getMinecraft().world.getTotalWorldTime();
	}

	@Override
	protected void drawCountDown(int secondsRemaining)
	{
        TextComponentString text = new TextComponentString("" + secondsRemaining + "...");
        Style style = new Style();
        style.setBold(true);
        if (secondsRemaining <= 5)
            style.setColor(TextFormatting.RED);

        text.setStyle(style);
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(text, 1);
	}
	
	@Override
    public void prepare(MissionInit missionInit) {}

	@Override
    public void cleanup() {}
	
	@Override
	public String getOutcome() { return this.quitCode; }
}