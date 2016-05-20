package com.microsoft.Malmo.MissionHandlerInterfaces;

import com.microsoft.Malmo.Schemas.MissionInit;

/** Interface for objects which are responsible for providing a reward signal for reinforcement learning.<br>
 */
public interface IRewardProducer
{
    /** Get the reward value for the current Minecraft state.
     * @param missionInit the MissionInit object for the currently running mission, which may contain parameters for the reward requirements.
     * @return a float determining the current reward signal for the learning agent
     */
    public float getReward(MissionInit missionInit);
    
    /** Called once before the mission starts - use for any necessary initialisation.*/
    public void prepare(MissionInit missionInit);
    
    /** Called once after the mission ends - use for any necessary cleanup.*/
    public void cleanup();
}
