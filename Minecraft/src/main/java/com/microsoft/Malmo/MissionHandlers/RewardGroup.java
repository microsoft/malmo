package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

public class RewardGroup extends HandlerBase implements IRewardProducer
{
    private ArrayList<IRewardProducer> producers;
	
	/** Add another RewardProducer object.<br>
	 * @param producer the reward producing object to add to the mix.
	 */
	public void addRewardProducer(IRewardProducer producer)
	{
		if (this.producers == null)
		{
			this.producers = new ArrayList<IRewardProducer>();
		}
		this.producers.add(producer);
	}

	@Override
	public float getReward(MissionInit missionInit)
	{
		float reward = 0;
		if (this.producers != null)
		{
			for (IRewardProducer rp : this.producers)
				reward += rp.getReward(missionInit);
		}
		return reward;
	}
	
	@Override
	public void prepare(MissionInit missionInit)
	{
		if (this.producers != null)
		{
			for (IRewardProducer rp : this.producers)
				rp.prepare(missionInit);
		}
	}
    
	@Override
    public void cleanup()
    {
		if (this.producers != null)
		{
			for (IRewardProducer rp : this.producers)
				rp.cleanup();
		}
    }
}
