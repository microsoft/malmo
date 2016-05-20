package com.microsoft.Malmo.MissionHandlerInterfaces;

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
    public void buildOnWorld(MissionInit missionInit) throws DecoratorException;

	/** Called periodically by the server, during the mission run. Use to provide dynamic behaviour.
	 * @param world the World we are controlling.
	 */
	void update(World world);
}
