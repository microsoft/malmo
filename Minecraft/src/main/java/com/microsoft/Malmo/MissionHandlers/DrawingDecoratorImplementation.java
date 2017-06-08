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

        this.drawing = (DrawingDecorator) params;
        return true;
    }

    @Override
    public void buildOnWorld(MissionInit missionInit, World world)
    {
        Mission mission = missionInit.getMission();
        if (mission != null)
        {
            try
            {
                BlockDrawingHelper drawContext = new BlockDrawingHelper();
                drawContext.Draw(this.drawing, world);
            }
            catch (Exception e)
            {
                System.out.println("Error drawing into the world: " + e.getMessage());
            }
        }
    }

    @Override
    public void update(World world) {}

    @Override
    public boolean getExtraAgentHandlersAndData(List<Object> handlers, Map<String, String> data)
    {
        return false;
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
    }

    @Override
    public void cleanup()
    {
    }

    @Override
    public boolean targetedUpdate(String nextAgentName)
    {
        return false;   // Does nothing.
    }

    @Override
    public void getTurnParticipants(ArrayList<String> participants, ArrayList<Integer> participantSlots)
    {
        // Does nothing.
    }
}