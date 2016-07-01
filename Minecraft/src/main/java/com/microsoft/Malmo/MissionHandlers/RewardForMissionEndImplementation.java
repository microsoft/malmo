// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

package com.microsoft.Malmo.MissionHandlers;

import java.util.Hashtable;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.MissionEndRewardCase;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForMissionEnd;

public class RewardForMissionEndImplementation extends HandlerBase implements IRewardProducer {
    private RewardForMissionEnd params = null;

    @Override
    public boolean parseParameters(Object params) {
        if (params == null || !(params instanceof RewardForMissionEnd))
            return false;

        this.params = (RewardForMissionEnd) params;
        return true;
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward) {
        try {
            Hashtable<String, Object> properties = MalmoMod.getPropertiesForCurrentThread();
            if (properties.containsKey("QuitCode")) {
                float reward_value = parseQuitCode((String) properties.get("QuitCode"));
                reward.add( this.params.getDimension(), reward_value);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void prepare(MissionInit missionInit) {
        // Make sure we start with a clean slate:
        try {
            if (MalmoMod.getPropertiesForCurrentThread().containsKey("QuitCode"))
                MalmoMod.getPropertiesForCurrentThread().remove("QuitCode");
        } catch (Exception e) {
            System.out.println("Failed to get properties.");
        }
    }

    @Override
    public void cleanup() {
    }

    private float parseQuitCode(String qc) {
        float reward = 0;
        if (qc != null && !qc.isEmpty() && this.params != null) {
            String[] codes = qc.split(";");
            for (String s : codes) {
                for (MissionEndRewardCase merc : this.params.getReward()) {
                    if (merc.getDescription().equalsIgnoreCase(s))
                        reward += merc.getReward().floatValue();
                }
                if (s.equals(MalmoMod.AGENT_DEAD_QUIT_CODE))
                    reward += this.params.getRewardForDeath().floatValue();
            }
        }
        return reward;
    }
}
