package com.microsoft.Malmo.MissionHandlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

public class RewardBase extends HandlerBase implements IRewardProducer
{
    private String agentName;
    protected MultidimensionalReward cachedRewards = new MultidimensionalReward();

    public String getAgentName() { return this.agentName; }

    protected float adjustAndDistributeReward(float reward, int dimension, String distribution)
    {
        float scaled_reward = reward;
        if (distribution == null || distribution.isEmpty())
            return reward;
        List<String> parties = Arrays.asList(distribution.split(" "));
        // Search for our agent name in this list of parties:
        int ind = 0;
        for (String party : parties)
        {
            if (party.startsWith(this.agentName + ":"))
                break;
            ind++;
        }
        if (ind == parties.size())
        {
            // Didn't find it - search for "me":
            ind = 0;
            for (String party : parties)
            {
                if (party.startsWith("me:"))
                    break;
                ind++;
            }
        }
        if (ind != parties.size())
        {
            String us = parties.get(ind);
            String[] parts = us.split(":");
            if (parts.length != 2)  // Syntax error
            {
                System.out.println("ERROR: malformed argument for distribution of reward - " + us);
                System.out.println("Entire reward going to " + this.agentName);
                return reward;
            }
            else
            {
                Float f = Float.valueOf(parts[1]);
                if (f != null)
                {
                    // Scale our reward:
                    scaled_reward = reward * f;
                }
            }
        }
        else
            scaled_reward = 0;  // There's a distribution, but we're not included in it - we get nothing.
        // Now broadcast the reward to the other clients (but don't make a map entry for ourselves)
        Map<String, String> data = new HashMap<String, String>();
        for (String agent : parties)
        {
            String[] parts = agent.split(":");
            if (parts.length == 2 && ind != 0)
                data.put(parts[0], parts[1]);
            ind--;
        }
        // And put the original reward in the map:
        data.put("original_reward", ((Float)reward).toString());
        // And the dimension:
        data.put("dimension", ((Integer)dimension).toString());
        MalmoMod.network.sendToServer(new MalmoMod.MalmoMessage(MalmoMessageType.CLIENT_SHARE_REWARD, 0, data));
        return scaled_reward;
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        this.agentName = missionInit.getMission().getAgentSection().get(missionInit.getClientRole()).getName();
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward)
    {
        reward.add(this.cachedRewards);
        this.cachedRewards.clear();
    }

    protected void addCachedReward(int dimension, float reward)
    {
        synchronized (this.cachedRewards)
        {
            this.cachedRewards.add(dimension, reward);
        }
    }

    protected void addCachedReward(MultidimensionalReward reward)
    {
        synchronized (this.cachedRewards)
        {
            this.cachedRewards.add(reward);
        }
    }

    protected void addAndShareCachedReward(int dimension, float reward, String distribution)
    {
        float adjusted_reward = adjustAndDistributeReward(reward, dimension, distribution);
        addCachedReward(dimension, adjusted_reward);
    }

    @Override
    public void cleanup()
    {
    }
}