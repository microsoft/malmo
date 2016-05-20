package com.microsoft.Malmo.MissionHandlerInterfaces;

import com.microsoft.Malmo.Schemas.MissionInit;

/** Interface for objects which are used to determine the end-of-mission.
 */
public interface IWantToQuit
{
    /** Called repeatedly during the mission in order to determine when (if ever) the mission should end.
     * @param missionInit the MissionInit object for the currently running mission, which may contain parameters to determine the quit behaviour
     * @return true when the mission is to be ended (for whatever reason); false if the mission is to continue.
     */
    public boolean doIWantToQuit(MissionInit missionInit);
    
    /** Called once before the mission starts - use for any necessary initialisation.*/
    public void prepare(MissionInit missionInit);
    
    /** Called once after the mission ends - use for any necessary cleanup.*/
    public void cleanup();
    
    /** Called by the Mod after quitting, provides a means to return a quit code to the agent.*/
    public String getOutcome();
}
