package com.microsoft.Malmo.MissionHandlerInterfaces;

import com.microsoft.Malmo.Schemas.MissionInit;

/** Interface for objects which can determine the world structure for the Minecraft mission.
 */
public interface IWorldGenerator
{
    /** Provide a world - eg by loading it from a basemap file, or by creating one procedurally.
     * @param missionInit the MissionInit object for the currently running mission, which may contain parameters for the observation requirements.
     * @return true if a world has been created, false otherwise
     */
    public boolean createWorld(MissionInit missionInit);
    
    /** Determine whether or not createWorld should be called.<br>
     * If this returns false, createWorld will not be called, and the player will simply be respawned in the current world.
     * It provides a means for a "quick reset" - eg, the world builder could decide that the state of the current world is close enough to the
     * desired state that there is no point building a whole new world.
     * @param missionInit the MissionInit object for the currently running mission, which may contain parameters for the observation requirements.
     * @return true if the world should be created, false otherwise.
     */
    public boolean shouldCreateWorld(MissionInit missionInit);
}
