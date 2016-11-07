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

import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForSendingCommand;

/**
 * Simple discrete reward signal, which tests for movement, goal-reaching, and
 * lava-swimming.<br>
 */
public class RewardForSendingCommandImplementation extends RewardBase implements IRewardProducer {
    protected float rewardPerCommand;
    protected Integer commandTally = 0;
    private RewardForSendingCommand params;

    @Override
    public boolean parseParameters(Object params) {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForSendingCommand))
            return false;

        this.params = (RewardForSendingCommand) params;
        this.rewardPerCommand = this.params.getReward().floatValue();
        return true;
    }

    @Override
    public void prepare(MissionInit missionInit) {
        super.prepare(missionInit);
        // We need to see the commands as they come in, so we can calculate the
        // reward.
        // To do this we create our own command handler and insert it at the
        // root of the command chain.
        // This is also how the ObservationFromRecentCommands handler works.
        // It's slightly dirty behaviour, but it's cleaner than the other
        // options!
        MissionBehaviour mb = parentBehaviour();
        ICommandHandler oldch = mb.commandHandler;
        CommandGroup newch = new CommandGroup() {
            protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
                // See if this command gets handled by the legitimate handlers:
                boolean handled = super.onExecute(verb, parameter, missionInit);
                if (handled) // Yes, so record it:
                {
                    synchronized (RewardForSendingCommandImplementation.this.commandTally) {
                        RewardForSendingCommandImplementation.this.commandTally++;
                    }
                }
                return handled;
            }
        };

        newch.setOverriding((oldch != null) ? oldch.isOverriding() : true);
        if (oldch != null)
            newch.addCommandHandler(oldch);
        mb.commandHandler = newch;
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward) {
        super.getReward(missionInit, reward);
        synchronized (RewardForSendingCommandImplementation.this.commandTally) {
            if( this.commandTally > 0) {
                float reward_value = this.rewardPerCommand * this.commandTally;
                float adjusted_reward = adjustAndDistributeReward(reward_value, this.params.getDimension(), this.params.getDistribution());
                reward.add( this.params.getDimension(), adjusted_reward );
                this.commandTally = 0;
            }
        }
    }
}
