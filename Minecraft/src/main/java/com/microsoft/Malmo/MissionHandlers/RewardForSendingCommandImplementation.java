package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForSendingCommand;

/** Simple discrete reward signal, which tests for movement, goal-reaching, and lava-swimming.<br>
 */
public class RewardForSendingCommandImplementation extends HandlerBase implements IRewardProducer
{
	protected float rewardPerCommand;
	protected Integer commandTally = 0;
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof RewardForSendingCommand))
			return false;
		
		this.rewardPerCommand = ((RewardForSendingCommand)params).getReward().floatValue();
		return true;
	}
	
	@Override
	public void prepare(MissionInit missionInit)
	{
        // We need to see the commands as they come in, so we can calculate the reward.
        // To do this we create our own command handler and insert it at the root of the command chain.
        // This is also how the ObservationFromRecentCommands handler works.
		// It's slightly dirty behaviour, but it's cleaner than the other options!
        MissionBehaviour mb = parentBehaviour();
        ICommandHandler oldch = mb.commandHandler;
        CommandGroup newch = new CommandGroup() {
            protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
            {
                // See if this command gets handled by the legitimate handlers:
                boolean handled = super.onExecute(verb, parameter, missionInit);
                if (handled)    // Yes, so record it:
                {
                	synchronized (RewardForSendingCommandImplementation.this.commandTally)
                	{
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
	public void cleanup() {}
	
	@Override
	public float getReward(MissionInit missionInit)
	{
		float reward;
    	synchronized (RewardForSendingCommandImplementation.this.commandTally)
    	{
    		reward = this.rewardPerCommand * this.commandTally;
    		this.commandTally = 0;
    	}
        return reward;
    }
}
