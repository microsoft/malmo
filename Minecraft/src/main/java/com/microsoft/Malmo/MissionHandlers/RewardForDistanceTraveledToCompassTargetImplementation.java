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
    BlockPos prevSpawn;

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForDistanceTraveledToCompassTarget))
            return false;

        this.params = (RewardForDistanceTraveledToCompassTarget)params;

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if( player != null && player.world != null){
            prevSpawn = player.world.getSpawnPoint();
            BlockPos playerLoc = player.getPosition();
            this.previousDistance = playerLoc.getDistance(prevSpawn.getX(), prevSpawn.getY(), prevSpawn.getZ());
        }
        else{
            prevSpawn = new BlockPos(0,0,0);
            previousDistance = 0;
        }

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
        if (this.prevSpawn.getX() != spawn.getX() ||
                this.prevSpawn.getY() != spawn.getY() ||
                this.prevSpawn.getZ() != spawn.getZ()) {
            this.totalReward = 0;
        }

        this.previousDistance = currentDistance;
        this.prevSpawn = spawn;

        super.getReward(missionInit, reward);
        if (sendReward)
        {
            float adjusted_reward = adjustAndDistributeReward(this.totalReward, this.params.getDimension(), this.params.getRewardDistribution());
            reward.add(this.params.getDimension(), adjusted_reward);
        }
    }
}
