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

package com.microsoft.Malmo.MissionHandlerInterfaces;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;

import com.microsoft.Malmo.Schemas.MissionInit;

/** Interface for objects which can determine the world structure for the Minecraft mission.
 */
public interface IWorldDecorator
{
    public class DecoratorException extends Exception
    {
        private static final long serialVersionUID = 1L;
        public DecoratorException(String message)
        {
            super(message);
        }
    }

    /** Get the world into the required state for the start of the mission.
     * @param missionInit the MissionInit object for the currently running mission, which may contain parameters for the observation requirements.
     */
    public void buildOnWorld(MissionInit missionInit, World world) throws DecoratorException;

    /** Gives the decorator a chance to add any client-side mission handlers that might be required - eg end-points for the maze generator, etc -
     * and to communicate (via the map) any data back to the client-side.
     * @param handlers A list of handlers to which the decorator can add
     * @param data A map which will be passed to the client
     * @return true if new decorators were added
     */
    public boolean getExtraAgentHandlersAndData(List<Object> handlers, Map<String, String> data);

    /** Called periodically by the server, during the mission run. Use to provide dynamic behaviour.
     * @param world the World we are controlling.
     */
    void update(World world);

    /** Called once AFTER buildOnWorld but before the mission starts - use for any necessary mission initialisation.
     */
    public void prepare(MissionInit missionInit);

    /** Called once after the mission ends - use for any necessary mission cleanup.
     */
    public void cleanup();

    /** Used by the turn scheduler - if decorator matches this string, it must acknowledge and take its turn.
     * @param nextAgentName - string to match against
     * @return true if matching
     */
    public boolean targetedUpdate(String nextAgentName);

    /** Used by the turn scheduler - if the decorator wants to be part of the turn schedule, it must add a name
     * and a requested slot (can be null) to these arrays.
     * @param participants
     * @param participantSlots
     */
    public void getTurnParticipants(ArrayList<String> participants, ArrayList<Integer> participantSlots);
}
