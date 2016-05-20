package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.AgentQuitFromReachingPosition;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.PointWithToleranceAndDescription;
import com.microsoft.Malmo.Utils.PositionHelper;

/** Simple IWantToQuit object that returns true when the player gets to within a certain tolerance of a goal position.<br>
 * The tolerance and target position are currently specified in the MissionInit's Mission's Goal object.
 * At some point this may not exist anymore, in which case we'll need to think about how best to parameterise this object.
 */
public class AgentQuitFromReachingPositionImplementation extends HandlerBase implements IWantToQuit
{
	AgentQuitFromReachingPosition qrpparams;
	String quitCode = "";
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof AgentQuitFromReachingPosition))
			return false;
		
		this.qrpparams = (AgentQuitFromReachingPosition)params;
		return true;
	}

    @Override
	public boolean doIWantToQuit(MissionInit missionInit)
	{
		if (missionInit == null || this.qrpparams == null)
			return false;

		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		for (PointWithToleranceAndDescription goal : this.qrpparams.getMarker())
		{
			float distance = PositionHelper.calcDistanceFromPlayerToPosition(player, goal);
            if (distance <= goal.getTolerance().floatValue())
            {
            	this.quitCode = goal.getDescription();
            	return true;
            }
		}
		return false;
	}

	@Override
    public void prepare(MissionInit missionInit) {}

	@Override
    public void cleanup() {}
	
	@Override
	public String getOutcome() { return this.quitCode; }
}