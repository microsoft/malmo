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
import java.util.Random;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.AnimationDecorator;
import com.microsoft.Malmo.Schemas.AnimationDecorator.Linear;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.AnimationDrawingHelper;
import com.microsoft.Malmo.Utils.EvaluationHelper;
import com.microsoft.Malmo.Utils.SeedHelper;

/** WorldBuilder that takes a DrawingDecorator object and animates it.<br>
 */
public class AnimationDecoratorImplementation extends HandlerBase implements IWorldDecorator
{
    AnimationDecorator params = null;
    AnimationDrawingHelper drawContext = new AnimationDrawingHelper();
    Vec3d origin;
    Vec3d velocity;
    Vec3d minCanvas;
    Vec3d maxCanvas;
    int frameCount = 0;
    int tickCounter = 0;
    Random rng;

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof AnimationDecorator))
            return false;

        this.params = (AnimationDecorator) params;

        // Initialise starting positions / velocities:
        if (this.params.getLinear() != null)
        {
            Linear linear = this.params.getLinear();
            this.origin = new Vec3d(linear.getInitialPos().getX().doubleValue(), linear.getInitialPos().getY().doubleValue(), linear.getInitialPos().getZ().doubleValue());
            this.velocity = new Vec3d(linear.getInitialVelocity().getX().doubleValue(), linear.getInitialVelocity().getY().doubleValue(), linear.getInitialVelocity().getZ().doubleValue());
            this.minCanvas = new Vec3d(linear.getCanvasBounds().getMin().getX(), linear.getCanvasBounds().getMin().getY(), linear.getCanvasBounds().getMin().getZ());
            this.maxCanvas = new Vec3d(linear.getCanvasBounds().getMax().getX(), linear.getCanvasBounds().getMax().getY(), linear.getCanvasBounds().getMax().getZ());
        }
        else
        {
            // Initialise a RNG:
            long seed = 0;
            String strSeed = this.params.getParametric().getSeed();
            if (strSeed == null || strSeed == "" || strSeed.equals("random"))
                seed = SeedHelper.getRandom().nextLong();
            else
                seed = Long.parseLong(strSeed);
            this.rng = new Random(seed);
            try
            {
                double x = EvaluationHelper.eval(this.params.getParametric().getX(), 0, this.rng);
                double y = EvaluationHelper.eval(this.params.getParametric().getY(), 0, this.rng);
                double z = EvaluationHelper.eval(this.params.getParametric().getZ(), 0, this.rng);
                this.origin = new Vec3d(x, y, z);
            }
            catch (Exception e)
            {
                // Malformed equations.
                System.out.println("ERROR: malformed equations in animation - check these:");
                System.out.println("        " + this.params.getParametric().getX());
                System.out.println("        " + this.params.getParametric().getY());
                System.out.println("        " + this.params.getParametric().getZ());
            }
        }
        return true;
    }

    @Override
    public void buildOnWorld(MissionInit missionInit, World world) throws DecoratorException
    {
        if (this.origin == null)
            throw new DecoratorException("Origin not specified - check syntax of equations?");
        try
        {
            this.drawContext.setOrigin(this.origin);
            this.drawContext.Draw(this.params.getDrawingDecorator(), world);
        }
        catch (Exception e)
        {
            throw new DecoratorException("Error trying to build animation - " + e.getMessage());
        }
    }

    @Override
    public void update(World world)
    {
        this.tickCounter++;
        if (this.tickCounter < this.params.getTicksPerUpdate())
        {
            this.tickCounter++;
            return;
        }
        this.frameCount++;
        this.tickCounter = 0;
        BlockPos oldpos = new BlockPos(this.origin);
        if (this.params.getLinear() != null)
        {
            Linear linear = this.params.getLinear();
            double dx = this.velocity.xCoord;
            double dy = this.velocity.yCoord;
            double dz = this.velocity.zCoord;
            if (this.drawContext.getMax().xCoord + dx > linear.getCanvasBounds().getMax().getX() + 1.0 || this.drawContext.getMin().xCoord + dx < linear.getCanvasBounds().getMin().getX())
                dx = -dx;
            if (this.drawContext.getMax().yCoord + dy > linear.getCanvasBounds().getMax().getY() + 1.0 || this.drawContext.getMin().yCoord + dy < linear.getCanvasBounds().getMin().getY())
                dy = -dy;
            if (this.drawContext.getMax().zCoord + dz > linear.getCanvasBounds().getMax().getZ() + 1.0 || this.drawContext.getMin().zCoord + dz < linear.getCanvasBounds().getMin().getZ())
                dz = -dz;
            this.velocity = new Vec3d(dx, dy, dz);
            this.origin = this.origin.add(this.velocity);
        }
        else
        {
            try
            {
                double x = EvaluationHelper.eval(this.params.getParametric().getX(), this.frameCount, this.rng);
                double y = EvaluationHelper.eval(this.params.getParametric().getY(), this.frameCount, this.rng);
                double z = EvaluationHelper.eval(this.params.getParametric().getZ(), this.frameCount, this.rng);
                this.origin = new Vec3d(x, y, z);
            }
            catch (Exception e)
            {
                // Just fail and move on.
                System.out.println("ERROR - check syntax of equations for animation.");
            }
        }
        BlockPos newpos = new BlockPos(this.origin);
        if (oldpos.equals(newpos))
            return;
        try
        {
            this.drawContext.setOrigin(this.origin);
            this.drawContext.Draw(this.params.getDrawingDecorator(), world);
            this.drawContext.clearPrevious(world);
        }
        catch (Exception e)
        {
            System.out.println("ERROR - can not draw animation.");
        }
    }

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
