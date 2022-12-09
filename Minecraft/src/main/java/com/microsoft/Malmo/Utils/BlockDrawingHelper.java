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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import com.microsoft.Malmo.Schemas.BlockType;
import com.microsoft.Malmo.Schemas.Colour;
import com.microsoft.Malmo.Schemas.ContainedObjectType;
import com.microsoft.Malmo.Schemas.DrawBlock;
import com.microsoft.Malmo.Schemas.DrawContainer;
import com.microsoft.Malmo.Schemas.DrawCuboid;
import com.microsoft.Malmo.Schemas.DrawEntity;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.DrawLine;
import com.microsoft.Malmo.Schemas.DrawSign;
import com.microsoft.Malmo.Schemas.DrawSphere;
import com.microsoft.Malmo.Schemas.DrawingDecorator;
import com.microsoft.Malmo.Schemas.EntityTypes;
import com.microsoft.Malmo.Schemas.Facing;
import com.microsoft.Malmo.Schemas.NoteTypes;
import com.microsoft.Malmo.Schemas.ShapeTypes;
import com.microsoft.Malmo.Schemas.Variation;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;

/**
 *  The Mission node can specify drawing primitives, which are drawn in the world by this helper class.  
 */
public class BlockDrawingHelper
{
    private class StateCheck
    {
        IBlockState desiredState;
        BlockPos pos;
        List<IProperty> propertiesToCheck;
    }

    private List<StateCheck> checkList;

    /** Small class which captures an IBlockState, but also the XML values
     * which created it, if they exist.
     */
    public static class XMLBlockState
    {
        IBlockState state;
        Colour colour;
        Facing face;
        Variation variant;
        BlockType type;
 
        public XMLBlockState(IBlockState state)
        {
            this.state = state;
        }

        public XMLBlockState(BlockType type, Colour colour, Facing face, Variation variant)
        {
            this.type = type;
            IBlockState blockType = MinecraftTypeHelper.ParseBlockType(type.value());
            if (blockType != null)
            {
                blockType = applyModifications(blockType, colour, face, variant);
                this.state = blockType;
            }
            this.colour = colour;
            this.face = face;
            this.variant = variant;
        }

        public XMLBlockState(IBlockState state, Colour colour, Facing face, Variation variant)
        {
            if (state != null)
            {
                state = applyModifications(state, colour, face, variant);
                this.state = state;
            }
            this.colour = colour;
            this.face = face;
            this.variant = variant;
        }

        public Block getBlock()
        {
            return this.state != null ? this.state.getBlock() : null;
        }

        public boolean isValid()
        {
            return this.state != null;
        }
    }

    public void beginDrawing(World w)
    {
        // Any pre-drawing initialisation code here.
        this.checkList = new ArrayList<StateCheck>();
    }
    
    public void endDrawing(World w)
    {
        // Post-drawing code.
        for (StateCheck sc : this.checkList)
        {
            IBlockState stateActual = w.getBlockState(sc.pos);
            Block blockActual = stateActual.getBlock();
            Block blockDesired = sc.desiredState.getBlock();
            if (blockActual == blockDesired)
            {
                // The blocks are the same, so we can assume the block hasn't been deliberately overwritten.
                // Now check the block states:
                if (stateActual != sc.desiredState)
                {
                    if (sc.propertiesToCheck == null)
                    {
                        // No specific properties to check - just do a blanket reset.
                        w.setBlockState(sc.pos, sc.desiredState);
                    }
                    else
                    {
                        // Reset only the properties we've been asked to check:
                        for (IProperty prop : sc.propertiesToCheck)
                        {
                            stateActual = stateActual.withProperty(prop, sc.desiredState.getValue(prop));
                        }
                        w.setBlockState(sc.pos,  stateActual);
                    }
                }
            }
        }
    }

    /**
     * Draws the specified drawing into the Minecraft world supplied.
     * @param drawingNode The sequence of drawing primitives to draw.
     * @param world The world in which to draw them.
     * @throws Exception Unrecognised block types or primitives cause an exception to be thrown.
     */
    public void Draw( DrawingDecorator drawingNode, World world ) throws Exception
    {
        beginDrawing(world);

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
            else if (obj instanceof DrawEntity)
                DrawPrimitive( (DrawEntity)obj, world );
            else if (obj instanceof DrawContainer)
                DrawPrimitive( (DrawContainer)obj, world );
            else if (obj instanceof DrawSign)
                DrawPrimitive( (DrawSign)obj, world );
            else
                throw new Exception("Unsupported drawing primitive: "+obj.getClass().getName() );
        }

