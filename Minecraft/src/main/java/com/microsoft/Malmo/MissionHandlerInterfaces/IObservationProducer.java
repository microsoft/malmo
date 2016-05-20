package com.microsoft.Malmo.MissionHandlerInterfaces;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Interface for observation data builders.<br>
 * These objects will build observation data from the Minecraft environment, and add them to a JSON object.
 */
public interface IObservationProducer
{
    /** Gather whatever data is required about the Minecraft environment, and return it as a string.
     * @param json the JSON object into which to add our observations
     * @param missionInit the MissionInit object for the currently running mission, which may contain parameters for the observation requirements.
     */
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit);

    /** Called once before the mission starts - use for any necessary initialisation.
     * @param missionInit TODO
     */
    public void prepare(MissionInit missionInit);
    
    /** Called once after the mission ends - use for any necessary cleanup.
     * 
     */
    public void cleanup();
}
