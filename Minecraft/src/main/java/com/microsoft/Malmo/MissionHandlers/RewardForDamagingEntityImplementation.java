package com.microsoft.Malmo.MissionHandlers;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.EntityTypes;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.MobWithReward;
import com.microsoft.Malmo.Schemas.RewardForDamagingEntity;

public class RewardForDamagingEntityImplementation extends RewardBase implements IRewardProducer
{
    RewardForDamagingEntity params;
    Map<MobWithReward, Float> damages = new HashMap<MobWithReward, Float>();

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForDamagingEntity))
            return false;

        this.params = (RewardForDamagingEntity) params;
        return true;
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward)
    {
        super.getReward(missionInit, reward);
        if (missionInit == null || Minecraft.getMinecraft().player == null)
            return;
        synchronized (this.damages)
        {
            for (MobWithReward mob : this.damages.keySet())
            {
                float damage_amount = this.damages.get(mob);
                float damage_reward = damage_amount * mob.getReward().floatValue();
                float adjusted_reward = adjustAndDistributeReward(damage_reward, this.params.getDimension(), mob.getDistribution());
                reward.add(this.params.getDimension(), adjusted_reward);
            }
            this.damages.clear();
        }
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        super.prepare(missionInit);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onLivingAttackEvent(LivingAttackEvent event)
    {
        if (event.getEntity() == null || event.getSource().getEntity() != Minecraft.getMinecraft().player)
            return;
        synchronized (this.damages)
        {
            for (MobWithReward mob : this.params.getMob())
            {
                // Have we caught one of these mobs?
                for (EntityTypes et : mob.getType())
                {
                    String mobName = et.value();
                    if (event.getEntity().getName().equals(mobName))
                    {
                        if (this.damages.containsKey(mob))
                            this.damages.put(mob, this.damages.get(mob) + event.getAmount());
                        else
                            this.damages.put(mob, event.getAmount());
                    }
                }
            }
        }
    }
}