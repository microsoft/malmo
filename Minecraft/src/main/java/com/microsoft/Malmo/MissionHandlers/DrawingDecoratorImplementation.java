package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.DrawingDecorator;
import com.microsoft.Malmo.Schemas.Mission;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.BlockDrawingHelper;

/** WorldBuilder that takes the XML drawing instructions from the Mission object.<br>
 */
public class DrawingDecoratorImplementation extends HandlerBase implements IWorldDecorator
{
	DrawingDecorator drawing = null;
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof DrawingDecorator))
			return false;
		
		this.drawing = (DrawingDecorator)params;
		return true;
	}

    @Override
	public void buildOnWorld(MissionInit missionInit)
	{
        Mission mission = missionInit.getMission();
        if (mission != null)
        {
            try
            {
                BlockDrawingHelper.Draw(this.drawing, MinecraftServer.getServer().getEntityWorld());
            }
            catch (Exception e)
            {
                System.out.println("Error drawing into the world: " + e.getMessage());
            }
        }
	}  

    @Override
    public void update(World world) {}
}