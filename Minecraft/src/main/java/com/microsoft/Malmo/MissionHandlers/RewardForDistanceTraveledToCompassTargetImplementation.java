package com.microsoft.Malmo.MissionHandlers;

import java.lang.*;

import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForDistanceTraveledToCompassTarget;


import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class RewardForDistanceTraveledToCompassTargetImplementation extends RewardBase
{
    RewardForDistanceTraveledToCompassTarget params;
    double previousDistance;
    float totalReward;
    boolean positionInitialized;
    BlockPos spawn;

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForDistanceTraveledToCompassTarget))
            return false;

        this.params = (RewardForDistanceTraveledToCompassTarget)params;

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        spawn = player.world.getSpawnPoint();
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
        spawn = player.world.getSpawnPoint();
        Vec3d playerLoc = player.getPositionVector();
        Vec3d spawnPos = new Vec3d(spawn.getX(), spawn.getY(), spawn.getZ());

        double currentDistance = playerLoc.distanceTo(spawnPos);
        float delta = (float)(this.previousDistance - currentDistance);

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
        if(!this.positionInitialized && Math.abs(delta) > 0.0001) {
            this.totalReward = 0;
        } else {
            this.positionInitialized = true;
        }

        this.previousDistance = currentDistance;

        super.getReward(missionInit, reward);
        if (sendReward)
        {
            float adjusted_reward = adjustAndDistributeReward(this.totalReward, this.params.getDimension(), this.params.getRewardDistribution());
            reward.add(this.params.getDimension(), adjusted_reward);
        }
    }
}
