package com.microsoft.Malmo.MissionHandlers;

import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.Pos;
import com.microsoft.Malmo.Schemas.RewardForReachingPosition;
import com.microsoft.Malmo.Schemas.PointWithReward;
import com.microsoft.Malmo.Utils.PositionHelper;

/** Simple IRewardProducer object that returns a large reward when the player gets to within a certain tolerance of a goal position.<br>
 */
public class RewardForReachingPositionImplementation extends HandlerBase implements IRewardProducer
{
	Pos targetPos;
	float reward;
	float tolerance;
	boolean oneShot;
	boolean fired = false;
	List<PointWithReward> rewardPoints;
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof RewardForReachingPosition))
			return false;
		
		RewardForReachingPosition rrpparams = (RewardForReachingPosition)params;
		this.rewardPoints = rrpparams.getMarker();
		return true;
	}

    @Override
    public float getReward(MissionInit missionInit)
    {
        if (missionInit == null || Minecraft.getMinecraft().thePlayer == null)
            return 0;

        float rewardTotal = 0;
        if (this.rewardPoints != null)
        {
        	Iterator<PointWithReward> goalIterator = this.rewardPoints.iterator();
        	while (goalIterator.hasNext())
        	{
        		PointWithReward goal = goalIterator.next();
        		boolean oneShot = goal.isOneshot();
        		float reward = goal.getReward().floatValue();
        		float tolerance = goal.getTolerance().floatValue();

                float distance = PositionHelper.calcDistanceFromPlayerToPosition(Minecraft.getMinecraft().thePlayer, goal);
                if (distance <= tolerance)
                {
                    rewardTotal += reward;
                    if (oneShot)
                    	goalIterator.remove();	// Safe to do this via an iterator.
                }
            }
        }

        return rewardTotal;
    }
    
	@Override
	public void prepare(MissionInit missionInit) {}

	@Override
    public void cleanup() {}
}
