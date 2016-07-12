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

package com.microsoft.Malmo.Utils;

import java.util.List;

import javax.xml.bind.JAXBElement;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockLever.EnumOrientation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import com.microsoft.Malmo.Schemas.BlockVariant;
import com.microsoft.Malmo.Schemas.Colour;
import com.microsoft.Malmo.Schemas.DrawBlock;
import com.microsoft.Malmo.Schemas.DrawCuboid;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.DrawLine;
import com.microsoft.Malmo.Schemas.DrawSphere;
import com.microsoft.Malmo.Schemas.DrawingDecorator;
import com.microsoft.Malmo.Schemas.Facing;

/**
 *  The Mission node can specify drawing primitives, which are drawn in the world by this helper class.  
 */
public class BlockDrawingHelper
{
    /**
     * Draws the specified drawing into the Minecraft world supplied.
     * @param drawingNode The sequence of drawing primitives to draw.
     * @param world The world in which to draw them.
     * @throws Exception Unrecognised block types or primitives cause an exception to be thrown.
     */
    public static void Draw( DrawingDecorator drawingNode, World world ) throws Exception
    {
        for(JAXBElement<?> jaxbobj : drawingNode.getDrawObjectType())
        {
    		Object obj = jaxbobj.getValue();
            // isn't there an easier way of doing this?
            if( obj instanceof DrawBlock )
                DrawPrimitive( (DrawBlock)obj, world );
            else if( obj instanceof DrawItem )
                DrawPrimitive( (DrawItem)obj, world );
            else if( obj instanceof DrawCuboid )
                DrawPrimitive( (DrawCuboid)obj, world );
            else if (obj instanceof DrawSphere )
                DrawPrimitive( (DrawSphere)obj, world );
            else if (obj instanceof DrawLine )
                DrawPrimitive( (DrawLine)obj, world );
            else 
                throw new Exception("Unsupported drawing primitive: "+obj.getClass().getName() );
        }
    }
    
    /**
     * Draw a single Minecraft block.
     * @param b Contains information about the block to be drawn.
     * @param w The world in which to draw.
     * @throws Exception Throws an exception if the block type is not recognised.
     */
    private static void DrawPrimitive( DrawBlock b, World w ) throws Exception
    {
        IBlockState blockType = MinecraftTypeHelper.ParseBlockType( b.getType().value() );
        if( blockType == null )
            throw new Exception("Unrecognised block type: "+b.getType().value());
        BlockPos pos = new BlockPos( b.getX(), b.getY(), b.getZ() );
        blockType = applyModifications(blockType, b.getColour(),  b.getFace(), b.getVariant());
        w.setBlockState( pos, blockType );
    }
    
    public static IBlockState applyModifications(IBlockState blockType, Colour colour, Facing facing, BlockVariant variant )
    {
        if (blockType == null)
            return null;
        
        if (colour != null)
            blockType = applyColour(blockType, colour);
        if (facing != null)
            blockType = applyFacing(blockType, facing);
        if (variant != null)
            blockType = applyVariant(blockType, variant);

        return blockType;
    }
    
    /**
     * Draw a solid sphere made up of Minecraft blocks.
     * @param s Contains information about the sphere to be drawn.
     * @param w The world in which to draw.
     * @throws Exception Throws an exception if the block type is not recognised.
     */
    private static void DrawPrimitive( DrawSphere s, World w ) throws Exception
    {
        IBlockState blockType = MinecraftTypeHelper.ParseBlockType( s.getType().value() );
        if( blockType == null )
            throw new Exception("Unrecognised block type: " + s.getType().value());
        blockType = applyModifications(blockType, s.getColour(),  null, s.getVariant());

        int radius = s.getRadius();
        for( int x = s.getX() - radius; x <= s.getX() + radius; x++ )
        {
            for( int y = s.getY() - radius; y <= s.getY() + radius; y++ )
            {
                for( int z = s.getZ() - radius; z <= s.getZ() + radius; z++ )
                {
                    if ((z - s.getZ()) * (z - s.getZ()) + (y - s.getY()) * (y - s.getY()) + (x - s.getX()) * (x - s.getX()) <= (radius*radius))
                    {
                        BlockPos pos = new BlockPos( x, y, z );
                        w.setBlockState( pos, blockType );
                        List<Entity> entities = w.getEntitiesWithinAABBExcludingEntity(null,  new AxisAlignedBB(pos, pos).expand(0.5, 0.5, 0.5));
                        for (Entity ent : entities)
                        	if (!(ent instanceof EntityPlayer))
                        		w.removeEntity(ent);
                    }
                }
            }
        }
    }

