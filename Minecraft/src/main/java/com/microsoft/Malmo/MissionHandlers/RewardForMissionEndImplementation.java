package com.microsoft.Malmo.MissionHandlers;

import java.util.Hashtable;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.MissionEndRewardCase;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForMissionEnd;

public class RewardForMissionEndImplementation extends HandlerBase implements IRewardProducer
{
	private RewardForMissionEnd params = null;
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof RewardForMissionEnd))
			return false;
		
		this.params = (RewardForMissionEnd)params;
		return true;
	}

	@Override
	public float getReward(MissionInit missionInit)
	{
		try
		{
			Hashtable<String, Object> properties = MalmoMod.getPropertiesForCurrentThread();
			if (properties.containsKey("QuitCode"))
				return parseQuitCode((String)properties.get("QuitCode"));
		}
		catch (Exception e)
		{
		}
		return 0;
	}

	@Override
	public void prepare(MissionInit missionInit)
	{
		// Make sure we start with a clean slate:
        try
        {
        	if (MalmoMod.getPropertiesForCurrentThread().containsKey("QuitCode"))
            	MalmoMod.getPropertiesForCurrentThread().remove("QuitCode");
		}
        catch (Exception e)
        {
        	System.out.println("Failed to get properties.");
		}
	}

	@Override
	public void cleanup()
	{
	}
	
	private float parseQuitCode(String qc)
	{
		float reward = 0;
		if (qc != null && !qc.isEmpty() && this.params != null)
		{
			String[] codes = qc.split(";");
			for (String s : codes)
			{
				for (MissionEndRewardCase merc : this.params.getReward())
				{
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
