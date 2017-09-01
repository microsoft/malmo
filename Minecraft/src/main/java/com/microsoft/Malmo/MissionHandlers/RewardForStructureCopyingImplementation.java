package com.microsoft.Malmo.MissionHandlers;

import java.util.Map;

import net.minecraftforge.common.MinecraftForge;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.IMalmoMessageListener;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardDensityForBuildAndBreak;
import com.microsoft.Malmo.Schemas.RewardForStructureCopying;

public class RewardForStructureCopyingImplementation extends RewardBase implements IRewardProducer, IMalmoMessageListener
{
    private RewardForStructureCopying rscparams;
    private RewardDensityForBuildAndBreak rewardDensity;

    private float reward = 0.0F;
    private int dimension;
    private boolean structureHasBeenCompleted = false;

    /**
     * Attempt to parse the given object as a set of parameters for this handler.
     *
     * @param params the parameter block to parse
     * @return true if the object made sense for this handler; false otherwise.
     */
    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForStructureCopying))
            return false;

        this.rscparams = (RewardForStructureCopying) params;

        this.rewardDensity = rscparams.getRewardDensity();
        this.dimension = rscparams.getDimension();
        return true;
    }

    /**
     * Get the reward value for the current Minecraft state.
     *
     * @param missionInit the MissionInit object for the currently running mission,
     * which may contain parameters for the reward requirements.
     * @param multidimReward
     */
    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward multidimReward)
    {
        super.getReward(missionInit, multidimReward);
        if (this.rewardDensity == RewardDensityForBuildAndBreak.MISSION_END)
        {
            // Only send the reward at the very end of the mission.
            if (multidimReward.isFinalReward() && this.reward != 0)
            {
                float adjusted_reward = adjustAndDistributeReward(this.reward, this.dimension, this.rscparams.getRewardDistribution());
                multidimReward.add(this.dimension, adjusted_reward);
            }
        }
        else
        {
            // Send reward immediately.
            if (this.reward != 0)
            {
                synchronized (this)
                {
                    float adjusted_reward = adjustAndDistributeReward(this.reward, this.dimension, this.rscparams.getRewardDistribution());
                    multidimReward.add(this.dimension, adjusted_reward);
                    this.reward = 0;
                }
            }
        }
    }

    /**
     * Called once before the mission starts - use for any necessary initialisation.
     * @param missionInit
     */
    @Override
    public void prepare(MissionInit missionInit)
    {
        super.prepare(missionInit);
        MinecraftForge.EVENT_BUS.register(this);
        MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_BUILDBATTLEREWARD);

        if (this.rscparams.getAddQuitProducer() != null)
        {
            // In order to trigger the end of the mission, we need to hook into the quit handlers.
            MissionBehaviour mb = parentBehaviour();
            final String quitDescription = this.rscparams.getAddQuitProducer().getDescription();
            mb.addQuitProducer(new IWantToQuit()
            {
                @Override
                public void prepare(MissionInit missionInit)
                {
                }
    
                @Override
                public String getOutcome()
                {
                    return quitDescription;
                }
    
                @Override
                public boolean doIWantToQuit(MissionInit missionInit)
                {
                    return RewardForStructureCopyingImplementation.this.structureHasBeenCompleted;
                }
    
                @Override
                public void cleanup()
                {
                }
            });
        }
    }

    /**
     * Called once after the mission ends - use for any necessary cleanup.
     */
    @Override
    public void cleanup()
    {
        super.cleanup();
        MinecraftForge.EVENT_BUS.unregister(this);
        structureHasBeenCompleted = false;
        MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_MISSIONOVER);
    }

    @Override
    public void onMessage(MalmoMessageType messageType, Map<String, String> data)
    {
        if (messageType == MalmoMessageType.SERVER_BUILDBATTLEREWARD && data != null)
        {
            String strCompleted = data.get("completed");
            String strReward = data.get("reward");
            Boolean completed = strCompleted != null ? Boolean.valueOf(strCompleted) : Boolean.FALSE;
            Integer reward = strReward != null ? Integer.valueOf(strReward) : 0;
            synchronized (this)
            {
                if (completed == Boolean.TRUE)
                {
                    this.structureHasBeenCompleted = true;
                    this.reward += this.rscparams.getRewardForCompletion().floatValue();
                }
                if (reward != null)
                {
                    this.reward += reward * this.rscparams.getRewardScale().floatValue();
                }
            }
        }
    }
}