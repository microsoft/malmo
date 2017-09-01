package com.microsoft.Malmo.MissionHandlers;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.AgentQuitFromCatchingMob;
import com.microsoft.Malmo.Schemas.EntityTypes;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.MobWithDescription;

public class AgentQuitFromCatchingMobImplementation extends HandlerBase implements IWantToQuit
{
    AgentQuitFromCatchingMob qcmparams;
    String quitCode;
    
    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof AgentQuitFromCatchingMob))
            return false;

        this.qcmparams = (AgentQuitFromCatchingMob)params;
        return true;
    }

    @Override
    public boolean doIWantToQuit(MissionInit missionInit)
    {
        this.quitCode = "";
        List<Entity> caughtEntities = RewardForCatchingMobImplementation.getCaughtEntities();
        for (Entity ent : caughtEntities)
        {
            // Do we care about this entity?
            for (MobWithDescription mob : this.qcmparams.getMob())
            {
                for (EntityTypes et : mob.getType())
                {
                    if (et.value().equals(ent.getName()))
                    {
                        if (!mob.isGlobal())
                        {
                            // If global flag is false, our player needs to be adjacent to the mob in order to claim the reward.
                            BlockPos entityPos = new BlockPos(ent.posX, ent.posY, ent.posZ);
                            EntityPlayerSP player = Minecraft.getMinecraft().player;
                            BlockPos playerPos = new BlockPos(player.posX, player.posY, player.posZ);
                            if (Math.abs(entityPos.getX() - playerPos.getX()) + Math.abs(entityPos.getZ() - playerPos.getZ()) > 1)
                                continue;
                        }
                        this.quitCode = mob.getDescription();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
    }

    @Override
    public void cleanup()
    {
    }

    @Override
    public String getOutcome()
    {
        return this.quitCode;
    }
}