    /**
     * Dumb code to draw a solid line made up of Minecraft blocks.<br>
     * (Doesn't do any fancy Bresenham stuff because the cost of computing the points on the line
     * presumably pales into insignificance compared to the cost of turning each point into a Minecraft block.)
     * @param l Contains information about the line to be drawn.
     * @param w The world in which to draw.
     * @throws Exception Throws an exception if the block type is not recognised.
     */
    private static void DrawPrimitive( DrawLine l, World w ) throws Exception
    {
        // Set up the blocktype for the main blocks of the line:
        IBlockState blockType = MinecraftTypeHelper.ParseBlockType( l.getType().value() );
        if( blockType == null )
            throw new Exception("Unrecognised block type: " + l.getType().value());
        blockType = applyModifications(blockType, l.getColour(),  l.getFace(), l.getVariant());

        // Set up the blocktype for the steps of the line, if one has been specified:
        IBlockState stepType = blockType;
        if (l.getSteptype() != null)
        {
            stepType = MinecraftTypeHelper.ParseBlockType( l.getSteptype().value() );
            if( stepType == null )
                throw new Exception("Unrecognised block type: " + l.getSteptype().value());
            stepType = applyModifications(stepType, l.getColour(),  l.getFace(), l.getVariant());
        }

        float dx = (l.getX2() - l.getX1());
        float dy = (l.getY2() - l.getY1());
        float dz = (l.getZ2() - l.getZ1());
        float steps = (int)Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
        if (steps < 1)
            steps = 1;
        dx /= steps;
        dy /= steps;
        dz /= steps;

        int prevY = l.getY1();
        int prevZ = l.getZ1();
        int prevX = l.getX1();
        for (int i = 0; i <= steps; i++)
        {
            int x = Math.round(l.getX1() + (float)i * dx);
            int y = Math.round(l.getY1() + (float)i * dy);
            int z = Math.round(l.getZ1() + (float)i * dz);
            BlockPos pos = new BlockPos(x, y, z);
            clearEntities(w, x-0.5,y-0.5,z-0.5,x+0.5,y+0.5,z+0.5);
            w.setBlockState(pos, y == prevY ? blockType : stepType);

            // Ensure 4-connected:
            if (x != prevX && z != prevZ)
            {
                pos = new BlockPos(x, y, prevZ);
                clearEntities(w, x-0.5,y-0.5,prevZ-0.5,x+0.5,y+0.5,prevZ+0.5);
                w.setBlockState(pos, y == prevY ? blockType : stepType);
            }
            prevY = y;
            prevX = x;
            prevZ = z;
        }
    }
    
    public static void clearEntities(World w, double x1, double y1, double z1, double x2, double y2, double z2)
    {
        List<Entity> entities = w.getEntitiesWithinAABBExcludingEntity(null,  new AxisAlignedBB(x1, y1, z1, x2, y2, z2));
        for (Entity ent : entities)
        	if (!(ent instanceof EntityPlayer))
        		w.removeEntity(ent);
    }

    /** Select the request variant of the Minecraft block, if applicable
     * @param state The block to be varied
     * @param colour The new variation
     * @return A new blockstate which is the requested variant of the original, if such a variant exists; otherwise it returns the original block.
     */
    private static IBlockState applyVariant(IBlockState state, BlockVariant variant)
    {
        for (IProperty prop : (java.util.Set<IProperty>)state.getProperties().keySet())
        {
            if (prop.getName().equals("variant") && prop.getValueClass().isEnum())
            {
                Object[] values = prop.getValueClass().getEnumConstants();
                for (Object obj : values)
                {
                    if (obj != null && obj.toString().equalsIgnoreCase(variant.value()))
                    {
                        return state.withProperty(prop, (Comparable)obj);
                    }
                }
            }
        }
        return state;
    }

