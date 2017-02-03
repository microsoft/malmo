package com.microsoft.Malmo.MissionHandlers;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Return an observation to signal to the agent when it is their turn, in a turn-based scenario.
 * If it is not our turn, no observation is sent.
 * If it *is* our turn, two pieces of information are sent:
 *  *turn_number - will be incremented after each turn;
 *    can be used by the agent to guard against confusion due to commands/observations crossing each other en route.
 *  *turn_key - a randomly generated key, which must be sent back as a parameter with the corresponding command.
 *    used to ensure that commands are only sent at appropriate times.
 */
public class ObservationFromTurnSchedulerImplementation extends HandlerBase implements IObservationProducer
{
    private int turn = 0;
    private String key = "";
    private boolean isOurTurn = false;
            
    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        synchronized(this)
        {
            if (this.isOurTurn)
            {
                json.addProperty("turn_number", this.turn);
                json.addProperty("turn_key", this.key);
            }
        }
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
    }

    @Override
    public void cleanup()
    {
    }

    public void setKeyAndIncrement(String newkey)
    {
        synchronized(this)
        {
            this.key = newkey;
            this.turn++;
            this.isOurTurn = true;
        }
    }

    public boolean matchesKey(String key)
    {
        return this.isOurTurn && key.equals(this.key);
    }

    public void turnUsed()
    {
        synchronized(this)
        {
            this.isOurTurn = false;
        }
    }
}