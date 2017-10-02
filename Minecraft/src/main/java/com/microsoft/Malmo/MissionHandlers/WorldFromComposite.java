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

package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public void buildOnWorld(MissionInit missionInit, World world) throws DecoratorException
    {
        for (IWorldDecorator builder : this.builders)
        {
            builder.buildOnWorld(missionInit, world);
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

    @Override
    public boolean getExtraAgentHandlersAndData(List<Object> handlers, Map<String, String> data)
    {
        boolean added = false;
        for (IWorldDecorator builder : this.builders)
        {
            added |= builder.getExtraAgentHandlersAndData(handlers, data);
        }
        return added;
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        for (IWorldDecorator builder : this.builders)
        {
            builder.prepare(missionInit);
        }
    }

    @Override
    public void cleanup()
    {
        for (IWorldDecorator builder : this.builders)
        {
            builder.cleanup();
        }
    }

    @Override
    public boolean targetedUpdate(String nextAgentName)
    {
        for (IWorldDecorator builder : this.builders)
        {
            if (builder.targetedUpdate(nextAgentName))
                return true;
        }
        return false;
    }

    @Override
    public void getTurnParticipants(ArrayList<String> participants, ArrayList<Integer> participantSlots)
    {
        for (IWorldDecorator builder : this.builders)
        {
            builder.getTurnParticipants(participants, participantSlots);
        }
    }

    public boolean isFixed()
    {
        return false;   // Return true to stop MissionBehaviour from adding new handlers to this group.
    }
}
