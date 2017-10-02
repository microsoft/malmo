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

import net.minecraft.world.World;

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
    public boolean shouldCreateWorld(MissionInit missionInit, World world);
    
    public String getErrorDetails();
}
