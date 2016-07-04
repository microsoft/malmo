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

import com.microsoft.Malmo.Schemas.AgentHandlers;
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
    public void buildOnWorld(MissionInit missionInit) throws DecoratorException;
    
    /** Gives the decorator a chance to add any client-side mission handlers that might be required - eg end-points for the maze generator, etc.
     * @param handlers A list of handlers to which the decorator can add
     * @return true if new decorators were added
     */
    public boolean getExtraAgentHandlers(AgentHandlers handlers);

	/** Called periodically by the server, during the mission run. Use to provide dynamic behaviour.
	 * @param world the World we are controlling.
	 */
	void update(World world);
}
