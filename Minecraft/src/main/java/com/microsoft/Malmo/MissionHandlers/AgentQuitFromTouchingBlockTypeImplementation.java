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

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.MissionHandlers.RewardForCollectingItemImplementation.GainItemEvent;
import com.microsoft.Malmo.Schemas.AgentQuitFromTouchingBlockType;
import com.microsoft.Malmo.Schemas.BlockSpec;
import com.microsoft.Malmo.Schemas.BlockSpecWithDescription;
import com.microsoft.Malmo.Schemas.BlockType;
import com.microsoft.Malmo.Schemas.Variation;
import com.microsoft.Malmo.Schemas.Colour;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.PositionHelper;

public class AgentQuitFromTouchingBlockTypeImplementation extends HandlerBase implements IWantToQuit
{
	AgentQuitFromTouchingBlockType params;
	List<String> blockTypeNames;
	String quitCode = "";
	boolean wantToQuit = false;

	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof AgentQuitFromTouchingBlockType))
			return false;
		
		this.params = (AgentQuitFromTouchingBlockType)params;
		// Flatten all the possible block type names for ease of matching later:
		this.blockTypeNames = new ArrayList<String>();
		for (BlockSpec bs : this.params.getBlock())
		{
			for (BlockType bt : bs.getType())
			{
				Block b = Block.getBlockFromName(bt.value());
				this.blockTypeNames.add(b.getUnlocalizedName().toLowerCase());
			}
		}
		return true;
	}

    @SubscribeEvent
    public void onDiscretePartialMoveEvent(DiscreteMovementCommandsImplementation.DiscretePartialMoveEvent event)
    {
        this.wantToQuit = doIWantToQuit(null);
    }
	
	@Override
	public boolean doIWantToQuit(MissionInit missionInit)
	{
	    if (this.wantToQuit)
	        return true;
	    
		EntityPlayerSP player = Minecraft.getMinecraft().player;
        List<BlockPos> touchingBlocks = PositionHelper.getTouchingBlocks(player);
        for (BlockPos pos : touchingBlocks)
        {
        	IBlockState bs = player.world.getBlockState(pos);
        	// Does this block match our trigger specs?
        	String blockname = bs.getBlock().getUnlocalizedName().toLowerCase();
        	if (!this.blockTypeNames.contains(blockname))
        		continue;
        	
    		// The type matches one of our block types, so now we need to perform additional checks.
        	for (BlockSpecWithDescription blockspec : this.params.getBlock())
        	{
        		if (findMatch(blockspec, bs))
        		{
        			this.quitCode = blockspec.getDescription();
        			return true;	// Yes, we want to quit!
        		}
        	}
        }
        return false;	// Nothing matched, we can quit happily.
	}
	
	private boolean findNameMatch(BlockSpec blockspec, String blockName)
	{
		for (BlockType bt : blockspec.getType())
		{
			Block b = Block.getBlockFromName(bt.value());
			if (b.getUnlocalizedName().equalsIgnoreCase(blockName))
				return true;
		}
		return false;
	}
	
	private boolean findColourMatch(BlockSpec blockspec, String blockColour)
	{
		if (blockspec.getColour() == null || blockspec.getColour().isEmpty())
			return true;	// If nothing to match against, we pass.
		for (Colour c : blockspec.getColour())
		{
			if (c.value().equalsIgnoreCase(blockColour))
				return true;
		}
		return false;
	}
	
	private boolean findVariantMatch(BlockSpec blockspec, String blockVariant)
	{
		if (blockspec.getVariant() == null || blockspec.getVariant().isEmpty())
			return true;	// If nothing to match against, we pass.
		for (Variation v : blockspec.getVariant())
		{
			if (v.getValue().equalsIgnoreCase(blockVariant))
				return true;
		}
		return false;
	}
	
	private boolean findMatch(BlockSpec blockspec, IBlockState blockstate)
	{
		// Firstly, do the block types match at all?
    	String blockname = blockstate.getBlock().getUnlocalizedName().toLowerCase();
    	if (!findNameMatch(blockspec, blockname))
    		return false;	// Block name wasn't found in this block type.

    	// Next, check for a colour match:
		net.minecraft.item.EnumDyeColor blockColour = null;
		for (IProperty prop : blockstate.getProperties().keySet())
		{
			if (prop.getName().equals("color") && prop.getValueClass() == net.minecraft.item.EnumDyeColor.class)
			{
				blockColour = (net.minecraft.item.EnumDyeColor)blockstate.getValue(prop);
			}
		}
		if (blockColour != null && !findColourMatch(blockspec, blockColour.getName()))
			return false;	// Colours didn't match.

		// Now check for the variant match:
		Object blockVariant = null;
        for (IProperty prop : blockstate.getProperties().keySet())
        {
            if (prop.getName().equals("variant") && prop.getValueClass().isEnum())
            {
            	blockVariant = blockstate.getValue(prop);
            }
        }
        if (blockVariant != null && !findVariantMatch(blockspec, blockVariant.toString()))
        	return false;
			
		// If we've got here, then we have a total match.
		return true;
	}
	
    @Override
    public void prepare(MissionInit missionInit)
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void cleanup()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
	public String getOutcome()
	{
		return this.quitCode;
	}
}