        endDrawing(world);
    }

    /**
     * Draw a single Minecraft block.
     * @param b Contains information about the block to be drawn.
     * @param w The world in which to draw.
     * @throws Exception Throws an exception if the block type is not recognised.
     */
    private void DrawPrimitive( DrawBlock b, World w ) throws Exception
    {
        XMLBlockState blockType = new XMLBlockState(b.getType(), b.getColour(),  b.getFace(), b.getVariant());
        if (!blockType.isValid())
            throw new Exception("Unrecogised item type: " + b.getType().value());
        BlockPos pos = new BlockPos( b.getX(), b.getY(), b.getZ() );
        clearEntities(w, b.getX(), b.getY(), b.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1);
        setBlockState(w, pos, blockType );
    }

    public static IBlockState applyModifications(IBlockState blockType, Colour colour, Facing facing, Variation variant )
    {
        if (blockType == null)
            return null;
        
        if (colour != null)
            blockType = MinecraftTypeHelper.applyColour(blockType, colour);
        if (facing != null)
            blockType = MinecraftTypeHelper.applyFacing(blockType, facing);
        if (variant != null)
            blockType = MinecraftTypeHelper.applyVariant(blockType, variant);

        return blockType;
    }
    
    /**
     * Draw a solid sphere made up of Minecraft blocks.
     * @param s Contains information about the sphere to be drawn.
     * @param w The world in which to draw.
     * @throws Exception Throws an exception if the block type is not recognised.
     */
    private void DrawPrimitive( DrawSphere s, World w ) throws Exception
    {
        XMLBlockState blockType = new XMLBlockState(s.getType(), s.getColour(), null, s.getVariant());
        if (!blockType.isValid())
            throw new Exception("Unrecognised block type: " + s.getType().value());
 
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
                        setBlockState( w, pos, blockType );
                        AxisAlignedBB aabb = new AxisAlignedBB(pos, new BlockPos(x+1, y+1, z+1));
                        clearEntities(w, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
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
    private void DrawPrimitive( DrawLine l, World w ) throws Exception
    {
        // Set up the blocktype for the main blocks of the line:
        XMLBlockState blockType = new XMLBlockState(l.getType(), l.getColour(), l.getFace(), l.getVariant());
        if (!blockType.isValid())
            throw new Exception("Unrecognised block type: " + l.getType().value());

        // Set up the blocktype for the steps of the line, if one has been specified:
        XMLBlockState stepType = blockType;
        if (l.getSteptype() != null)
        {
            stepType = new XMLBlockState(l.getSteptype(), l.getColour(), l.getFace(), l.getVariant());
            if (!stepType.isValid())
                throw new Exception("Unrecognised block type: " + l.getSteptype().value());
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
            clearEntities(w, x, y, z, x + 1, y + 1, z + 1);
            setBlockState(w, pos, y == prevY ? blockType : stepType);

            // Ensure 4-connected:
            if (x != prevX && z != prevZ)
            {
                pos = new BlockPos(x, y, prevZ);
                clearEntities(w, x, y, prevZ, x + 1, y + 1, prevZ + 1);
                setBlockState(w, pos, y == prevY ? blockType : stepType);
            }
            prevY = y;
            prevX = x;
            prevZ = z;
        }
    }
    
    public void clearEntities(World w, double x1, double y1, double z1, double x2, double y2, double z2)
    {
        List<Entity> entities = w.getEntitiesWithinAABBExcludingEntity(null,  new AxisAlignedBB(x1, y1, z1, x2, y2, z2));
        for (Entity ent : entities)
            if (!(ent instanceof EntityPlayer))
                w.removeEntity(ent);
    }

    /**
     * Spawn a single item at the specified position.
     * @param i Contains information about the item to be spawned.
     * @param w The world in which to spawn.
     * @throws Exception Throws an exception if the item type is not recognised.
     */
    private void DrawPrimitive( DrawItem i, World w ) throws Exception
    {
        ItemStack item = MinecraftTypeHelper.getItemStackFromDrawItem(i);
        if (item == null)
            throw new Exception("Unrecognised item type: "+i.getType());
        BlockPos pos = new BlockPos( i.getX(), i.getY(), i.getZ() );
        placeItem(item, pos, w, true);
    }
    
    /** Spawn a single entity at the specified position.
     * @param e the actual entity to be spawned.
     * @param w the world in which to spawn the entity.
     * @throws Exception
     */
    private void DrawPrimitive( DrawEntity e, World w ) throws Exception
    {
        String oldEntityName = e.getType().getValue();
        String id = null;
        for (EntityEntry ent : net.minecraftforge.fml.common.registry.ForgeRegistries.ENTITIES)
        {
           if (ent.getName().equals(oldEntityName))
           {
               id = ent.getRegistryName().toString();
               break;
           }
        }
        if (id == null)
            return;

        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setString("id", id);
        nbttagcompound.setBoolean("PersistenceRequired", true); // Don't let this entity despawn
        Entity entity;
        try
        {
            entity = EntityList.createEntityFromNBT(nbttagcompound, w);
            if (entity != null)
            {
                positionEntity(entity, e.getX().doubleValue(), e.getY().doubleValue(), e.getZ().doubleValue(), e.getYaw().floatValue(), e.getPitch().floatValue());
                entity.setVelocity(e.getXVel().doubleValue(), e.getYVel().doubleValue(), e.getZVel().doubleValue());
                // Set all the yaw values imaginable:
                if (entity instanceof EntityLivingBase)
                {
                    ((EntityLivingBase)entity).rotationYaw = e.getYaw().floatValue();
                    ((EntityLivingBase)entity).prevRotationYaw = e.getYaw().floatValue();
                    ((EntityLivingBase)entity).prevRotationYawHead = e.getYaw().floatValue();
                    ((EntityLivingBase)entity).rotationYawHead = e.getYaw().floatValue();
                    ((EntityLivingBase)entity).prevRenderYawOffset = e.getYaw().floatValue();
                    ((EntityLivingBase)entity).renderYawOffset = e.getYaw().floatValue();
                }
                w.getBlockState(entity.getPosition());  // Force-load the chunk if necessary, to ensure spawnEntity will work.
                if (!w.spawnEntity(entity))
                {
                    System.out.println("WARNING: Failed to spawn entity! Chunk not loaded?");
                }
            }
        }
        catch (RuntimeException runtimeexception)
        {
            // Cannot summon this entity.
            throw new Exception("Couldn't create entity type: " + e.getType().getValue());
        }
    }

    protected void DrawPrimitive( DrawContainer c, World w ) throws Exception
    {
        // First, draw the container block:
        String cType = c.getType().value();
        BlockType bType = BlockType.fromValue(cType); // Safe - ContainerType is a subset of BlockType
        XMLBlockState blockType = new XMLBlockState(bType, c.getColour(), c.getFace(), c.getVariant());
        if (!blockType.isValid())
            throw new Exception("Unrecogised item type: " + c.getType().value());
        BlockPos pos = new BlockPos( c.getX(), c.getY(), c.getZ() );
        setBlockState(w, pos, blockType );
        // Now fill the container:
        TileEntity tileentity = w.getTileEntity(pos);
        if (tileentity instanceof TileEntityLockableLoot)
        {
            // First clear out any leftovers:
            ((TileEntityLockableLoot)tileentity).clear();
            int index = 0;
            for (ContainedObjectType cot : c.getObject())
            {
                DrawItem di  = new DrawItem();
                di.setColour(cot.getColour());
                di.setType(cot.getType());
                di.setVariant(cot.getVariant());
                ItemStack stack = MinecraftTypeHelper.getItemStackFromDrawItem(di);
                stack.setCount(cot.getQuantity());
                ((TileEntityLockableLoot)tileentity).setInventorySlotContents(index, stack);
                index++;
            }
        }
    }

    protected void DrawPrimitive( DrawSign s, World w ) throws Exception
    {
        String sType = s.getType().value();
        BlockType bType = BlockType.fromValue(sType); // Safe - SignType is a subset of BlockType
        XMLBlockState blockType = new XMLBlockState(bType, s.getColour(), s.getFace(), s.getVariant());
        BlockPos pos = new BlockPos( s.getX(), s.getY(), s.getZ() );
        setBlockState(w, pos, blockType );
        if (blockType.type == BlockType.STANDING_SIGN && s.getRotation() != null)
        {
            IBlockState placedBlockState = w.getBlockState(pos);
            if (placedBlockState != null)
            {
                Block placedBlock = placedBlockState.getBlock();
                if (placedBlock != null)
                {
                    IBlockState rotatedBlock = placedBlock.getStateFromMeta(s.getRotation());
                    w.setBlockState(pos, rotatedBlock);
                }
            }
        }
        TileEntity tileentity = w.getTileEntity(pos);
        if (tileentity instanceof TileEntitySign)
        {
            TileEntitySign sign = (TileEntitySign)tileentity;
            if (s.getLine1() != null)
                sign.signText[0] = new TextComponentString(s.getLine1());
            if (s.getLine2() != null)
                sign.signText[1] = new TextComponentString(s.getLine2());
            if (s.getLine3() != null)
                sign.signText[2] = new TextComponentString(s.getLine3());
            if (s.getLine4() != null)
                sign.signText[3] = new TextComponentString(s.getLine4());
        }
    }

    protected void positionEntity( Entity entity, double x, double y, double z, float yaw, float pitch )
    {
        entity.setLocationAndAngles(x, y, z, yaw, pitch);
    }
    
    /** Spawn a single item at the specified position.
     * @param item the actual item to be spawned.
     * @param pos the position at which to spawn it.
     * @param world the world in which to spawn the item.
     */
    public void placeItem(ItemStack stack, BlockPos pos, World world, boolean centreItem)
    {
        EntityItem entityitem = createItem(stack, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), world, centreItem);
        // Set the motions to zero to prevent random movement.
        entityitem.motionX = 0;
        entityitem.motionY = 0;
        entityitem.motionZ = 0;
        entityitem.setDefaultPickupDelay();
        world.spawnEntity(entityitem);
    }

    protected EntityItem createItem(ItemStack stack, double x, double y, double z, World w, boolean centreItem)
    {
        if (centreItem)
        {
            x = ((int)x) + 0.5;
            y = ((int)y) + 0.5;
            z = ((int)z) + 0.5;
        }
        return new EntityItem(w, x, y, z, stack);
    }
    /**
     * Draw a filled cuboid of Minecraft blocks of a single type.
     * @param c Contains information about the cuboid to be drawn.
     * @param w The world in which to draw.
     * @throws Exception Throws an exception if the block type is not recognised.
     */
    private void DrawPrimitive( DrawCuboid c, World w ) throws Exception
    {
        XMLBlockState blockType = new XMLBlockState(c.getType(), c.getColour(), c.getFace(), c.getVariant());
        if (!blockType.isValid())
            throw new Exception("Unrecogised item type: "+c.getType().value());

        int x1 = Math.min(c.getX1(), c.getX2());
        int x2 = Math.max(c.getX1(), c.getX2());
        int y1 = Math.min(c.getY1(), c.getY2());
        int y2 = Math.max(c.getY1(), c.getY2());
        int z1 = Math.min(c.getZ1(), c.getZ2());
        int z2 = Math.max(c.getZ1(), c.getZ2());

        clearEntities(w, x1, y1, z1, x2 + 1, y2 + 1, z2 + 1);

        for( int x = x1; x <= x2; x++ ) {
            for( int y = y1; y <= y2; y++ ) {
                for( int z = z1; z <= z2; z++ ) {
                    BlockPos pos = new BlockPos(x, y, z);
                    setBlockState(w, pos, blockType);
                }
            }
        }
    }

    public void setBlockState(World w, BlockPos pos, XMLBlockState state)
    {
        if (!state.isValid())
            return;

        // Do some depressingly necessary specific stuff here for different block types:
        if (state.getBlock() instanceof BlockRailBase && state.variant != null)
        {
            // Caller has specified a variant - is it a shape variant?
            try
            {
                ShapeTypes shape = ShapeTypes.fromValue(state.variant.getValue());
                if (shape != null)
                {
                    // Yes, user has requested a particular shape.
                    // Minecraft won't honour this - and, worse, it may get altered by neighbouring pieces that are added later.
                    // So we don't bother trying to get this right now - we add it as a state check, and set it correctly
                    // after drawing has finished.
                    StateCheck sc = new StateCheck();
                    sc.pos = pos;
                    sc.desiredState = state.state;
                    sc.propertiesToCheck = new ArrayList<IProperty>();
                    sc.propertiesToCheck.add(((BlockRailBase)state.getBlock()).getShapeProperty());
                    this.checkList.add(sc);
                }
            }
            catch (IllegalArgumentException e)
            {
                // Wasn't a shape variation. Ignore.
            }
        }

        // Actually set the block state into the world:
        w.setBlockState(pos, state.state);

        // And now do the necessary post-placement processing:
        if (state.type == BlockType.MOB_SPAWNER)
        {
            TileEntity te = w.getTileEntity(pos);
            if (te != null && te instanceof TileEntityMobSpawner)   // Ought to be!
            {
                // Attempt to use the variation to control what type of mob this spawns:
                try
                {
                    EntityTypes entvar = EntityTypes.fromValue(state.variant.getValue());
                    ((TileEntityMobSpawner)te).getSpawnerBaseLogic().setEntityId(new ResourceLocation(entvar.value()));
                }
                catch (Exception e)
                {
                    // Do nothing - user has requested a non-entity variant.
                }
            }
        }
        if (state.type == BlockType.NOTEBLOCK)
        {
            TileEntity te = w.getTileEntity(pos);
            if (te != null && te instanceof TileEntityNote && state.variant != null)
            {
                try
                {
                    NoteTypes note = NoteTypes.fromValue(state.variant.getValue());
                    if (note != null)
                    {
                        // User has requested a particular note.
                        ((TileEntityNote)te).note = (byte)note.ordinal();
                    }
                }
                catch (IllegalArgumentException e)
                {
                    // Wasn't a note variation. Ignore.
                }
            }
        }
    }
}
