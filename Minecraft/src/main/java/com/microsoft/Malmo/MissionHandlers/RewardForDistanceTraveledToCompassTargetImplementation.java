package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForDistanceTraveledToCompassTarget;


import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;

public class RewardForDistanceTraveledToCompassTargetImplementation extends RewardBase
{
    RewardForDistanceTraveledToCompassTarget params;
    double previousDistance;
    float totalReward;
    boolean positionInitialized;

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForDistanceTraveledToCompassTarget))
            return false;

        this.params = (RewardForDistanceTraveledToCompassTarget)params;

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        BlockPos spawn = player.world.getSpawnPoint();
        BlockPos playerLoc = player.getPosition();
        this.previousDistance = playerLoc.getDistance(spawn.getX(), spawn.getY(), spawn.getZ());

        this.totalReward = 0;
        this.positionInitialized = false;

        return true;
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward)
    {
        boolean sendReward = false;

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        BlockPos spawn = player.world.getSpawnPoint();
        BlockPos playerLoc = player.getPosition();

        double currentDistance = playerLoc.getDistance(spawn.getX(), spawn.getY(), spawn.getZ());
        float delta = -1 * (float)(currentDistance - previousDistance);

        switch (this.params.getDensity()) {
        case MISSION_END:
            this.totalReward += this.params.getRewardPerBlock().floatValue() * delta;
            sendReward = reward.isFinalReward();
            break;
        case PER_TICK:
            this.totalReward = this.params.getRewardPerBlock().floatValue() * delta;
            sendReward = true;
            break;
        case PER_TICK_ACCUMULATED:
            this.totalReward += this.params.getRewardPerBlock().floatValue() * delta;
            sendReward = true;
            break;
        default:
            break;
        }

        // Avoid sending large rewards as the result of an initial teleport event
        if(!this.positionInitialized && (delta < -0.0001 || 0.0001 < delta)){
            this.positionInitialized = true;
            this.totalReward = 0;
        }

        this.previousDistance = playerLoc.getDistance(spawn.getX(), spawn.getY(), spawn.getZ());

        super.getReward(missionInit, reward);
        if (sendReward)
        {
            float adjusted_reward = adjustAndDistributeReward(this.totalReward, this.params.getDimension(), this.params.getRewardDistribution());
            reward.add(this.params.getDimension(), adjusted_reward);
        }
    }
}
