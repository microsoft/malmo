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

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.microsoft.Malmo.Schemas.Pos;

/** Helper functions for position-related doings.
 */
public class PositionHelper
{
	/** Calculate the Euclidean distance between the player and the target position.
	 * @param player the player whose distance from target we want to know
	 * @param targetPos the target position, specified as a Schema pos object
	 * @return a float containing the Euclidean distance 'twixt player and target
	 */
	public static float calcDistanceFromPlayerToPosition(EntityPlayerSP player, Pos targetPos)
	{
	    double x = player.posX - targetPos.getX().doubleValue();
	    double y = player.posY - targetPos.getY().doubleValue();
	    double z = player.posZ - targetPos.getZ().doubleValue();
	    return (float)Math.sqrt(x*x + y*y + z*z);
	}

	public static List<BlockPos> getTouchingBlocks(EntityPlayerSP player)
	{
		// Determine which blocks we are touching.
		// This code is adapted from Entity, where it is used to fire the Block.onEntityCollidedWithBlock methods.
		BlockPos blockposmin = new BlockPos(player.getEntityBoundingBox().minX - 0.001D, player.getEntityBoundingBox().minY - 0.001D, player.getEntityBoundingBox().minZ - 0.001D);
	    BlockPos blockposmax = new BlockPos(player.getEntityBoundingBox().maxX + 0.001D, player.getEntityBoundingBox().maxY + 0.001D, player.getEntityBoundingBox().maxZ + 0.001D);
	    List<BlockPos> blocks = new ArrayList<BlockPos>();
	    
	    if (player.world.isAreaLoaded(blockposmin, blockposmax))
	    {
	        for (int i = blockposmin.getX(); i <= blockposmax.getX(); ++i)
	        {
	            for (int j = blockposmin.getY(); j <= blockposmax.getY(); ++j)
	            {
	                for (int k = blockposmin.getZ(); k <= blockposmax.getZ(); ++k)
	                {
	                    blocks.add(new BlockPos(i, j, k));
	                }
	            }
	        }
	    }
	    return blocks;
	}

	/**
     * Finds the highest block on the x and z coordinate that is solid or liquid, and returns its y coord.
     */
    public static BlockPos getTopSolidOrLiquidBlock(World world, BlockPos pos)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        BlockPos blockpos;
        BlockPos blockpos1;

        for (blockpos = new BlockPos(pos.getX(), chunk.getTopFilledSegment() + 16, pos.getZ()); blockpos.getY() >= 0; blockpos = blockpos1)
        {
            blockpos1 = blockpos.down();
            IBlockState state = chunk.getBlockState(blockpos1);

            if ((state.getMaterial().blocksMovement() || state.getMaterial().isLiquid()) && !state.getBlock().isLeaves(state, world, blockpos1) && !state.getBlock().isFoliage(world, blockpos1))
            {
                break;
            }
        }

        return blockpos;
    }
}
