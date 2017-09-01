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

import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.Pos;
import com.microsoft.Malmo.Schemas.RewardForReachingPosition;
import com.microsoft.Malmo.Schemas.PointWithReward;
import com.microsoft.Malmo.Utils.PositionHelper;

/**
 * Simple IRewardProducer object that returns a large reward when the player
 * gets to within a certain tolerance of a goal position.<br>
 */
public class RewardForReachingPositionImplementation extends RewardBase implements IRewardProducer {
    Pos targetPos;
    float reward;
    float tolerance;
    boolean oneShot;
    boolean fired = false;
    List<PointWithReward> rewardPoints;
    private RewardForReachingPosition params;

    @Override
    public boolean parseParameters(Object params) {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForReachingPosition))
            return false;

        this.params = (RewardForReachingPosition) params;
        this.rewardPoints = this.params.getMarker();
        return true;
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward) {
        super.getReward(missionInit, reward);
        if (missionInit == null || Minecraft.getMinecraft().player == null)
            return;

        if (this.rewardPoints != null) {
            Iterator<PointWithReward> goalIterator = this.rewardPoints.iterator();
            while (goalIterator.hasNext()) {
                PointWithReward goal = goalIterator.next();
                boolean oneShot = goal.isOneshot();
                float reward_value = goal.getReward().floatValue();
                float tolerance = goal.getTolerance().floatValue();

                float distance = PositionHelper.calcDistanceFromPlayerToPosition(Minecraft.getMinecraft().player, goal);
                if (distance <= tolerance)
                {
                    float adjusted_reward = adjustAndDistributeReward(reward_value, this.params.getDimension(), goal.getDistribution());
                    reward.add(this.params.getDimension(), adjusted_reward);
                    if (oneShot)
                        goalIterator.remove(); // Safe to do this via an iterator.
                }
            }
        }
    }

    @Override
    public void prepare(MissionInit missionInit) {
        super.prepare(missionInit);
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }
}
