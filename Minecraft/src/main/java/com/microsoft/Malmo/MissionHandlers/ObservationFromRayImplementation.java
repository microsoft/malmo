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

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.DrawBlock;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

public class ObservationFromRayImplementation extends HandlerBase implements IObservationProducer
{
    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        buildMouseOverData(json);
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
    }

    @Override
    public void cleanup()
    {
    }

    /** Build the json object for the object under the cursor, whether it is a block or a creature.<br>
     * If there is any data to be returned, the json will be added in a subnode called "LineOfSight".
     * @param json a JSON object into which the info for the object under the mouse will be added.
     */
    public static void buildMouseOverData(JsonObject json)
    {
        // We could use Minecraft.getMinecraft().objectMouseOver but it's limited to the block reach distance, and
        // doesn't register floating tile items.
        float partialTicks = 0; // Ideally use Minecraft.timer.renderPartialTicks - but we don't need sub-tick resolution.
        Entity viewer = Minecraft.getMinecraft().thePlayer;
        float depth = 50;   // Hard-coded for now - in future will be parameterised via the XML.
        Vec3 eyePos = viewer.getPositionEyes(partialTicks);
        Vec3 lookVec = viewer.getLook(partialTicks);
        Vec3 searchVec = eyePos.addVector(lookVec.xCoord * depth, lookVec.yCoord * depth, lookVec.zCoord * depth);
        MovingObjectPosition mop = Minecraft.getMinecraft().theWorld.rayTraceBlocks(eyePos, searchVec, false, false, false);
        MovingObjectPosition mopEnt = findEntity(eyePos, lookVec, depth, mop, true);
        if (mopEnt != null)
            mop = mopEnt;
        if (mop == null)
        {
            return; // Nothing under the mouse.
        }
        // Calculate ranges for player interaction:
        double hitDist = mop.hitVec.distanceTo(eyePos);
        double blockReach = Minecraft.getMinecraft().playerController.getBlockReachDistance();
        double entityReach = Minecraft.getMinecraft().playerController.extendedReach() ? 6.0 : 3.0;

        JsonObject jsonMop = new JsonObject();
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
        {
            // We're looking at a block - send block data:
            jsonMop.addProperty("hitType", "block");
            jsonMop.addProperty("x", mop.hitVec.xCoord);
            jsonMop.addProperty("y", mop.hitVec.yCoord);
            jsonMop.addProperty("z", mop.hitVec.zCoord);
            IBlockState state = Minecraft.getMinecraft().theWorld.getBlockState(mop.getBlockPos());
            List<IProperty> extraProperties = new ArrayList<IProperty>();
            DrawBlock db = MinecraftTypeHelper.getDrawBlockFromBlockState(state, extraProperties);
            jsonMop.addProperty("type", db.getType().value());
            if (db.getColour() != null)
                jsonMop.addProperty("colour", db.getColour().value());
            if (db.getVariant() != null)
                jsonMop.addProperty("variant", db.getVariant().getValue());
            if (db.getFace() != null)
                jsonMop.addProperty("facing",  db.getFace().value());
            if (extraProperties.size() > 0)
            {
                // Add the extra properties that aren't covered by colour/variant/facing.
                for (IProperty prop : extraProperties)
                {
                    String key = "prop_" + prop.getName();
                    if (prop.getValueClass() == Boolean.class)
                        jsonMop.addProperty(key, Boolean.valueOf(state.getValue(prop).toString()));
                    else if (prop.getValueClass() == Integer.class)
                        jsonMop.addProperty(key, Integer.valueOf(state.getValue(prop).toString()));
                    else
                        jsonMop.addProperty(key, state.getValue(prop).toString());
                }
            }
            jsonMop.addProperty("inRange", hitDist <= blockReach);
        }
        else if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY)
        {
            // Looking at an entity:
            Entity entity = mop.entityHit;
            if (entity != null)
            {
                jsonMop.addProperty("x", entity.posX);
                jsonMop.addProperty("y", entity.posY);
                jsonMop.addProperty("z", entity.posZ);
                String name = entity.getName();
                String hitType = "entity";
                if (entity instanceof EntityItem)
                {
                    ItemStack is = ((EntityItem)entity).getEntityItem();
                    DrawItem di = MinecraftTypeHelper.getDrawItemFromItemStack(is);
                    if (di.getColour() != null)
                        jsonMop.addProperty("colour", di.getColour().value());
                    if (di.getVariant() != null)
                        jsonMop.addProperty("variant", di.getVariant().getValue());
                    jsonMop.addProperty("stackSize", is.stackSize);
                    name = di.getType();
                    hitType = "item";
                }
                jsonMop.addProperty("type", name);
                jsonMop.addProperty("hitType", hitType);
            }
            jsonMop.addProperty("inRange", hitDist <= entityReach);
        }
        json.add("LineOfSight", jsonMop);
    }

    static MovingObjectPosition findEntity(Vec3 eyePos, Vec3 lookVec, double depth, MovingObjectPosition mop, boolean includeTiles)
    {
        // Based on code in EntityRenderer.getMouseOver()
        if (mop != null)
            depth = mop.hitVec.distanceTo(eyePos);
        Vec3 searchVec = eyePos.addVector(lookVec.xCoord * depth, lookVec.yCoord * depth, lookVec.zCoord * depth);
        Entity pointedEntity = null;

        Vec3 hitVec = null;
        Entity viewer = Minecraft.getMinecraft().thePlayer;
        List<?> list = Minecraft.getMinecraft().theWorld.getEntitiesWithinAABBExcludingEntity(viewer, viewer.getEntityBoundingBox().addCoord(lookVec.xCoord * depth, lookVec.yCoord * depth, lookVec.zCoord * depth).expand(1.0, 1.0, 1.0));
        double distance = depth;

        for (int i = 0; i < list.size(); ++i)
        {
            Entity entity = (Entity)list.get(i);
            if (entity.canBeCollidedWith() || includeTiles)
            {
                float border = entity.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().expand((double)border, (double)border, (double)border);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(eyePos, searchVec);
                if (axisalignedbb.isVecInside(eyePos))
                {
                    // If entity is right inside our head?
                    if (distance >= 0)
                    {
                        pointedEntity = entity;
                        hitVec = (movingobjectposition == null) ? eyePos : mop.hitVec;
                        distance = 0.0D;
                    }
                }
                else if (movingobjectposition != null)
                {
                    double distToEnt = eyePos.distanceTo(movingobjectposition.hitVec);
                    if (distToEnt < distance || distance == 0.0D)
                    {
                        if (entity == entity.ridingEntity && !entity.canRiderInteract())
                        {
                            if (distance == 0.0D)
                            {
                                pointedEntity = entity;
                                hitVec = movingobjectposition.hitVec;
                            }
                        }
                        else
                        {
                            pointedEntity = entity;
                            hitVec = movingobjectposition.hitVec;
                            distance = distToEnt;
                        }
                    }
                }
            }
        }
        if (pointedEntity != null && (distance < depth || mop == null))
        {
            MovingObjectPosition newMop = new MovingObjectPosition(pointedEntity, hitVec);
            return newMop;
        }
        return null;
    }
}
