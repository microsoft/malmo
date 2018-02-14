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
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.DrawBlock;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.ObservationFromDistance;
import com.microsoft.Malmo.Schemas.ObservationFromRay;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

public class ObservationFromRayImplementation extends HandlerBase implements IObservationProducer
{
    private ObservationFromRay ofrparams;
    
    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof ObservationFromRay))
            return false;
        
        this.ofrparams = (ObservationFromRay)params;
        return true;
    }

    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        buildMouseOverData(json, this.ofrparams.isIncludeNBT());
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
    public static void buildMouseOverData(JsonObject json, boolean includeNBTData)
    {
        // We could use Minecraft.getMinecraft().objectMouseOver but it's limited to the block reach distance, and
        // doesn't register floating tile items.
        float partialTicks = 0; // Ideally use Minecraft.timer.renderPartialTicks - but we don't need sub-tick resolution.
        Entity viewer = Minecraft.getMinecraft().player;
        float depth = 50;   // Hard-coded for now - in future will be parameterised via the XML.
        Vec3d eyePos = viewer.getPositionEyes(partialTicks);
        Vec3d lookVec = viewer.getLook(partialTicks);
        Vec3d searchVec = eyePos.addVector(lookVec.xCoord * depth, lookVec.yCoord * depth, lookVec.zCoord * depth);
        RayTraceResult mop = Minecraft.getMinecraft().world.rayTraceBlocks(eyePos, searchVec, false, false, false);
        RayTraceResult mopEnt = findEntity(eyePos, lookVec, depth, mop, true);
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
        if (mop.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            // We're looking at a block - send block data:
            jsonMop.addProperty("hitType", "block");
            jsonMop.addProperty("x", mop.hitVec.xCoord);
            jsonMop.addProperty("y", mop.hitVec.yCoord);
            jsonMop.addProperty("z", mop.hitVec.zCoord);
            IBlockState state = Minecraft.getMinecraft().world.getBlockState(mop.getBlockPos());
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
            // Add the NBTTagCompound, if this is a tile entity.
            if (includeNBTData)
            {
                TileEntity tileentity = Minecraft.getMinecraft().world.getTileEntity(mop.getBlockPos());
                if (tileentity != null)
                {
                    NBTTagCompound data = tileentity.getUpdateTag();
                    if (data != null)
                    {
                        // Turn data directly into json and add it to what we're already returning.
                        String jsonString = data.toString();
                        try
                        {
                            JsonElement jelement = new JsonParser().parse(jsonString);
                            if (jelement != null)
                                jsonMop.add("NBTTagCompound", jelement);
                        }
                        catch (JsonSyntaxException e)
                        {
                            // Duff NBTTagCompound - ignore it.
                        }
                    }
                }
            }
            jsonMop.addProperty("inRange", hitDist <= blockReach);
            jsonMop.addProperty("distance", hitDist);
        }
        else if (mop.typeOfHit == RayTraceResult.Type.ENTITY)
        {
            // Looking at an entity:
            Entity entity = mop.entityHit;
            if (entity != null)
            {
                jsonMop.addProperty("x", entity.posX);
                jsonMop.addProperty("y", entity.posY);
                jsonMop.addProperty("z", entity.posZ);
                jsonMop.addProperty("yaw",  entity.rotationYaw);
                jsonMop.addProperty("pitch",  entity.rotationPitch);
                String name = MinecraftTypeHelper.getUnlocalisedEntityName(entity);
                String hitType = "entity";
                if (entity instanceof EntityItem)
                {
                    ItemStack is = ((EntityItem)entity).getEntityItem();
                    DrawItem di = MinecraftTypeHelper.getDrawItemFromItemStack(is);
                    if (di.getColour() != null)
                        jsonMop.addProperty("colour", di.getColour().value());
                    if (di.getVariant() != null)
                        jsonMop.addProperty("variant", di.getVariant().getValue());
                    jsonMop.addProperty("stackSize", is.getCount());
                    name = di.getType();
                    hitType = "item";
                }
                jsonMop.addProperty("type", name);
                jsonMop.addProperty("hitType", hitType);
            }
            jsonMop.addProperty("inRange", hitDist <= entityReach);
            jsonMop.addProperty("distance", hitDist);
        }
        json.add("LineOfSight", jsonMop);
    }

    static RayTraceResult findEntity(Vec3d eyePos, Vec3d lookVec, double depth, RayTraceResult mop, boolean includeTiles)
    {
        // Based on code in EntityRenderer.getMouseOver()
        if (mop != null)
            depth = mop.hitVec.distanceTo(eyePos);
        Vec3d searchVec = eyePos.addVector(lookVec.xCoord * depth, lookVec.yCoord * depth, lookVec.zCoord * depth);
        Entity pointedEntity = null;

        Vec3d hitVec = null;
        Entity viewer = Minecraft.getMinecraft().player;
        List<?> list = Minecraft.getMinecraft().world.getEntitiesWithinAABBExcludingEntity(viewer, viewer.getEntityBoundingBox().addCoord(lookVec.xCoord * depth, lookVec.yCoord * depth, lookVec.zCoord * depth).expand(1.0, 1.0, 1.0));
        double distance = depth;

        for (int i = 0; i < list.size(); ++i)
        {
            Entity entity = (Entity)list.get(i);
            if (entity.canBeCollidedWith() || includeTiles)
            {
                float border = entity.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().expand((double)border, (double)border, (double)border);
                RayTraceResult movingobjectposition = axisalignedbb.calculateIntercept(eyePos, searchVec);
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
                        if (entity == entity.getRidingEntity() && !entity.canRiderInteract())
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
            RayTraceResult newMop = new RayTraceResult(pointedEntity, hitVec);
            return newMop;
        }
        return null;
    }
}