    /** Recolour the Minecraft block
     * @param state The block to be recoloured
     * @param colour The new colour
     * @return A new blockstate which is a recoloured version of the original
     */
    private static IBlockState applyColour(IBlockState state, Colour colour)
    {
        for (IProperty prop : (java.util.Set<IProperty>)state.getProperties().keySet())
        {
            if (prop.getName().equals("color") && prop.getValueClass() == net.minecraft.item.EnumDyeColor.class)
            {
                net.minecraft.item.EnumDyeColor current = (net.minecraft.item.EnumDyeColor)state.getValue(prop);
                if (!current.getName().equalsIgnoreCase(colour.name()))
                {
                    return state.withProperty(prop, EnumDyeColor.valueOf(colour.name()));
                }
            }
        }
        return state;
    }
    
    /** Change the facing attribute of the Minecraft block
     * @param state The block to be edited
     * @param facing The new direction (N/S/E/W/U/D)
     * @return A new blockstate with the facing attribute edited
     */
    private static IBlockState applyFacing(IBlockState state, Facing facing)
    {
        for (IProperty prop : (java.util.Set<IProperty>)state.getProperties().keySet())
        {
            if (prop.getName().equals("facing"))
            {
            	if(prop.getValueClass() == EnumFacing.class)
            	{
            		EnumFacing current = (EnumFacing)state.getValue(prop);
	                if (!current.getName().equalsIgnoreCase(facing.name()))
	                {
	                    return state.withProperty(prop, EnumFacing.valueOf(facing.name()));
	                }
            	}
            	else if(prop.getValueClass() == EnumOrientation.class)
            	{
            		EnumOrientation current = (EnumOrientation)state.getValue(prop);
	                if (!current.getName().equalsIgnoreCase(facing.name()))
	                {
	                    return state.withProperty(prop, EnumOrientation.valueOf(facing.name()));
	                }
            	}
            }
        }
        return state;
    }

    /**
     * Spawn a single item at the specified position.
     * @param i Contains information about the item to be spawned.
     * @param w The world in which to spawn.
     * @throws Exception Throws an exception if the item type is not recognised.
     */
    private static void DrawPrimitive( DrawItem i, World w ) throws Exception
    {
        Item item = MinecraftTypeHelper.ParseItemType(i.getType());
        if (item == null)
            throw new Exception("Unrecognised item type: "+i.getType());
        BlockPos pos = new BlockPos( i.getX(), i.getY(), i.getZ() );
        placeItem(item, pos, w, true);
    }

    /** Spawn a single item at the specified position.
     * @param item the actual item to be spawned.
     * @param pos the position at which to spawn it.
     * @param world the world in which to spawn the item.
     */
    public static void placeItem(Item item, BlockPos pos, World world, boolean centreItem)
    {
    	double offset = (centreItem) ? 0.5D : 0.0D;
        ItemStack stack = new ItemStack(item);
        EntityItem entityitem = new EntityItem(world, (double)pos.getX() + offset, (double)pos.getY() + offset, (double)pos.getZ() + offset, stack);
        // Set the motions to zero to prevent random movement.
        entityitem.motionX = 0;
        entityitem.motionY = 0;
        entityitem.motionZ = 0;
        entityitem.setDefaultPickupDelay();
        world.spawnEntityInWorld(entityitem);
    }

    /**
     * Draw a filled cuboid of Minecraft blocks of a single type.
     * @param c Contains information about the cuboid to be drawn.
     * @param w The world in which to draw.
     * @throws Exception Throws an exception if the block type is not recognised.
     */
    private static void DrawPrimitive( DrawCuboid c, World w ) throws Exception
    {
        IBlockState blockType = MinecraftTypeHelper.ParseBlockType( c.getType().value() );
        if( blockType == null)
            throw new Exception("Unrecogised item type: "+c.getType().value());
        blockType = applyModifications(blockType, c.getColour(),  c.getFace(), c.getVariant());

        clearEntities(w, c.getX1(), c.getY1(), c.getZ1(), c.getX2(), c.getY2(), c.getZ2());

        int x1 = Math.min(c.getX1(), c.getX2());
        int x2 = Math.max(c.getX1(), c.getX2());
        int y1 = Math.min(c.getY1(), c.getY2());
        int y2 = Math.max(c.getY1(), c.getY2());
        int z1 = Math.min(c.getZ1(), c.getZ2());
        int z2 = Math.max(c.getZ1(), c.getZ2());
        for( int x = x1; x <= x2; x++ ) {
            for( int y = y1; y <= y2; y++ ) {
                for( int z = z1; z <= z2; z++ ) {
                    w.setBlockState( new BlockPos( x, y, z ), blockType );
                }
            }
        }
    }
}
