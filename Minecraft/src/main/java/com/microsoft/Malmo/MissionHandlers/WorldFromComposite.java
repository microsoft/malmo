package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;

import net.minecraft.world.World;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Composite class that manages a set of world builders
 */
public class WorldFromComposite extends HandlerBase implements IWorldDecorator
{
    private ArrayList<IWorldDecorator> builders = new ArrayList<IWorldDecorator>();

    public void addBuilder(IWorldDecorator builder)
    {
        this.builders.add(builder);
    }
    
    @Override
    public void buildOnWorld(MissionInit missionInit) throws DecoratorException
    {
        for (IWorldDecorator builder : this.builders)
        {
            builder.buildOnWorld(missionInit);
        }
    }
    
    @Override
    public void update(World world)
    {
        for (IWorldDecorator builder : this.builders)
        {
            builder.update(world);
        }
    }
}
