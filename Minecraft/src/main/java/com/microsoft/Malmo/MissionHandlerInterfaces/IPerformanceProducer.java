package com.microsoft.Malmo.MissionHandlerInterfaces;

import com.microsoft.Malmo.Schemas.MissionInit;

/** Interface for performance producers/
 */
public interface IPerformanceProducer
{
    /**
     * Called every step in a running mission.
     * @param reward The current reward
     * @param done If the environment is done.
     */
    public void step(double reward, boolean done);

    /**
     * Called at the beginning of every mission.
     */
    public void prepare(MissionInit missionInit);
}
