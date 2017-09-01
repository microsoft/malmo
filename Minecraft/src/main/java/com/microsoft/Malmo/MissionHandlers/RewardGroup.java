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

import java.util.ArrayList;
import java.util.HashMap;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

public class RewardGroup extends RewardBase implements IRewardProducer {
    private ArrayList<IRewardProducer> producers;

    /**
     * Add another RewardProducer object.<br>
     * 
     * @param producer
     *            the reward producing object to add to the mix.
     */
    public void addRewardProducer(IRewardProducer producer) {
        if (this.producers == null) {
            this.producers = new ArrayList<IRewardProducer>();
        }
        this.producers.add(producer);
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward) {
        if (this.producers != null) {
            for (IRewardProducer rp : this.producers)
                rp.getReward(missionInit,reward);
        }
    }

    @Override
    public void prepare(MissionInit missionInit) {
        if (this.producers != null) {
            for (IRewardProducer rp : this.producers)
                rp.prepare(missionInit);
        }
    }

    @Override
    public void cleanup() {
        if (this.producers != null) {
            for (IRewardProducer rp : this.producers)
                rp.cleanup();
        }
    }

    @Override
    public void appendExtraServerInformation(HashMap<String, String> map)
    {
        for (IRewardProducer rp : this.producers)
        {
            if (rp instanceof HandlerBase)
                ((HandlerBase)rp).appendExtraServerInformation(map);
        }
    }

    public boolean isFixed()
    {
        return false;   // Return true to stop MissionBehaviour from adding new handlers to this group.
    }
}
