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

import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.TimeHelper;

/** IWantToQuit object that returns true when a certain amount of time has elapsed.<br>
 * This object also draws a cheeky countdown on the Minecraft Chat HUD.
 */

public abstract class QuitFromTimeUpBase extends HandlerBase implements IWantToQuit
{
    private long initialWorldTime = 0;
	private float timelimitms;
	private int countdownSeconds;

	abstract protected long getWorldTime();
	abstract protected void drawCountDown(int secondsRemaining);
	
	protected void setTimeLimitMs(float limit)
	{
		this.timelimitms = limit;
	}

	@Override
	public boolean doIWantToQuit(MissionInit missionInit)
	{
		if (missionInit == null || missionInit.getMission() == null || Minecraft.getMinecraft().world == null)
			return false;

		// Initialise our start-of-mission time:
		if (this.initialWorldTime == 0)
			this.initialWorldTime = getWorldTime();

		long currentWorldTime = getWorldTime();
		long timeElapsedInWorldTicks = currentWorldTime - this.initialWorldTime;
		float timeRemainingInMs = this.timelimitms - (timeElapsedInWorldTicks * TimeHelper.MillisecondsPerWorldTick);
		int timeRemainingInSeconds = (int)Math.ceil(timeRemainingInMs / TimeHelper.MillisecondsPerSecond);
	    if (timeRemainingInSeconds != this.countdownSeconds)
	    {
	        this.countdownSeconds = timeRemainingInSeconds;
	        drawCountDown(this.countdownSeconds);
	    }
	    if (timeRemainingInMs <= 0)
	    	return true;
		return false;
	}
}