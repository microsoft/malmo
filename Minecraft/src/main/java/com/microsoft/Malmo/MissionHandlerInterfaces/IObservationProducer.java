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
