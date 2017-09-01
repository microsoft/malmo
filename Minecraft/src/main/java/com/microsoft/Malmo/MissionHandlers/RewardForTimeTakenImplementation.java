package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForTimeTaken;

public class RewardForTimeTakenImplementation extends RewardBase
{
    RewardForTimeTaken params;
    float totalReward;

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForTimeTaken))
            return false;

        this.params = (RewardForTimeTaken)params;
        this.totalReward = this.params.getInitialReward().floatValue();
        return true;
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward)
    {
        boolean sendReward = false;
        switch (this.params.getDensity())
        {
        case MISSION_END:
            this.totalReward += this.params.getDelta().floatValue();
            sendReward = reward.isFinalReward();
            break;
        case PER_TICK:
            this.totalReward = this.params.getDelta().floatValue();
            sendReward = true;
            break;
        case PER_TICK_ACCUMULATED:
            this.totalReward += this.params.getDelta().floatValue();
            sendReward = true;
            break;
        default:
            break;
        }

        super.getReward(missionInit, reward);
        if (sendReward)
        {
            float adjusted_reward = adjustAndDistributeReward(this.totalReward, this.params.getDimension(), this.params.getRewardDistribution());
            reward.add(this.params.getDimension(), adjusted_reward);
        }
    }
}
