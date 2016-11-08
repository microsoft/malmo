package com.microsoft.Malmo.MissionHandlers;

import java.util.Map;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.IMalmoMessageListener;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.Schemas.MissionInit;

public class RewardFromTeamImplementation extends RewardBase implements IMalmoMessageListener
{
    @Override
    public void onMessage(MalmoMessageType messageType, Map<String, String> data)
    {
        if (messageType == MalmoMessageType.SERVER_SHARE_REWARD)
        {
            if (data != null && data.containsKey(getAgentName()))
            {
                String reward = data.get("original_reward");
                if (reward != null)
                {
                    Float base_reward = Float.valueOf(reward);
                    Float scale_factor = Float.valueOf(data.get(getAgentName()));
                    String strDimension = data.get("dimension");
                    Integer nDimension = (strDimension != null) ? Integer.valueOf(strDimension) : null;
                    int dimension = (nDimension != null) ? nDimension : 0;
                    if (base_reward != null && scale_factor != null)
                    {
                        float adjusted_reward = base_reward * scale_factor;
                        addCachedReward(dimension, adjusted_reward);
                    }
                }
            }
        }
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        super.prepare(missionInit);
        MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_SHARE_REWARD);
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_SHARE_REWARD);
    }
}
