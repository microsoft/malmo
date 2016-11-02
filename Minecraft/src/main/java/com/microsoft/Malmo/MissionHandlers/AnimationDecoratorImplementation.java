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

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.AnimationDecorator;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.AnimationDrawingHelper;

/** WorldBuilder that takes a DrawingDecorator object and animates it.<br>
 */
public class AnimationDecoratorImplementation extends HandlerBase implements IWorldDecorator
{
    AnimationDecorator params = null;
    AnimationDrawingHelper drawContext = new AnimationDrawingHelper();
    Vec3 origin;
    Vec3 velocity;
    Vec3 minCanvas;
    Vec3 maxCanvas;

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof AnimationDecorator))
            return false;

        this.params = (AnimationDecorator) params;
        this.origin = new Vec3(this.params.getInitialPos().getX().doubleValue(), this.params.getInitialPos().getY().doubleValue(), this.params.getInitialPos().getZ().doubleValue());
        this.velocity = new Vec3(this.params.getInitialVelocity().getX().doubleValue(), this.params.getInitialVelocity().getY().doubleValue(), this.params.getInitialVelocity().getZ().doubleValue());
        this.minCanvas = new Vec3(this.params.getCanvasBounds().getMin().getX(), this.params.getCanvasBounds().getMin().getY(), this.params.getCanvasBounds().getMin().getZ());
        this.maxCanvas = new Vec3(this.params.getCanvasBounds().getMax().getX(), this.params.getCanvasBounds().getMax().getY(), this.params.getCanvasBounds().getMax().getZ());
        return true;
    }

    @Override
    public void buildOnWorld(MissionInit missionInit)
    {
        try
        {
            this.drawContext.setOrigin(this.origin);
            this.drawContext.Draw(this.params.getDrawingDecorator(), MinecraftServer.getServer().getEntityWorld());
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void update(World world)
    {
        double dx = this.velocity.xCoord;
        double dy = this.velocity.yCoord;
        double dz = this.velocity.zCoord;
        if (this.drawContext.getMax().xCoord + dx > this.params.getCanvasBounds().getMax().getX() + 1.0 || this.drawContext.getMin().xCoord + dx < this.params.getCanvasBounds().getMin().getX())
            dx = -dx;
        if (this.drawContext.getMax().yCoord + dy > this.params.getCanvasBounds().getMax().getY() + 1.0 || this.drawContext.getMin().yCoord + dy < this.params.getCanvasBounds().getMin().getY())
            dy = -dy;
        if (this.drawContext.getMax().zCoord + dz > this.params.getCanvasBounds().getMax().getZ() + 1.0 || this.drawContext.getMin().zCoord + dz < this.params.getCanvasBounds().getMin().getZ())
            dz = -dz;
        this.velocity = new Vec3(dx, dy, dz);
        BlockPos oldpos = new BlockPos(this.origin);
        this.origin = this.origin.add(this.velocity);
        BlockPos newpos = new BlockPos(this.origin);
        if (oldpos.equals(newpos))
            return;
        try
        {
            long t1 = System.currentTimeMillis();
            this.drawContext.setOrigin(this.origin);
            this.drawContext.Draw(this.params.getDrawingDecorator(), MinecraftServer.getServer().getEntityWorld());
            long t2 = System.currentTimeMillis();
            this.drawContext.clearPrevious(MinecraftServer.getServer().getEntityWorld());
            long t3 = System.currentTimeMillis();
            System.out.println("Time: " + (t3-t1) + " (draw: " + (t2-t1) + ", clear: " + (t3-t2) + ")");
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public boolean getExtraAgentHandlers(List<Object> handlers)
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
}
