package com.microsoft.Malmo.Utils;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AnimationDrawingHelper extends BlockDrawingHelper
{
    Vec3d origin = new Vec3d(0,0,0);
    Set<BlockPos> drawing = new HashSet<BlockPos>();
    Set<BlockPos> previousFrame;
    Vec3d minPos;
    Vec3d maxPos;

    @Override
    public void beginDrawing(World w)
    {
        this.previousFrame = new HashSet<BlockPos>(this.drawing);
        this.drawing = new HashSet<BlockPos>();
        this.minPos = null;
        this.maxPos = null;
        super.beginDrawing(w);
    }

    public void endDrawing(World w)
    {
        super.endDrawing(w);
    }

    public void clearPrevious(World w)
    {
        if (this.previousFrame != null)
        {
            for (BlockPos pos : this.previousFrame)
            {
                w.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            }
        }
        this.previousFrame = null;
    }

    public Vec3d getMin()
    {
        return this.minPos;
    }

    public Vec3d getMax()
    {
        return this.maxPos;
    }

    public Vec3d getOrigin()
    {
        return this.origin;
    }

    public void setOrigin(Vec3d org)
    {
        this.origin = org;
    }

    @Override
    public void setBlockState(World w, BlockPos pos, XMLBlockState state)
    {
        BlockPos offsetPos = pos.add(this.origin.xCoord, this.origin.yCoord, this.origin.zCoord);
        this.drawing.add(offsetPos);
        this.previousFrame.remove(offsetPos);
        if (this.minPos == null)
            this.minPos = new Vec3d(offsetPos.getX() - 0.5, offsetPos.getY(), offsetPos.getZ() - 0.5);
        else
        {
            double x = Math.min(this.minPos.xCoord, offsetPos.getX() - 0.5);
            double y = Math.min(this.minPos.yCoord, offsetPos.getY() - 0.5);
            double z = Math.min(this.minPos.zCoord, offsetPos.getZ() - 0.5);
            if (x != this.minPos.xCoord || y != this.minPos.yCoord || z != this.minPos.zCoord)
                this.minPos = new Vec3d(x,y,z);
        }
        if (this.maxPos == null)
            this.maxPos = new Vec3d(offsetPos.getX() + 0.5, offsetPos.getY() + 1, offsetPos.getZ() + 0.5);
        else
        {
            double x = Math.max(this.maxPos.xCoord, offsetPos.getX() + 0.5);
            double y = Math.max(this.maxPos.yCoord, offsetPos.getY() + 0.5);
            double z = Math.max(this.maxPos.zCoord, offsetPos.getZ() + 0.5);
            if (x != this.maxPos.xCoord || y != this.maxPos.yCoord || z != this.maxPos.zCoord)
                this.maxPos = new Vec3d(x,y,z);
        }
        super.setBlockState(w, offsetPos, state);
    }

    @Override
    public void clearEntities(World w, double x1, double y1, double z1, double x2, double y2, double z2)
    {
        super.clearEntities(w, x1+this.origin.xCoord, y1+this.origin.yCoord, z1+this.origin.zCoord, x2+this.origin.xCoord, y2+this.origin.yCoord, z2+this.origin.zCoord);
    }

    @Override
    protected EntityItem createItem(ItemStack stack, double x, double y, double z, World w, boolean centreItem)
    {
        return super.createItem(stack, x+this.origin.xCoord, y+this.origin.yCoord, z+this.origin.zCoord, w, centreItem);
    }

    @Override
    protected void positionEntity( Entity entity, double x, double y, double z, float yaw, float pitch )
    {
        super.positionEntity(entity, x+this.origin.xCoord, y+this.origin.yCoord, z+this.origin.zCoord, yaw, pitch);
    }
}