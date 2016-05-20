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
		if (missionInit == null || missionInit.getMission() == null || Minecraft.getMinecraft().theWorld == null)
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