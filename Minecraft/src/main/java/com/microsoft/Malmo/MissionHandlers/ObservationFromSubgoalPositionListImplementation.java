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

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.ObservationFromSubgoalPositionList;
import com.microsoft.Malmo.Schemas.PointWithToleranceAndDescription;

public class ObservationFromSubgoalPositionListImplementation extends HandlerBase implements IObservationProducer
{
    private int subgoalIndex = 0;
    private ObservationFromSubgoalPositionList positions;

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof ObservationFromSubgoalPositionList))
            return false;

        this.positions = (ObservationFromSubgoalPositionList)params;
        return true;
    }

    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        int nTargets = this.positions.getPoint().size();
        boolean foundNextPoint = false;
        double targetx = 0;
        double targetz = 0;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null)
            return; // Nothing we can do.

        double sourcex = player.posX;
        double sourcez = player.posZ;

        while (this.subgoalIndex < nTargets && !foundNextPoint)
        {
            targetx = this.positions.getPoint().get(this.subgoalIndex).getX().doubleValue();
            targetz = this.positions.getPoint().get(this.subgoalIndex).getZ().doubleValue();
            double tol = this.positions.getPoint().get(this.subgoalIndex).getTolerance().doubleValue();

            if (Math.abs(targetx-sourcex) + Math.abs(targetz-sourcez) < tol)
                this.subgoalIndex++;
            else
                foundNextPoint = true;
        }

        if (!foundNextPoint)
            return; // Finished.

        // Calculate which way we need to turn in order to point towards the target:
        double dx = (targetx - sourcex);
        double dz = (targetz - sourcez);
        double targetYaw = (Math.atan2(dz, dx) * 180.0/Math.PI) - 90;
        double sourceYaw = player.rotationYaw;
        // Find shortest angular distance between the two yaws, preserving sign:
        double difference = targetYaw - sourceYaw;
        while (difference < -180)
            difference += 360;
        while (difference > 180)
            difference -= 360;
        // Normalise:
        difference /= 180.0;
        json.addProperty("yawDelta",  difference);
        PointWithToleranceAndDescription point = this.positions.getPoint().get(this.subgoalIndex);
        JsonObject pointElement = new JsonObject();
        pointElement.addProperty("XPos", point.getX().doubleValue());
        pointElement.addProperty("YPos", point.getY().doubleValue());
        pointElement.addProperty("ZPos",  point.getZ().doubleValue());
        pointElement.addProperty("description", point.getDescription());
        json.add("nextSubgoal", pointElement);
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
