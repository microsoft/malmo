package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.microsoft.Malmo.Schemas.EntityTypes;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.MobWithDescriptionAndReward;
import com.microsoft.Malmo.Schemas.RewardForCatchingMob;

public class RewardForCatchingMobImplementation extends RewardBase
{
    RewardForCatchingMob rcmparams;
    List<Entity> caughtEntities = new ArrayList<Entity>();

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForCatchingMob))
            return false;

        this.rcmparams = (RewardForCatchingMob)params;
        return true;
    }

    static List<Entity> getCaughtEntities()
    {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        World world = player.world;
        // Get all the currently loaded entities:
        List<?> entities = Minecraft.getMinecraft().world.getLoadedEntityList();
        // Now filter out all the player entities:
        List<BlockPos> entityPositions = new ArrayList<BlockPos>();
        for (Object obj : entities)
        {
            if (obj instanceof EntityPlayer)
            {
                EntityPlayer ep = (EntityPlayer)obj;
                entityPositions.add(new BlockPos(ep.posX, ep.posY, ep.posZ));
            }
        }
        // Now search for trapped entities
        List<Entity> trappedEntities = new ArrayList<Entity>();
        BlockPos playerPos = new BlockPos((int)player.posX, (int)player.posY, (int)player.posZ);
        for (Object obj : entities)
        {
            if (obj instanceof EntityPlayer)
                continue;   // Don't score points for catching other players.
            if (obj instanceof Entity)
            {
                Entity e = (Entity)obj;
                BlockPos entityPos = new BlockPos((int)e.posX, (int)e.posY, (int)e.posZ);
                // For now, only consider entities on the same plane as us:
                if (entityPos.getY() != playerPos.getY())
                    continue;
                // Now see whether the mob can move anywhere:
                boolean canEscape = false;
                for (int x = -1; x <= 1 && !canEscape; x++)
                {
                    for (int z = -1; z <= 1 && !canEscape; z++)
                    {
                        if (Math.abs(x) == Math.abs(z))
                            continue;   // Only consider the n/s/e/w blocks - ignore diagonals.
                        BlockPos square = new BlockPos(entityPos.getX() + x, entityPos.getY(), entityPos.getZ() + z);
                        if (world.isAirBlock(square) && !entityPositions.contains(square))
                            canEscape = true;
                    }
                }
                if (!canEscape)
                {
                    trappedEntities.add(e);
                }
            }
        }
        return trappedEntities;
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward)
    {
        super.getReward(missionInit, reward);

        List<Entity> trappedEntities = getCaughtEntities();
        for (MobWithDescriptionAndReward mob : this.rcmparams.getMob())
        {
            // Have we caught one of these mobs?
            for (EntityTypes et : mob.getType())
            {
                String mobName = et.value();
                for (Entity e : trappedEntities)
                {
                    if (e.getName().equals(mobName))
                    {
                        // Potential match... check other options.
                        if (!mob.isGlobal())
                        {
                            // If global flag is false, our player needs to be adjacent to the mob in order to claim the reward.
                            BlockPos entityPos = new BlockPos(e.posX, e.posY, e.posZ);
                            EntityPlayerSP player = Minecraft.getMinecraft().player;
                            BlockPos playerPos = new BlockPos(player.posX, player.posY, player.posZ);
                            if (Math.abs(entityPos.getX() - playerPos.getX()) + Math.abs(entityPos.getZ() - playerPos.getZ()) > 1)
                                continue;
                        }
                        // If oneshot flag is true, only allow the reward from this mob to be counted once.
                        if (mob.isOneshot() && this.caughtEntities.contains(e))
                            continue;
                        // Can claim the reward.
                        float adjusted_reward = adjustAndDistributeReward(mob.getReward().floatValue(), this.rcmparams.getDimension(), mob.getDistribution());
                        reward.add(this.rcmparams.getDimension(), adjusted_reward);
                    }
                }
            }
        }
    }
}
