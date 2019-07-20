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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.AgentSection;
import com.microsoft.Malmo.Schemas.BlockType;
import com.microsoft.Malmo.Schemas.Colour;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.PosAndDirection;
import com.microsoft.Malmo.Schemas.SnakeBlock;
import com.microsoft.Malmo.Schemas.SnakeDecorator;
import com.microsoft.Malmo.Schemas.Variation;
import com.microsoft.Malmo.Utils.BlockDrawingHelper;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;
import com.microsoft.Malmo.Utils.SeedHelper;

public class SnakeDecoratorImplementation extends HandlerBase implements IWorldDecorator
{
	private int buildX = -1;
	private int buildY = -1;
	private int buildZ = -1;
	private int buildDirection = 0;
	private int stairs = 0;
	private int timeSinceLastBuild = 0;
	private Random randomBuilder;
	private Random randomBlocks;
	ArrayList<BlockPos> path = new ArrayList<BlockPos>();
	private boolean pendingBlock = false;
	private IBlockState freshBlock = null;
	private IBlockState staleBlock = null;
	private int consecutiveGaps = 0;

	// Parameters:
	private int speedInTicks = 6;
	private int minYPos = 32;
	private int maxYPos = 250;
	private float chanceOfChangeOfDirection = 0.01f;
	private float chanceOfStairs = 0.08f;
	private float chanceOfGap = 0.04f;
	private int maxNumberOfStairs = 20;
	private int maxPathLength = 30;
	private String freshBlockName = "glowstone";
	private String staleBlockName = "air";
	private SnakeDecorator snakeParams;
	
	public SnakeDecoratorImplementation()
	{
		if(randomBuilder == null)
			randomBuilder = SeedHelper.getRandom();
		if(randomBlocks == null)
			randomBlocks = SeedHelper.getRandom();
			
		Block fresh = (Block)Block.REGISTRY.getObject(new ResourceLocation(this.freshBlockName));
		Block stale = (Block)Block.REGISTRY.getObject(new ResourceLocation(this.staleBlockName));
		this.freshBlock = (fresh != null) ? fresh.getDefaultState() : Blocks.GLOWSTONE.getDefaultState();
		this.staleBlock = (stale != null) ? stale.getDefaultState() : Blocks.AIR.getDefaultState();
	}
	
    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof SnakeDecorator))
            return false;
        this.snakeParams = (SnakeDecorator)params;
        return true;
    }
	
	@Override
	public void update(World world)
	{
		this.timeSinceLastBuild++;
		if (this.timeSinceLastBuild > this.speedInTicks && !this.pendingBlock)
			updatePath();

		if (this.path.size() > 0 && this.pendingBlock)
		{
    		BlockPos bp = this.path.get(this.path.size() - 1);
    		// Create the block, or a gap if we are leaving a gap:
       		world.setBlockState(bp, this.consecutiveGaps == 0 ? this.freshBlock : Blocks.AIR.getDefaultState());
			this.pendingBlock = false;
    		
    		// Create space above and below this block (even if we are leaving a gap):
			BlockPos bpUp = bp;
			BlockPos bpDown = bp;
    		for (int i = 0; i < 3; i++) {
        		bpUp = bpUp.add(0, 1, 0);
        		bpDown = bpDown.add(0, -1, 0);
        		world.setBlockToAir(bpUp);
        		world.setBlockToAir(bpDown);
    		}
   		
    		// Now remove block at the other end of the path, if need be:
    		if (this.path.size() > this.maxPathLength) {
    			bp = this.path.remove(0);
    			world.setBlockState(bp, this.staleBlock);
    		}
    	}
	}
	
	private void updatePath()
	{
		this.pendingBlock = true;
		this.timeSinceLastBuild = 0;

		// Update block position:
		this.buildX += ((this.buildDirection % 2) == 0) ? this.buildDirection - 1 : 0;
		this.buildZ += ((this.buildDirection % 2) == 1) ? this.buildDirection - 2 : 0;

		// We can add a gap, unless we've already added one, or we are going up:
		boolean addGap = (this.consecutiveGaps == 0 && this.stairs <= 0 && this.randomBuilder.nextFloat() < chanceOfGap);

		// Update the Y position:
		if (this.stairs > 0)
		{
			this.buildY++;
			this.stairs--;
		}
		else if (this.stairs < 0)
		{
			this.buildY--;
			this.stairs++;
		}

		// Clamp Y:
		this.buildY = (this.buildY < this.minYPos) ? this.minYPos : (this.buildY > this.maxYPos) ? this.maxYPos : this.buildY;

		// Add the block to our path:
		BlockPos bp = new BlockPos(this.buildX, this.buildY, this.buildZ);
		this.path.add(bp);

		if (!addGap)
		{
			this.consecutiveGaps = 0;
			
			// Update the deltas randomly:
			scrambleDirections();
		}
		else
		{
			this.consecutiveGaps++;
		}
	}
	
	private void scrambleDirections()
	{
		if (this.randomBuilder.nextFloat() < this.chanceOfStairs)
			this.stairs = this.randomBuilder.nextInt(1 + this.maxNumberOfStairs * 2) - this.maxNumberOfStairs;
		
		if (this.randomBuilder.nextFloat() < this.chanceOfChangeOfDirection)
		{
			this.buildDirection += this.randomBuilder.nextInt(3) - 1;
			if (this.buildDirection < 0)
				this.buildDirection += 4;
			else if (this.buildDirection > 3)
				this.buildDirection -= 4;
		}
	}

    private void initBlocks()
    {
	    this.freshBlock = getBlock(this.snakeParams.getFreshBlock(), this.randomBlocks);
	    this.staleBlock = getBlock(this.snakeParams.getStaleBlock(), this.randomBlocks);
    }

    private void initRNGs()
    {
        // Initialise a RNG for this scene:
        long seed = 0;
        if (this.snakeParams.getSeed() == null || this.snakeParams.getSeed().equals("random"))
            seed = System.currentTimeMillis();
        else
            seed = Long.parseLong(this.snakeParams.getSeed());

        this.randomBuilder = new Random(seed);
        this.randomBlocks = new Random(seed);
        
        // Should we initialise a separate RNG for the block types?
        if (this.snakeParams.getMaterialSeed() != null)
        {
            long bseed = 0;
            if (this.snakeParams.getMaterialSeed().equals("random"))
                bseed = System.currentTimeMillis();
            else
                bseed = Long.parseLong(this.snakeParams.getMaterialSeed());
            this.randomBlocks = new Random(bseed);
        }
    }

    private IBlockState getBlock(SnakeBlock sblock, Random rand)
    {
        String blockName = chooseBlock(sblock.getType(), rand);
        Colour blockCol = chooseColour(sblock.getColour(), rand);
        Variation blockVar = chooseVariant(sblock.getVariant(), rand);
        return BlockDrawingHelper.applyModifications(MinecraftTypeHelper.ParseBlockType(blockName), blockCol, null, blockVar);
    }
    
    private String chooseBlock(List<BlockType> types, Random r)
    {
        if (types == null || types.size() == 0)
            return "air";
        return types.get(r.nextInt(types.size())).value();
    }
    
    private Colour chooseColour(List<Colour> colours, Random r)
    {
        if (colours == null || colours.size() == 0)
            return null;
        return colours.get(r.nextInt(colours.size()));
    }
    
    private Variation chooseVariant(List<Variation> vars, Random r)
    {
        if (vars == null || vars.size() == 0)
            return null;
        return vars.get(r.nextInt(vars.size()));
    }
    
	@Override
	public void buildOnWorld(MissionInit missionInit, World world)
	{
		initRNGs();
		initBlocks();
		initDimensionsAndBehaviour();
		setStartPoint(missionInit);
	}
	
	private void setStartPoint(MissionInit missionInit)
	{
        // Position the start point:
        PosAndDirection p = new PosAndDirection();
        p.setX(new BigDecimal(this.buildX));
        p.setY(new BigDecimal(this.buildY));
        p.setZ(new BigDecimal(this.buildZ));
        for (AgentSection as : missionInit.getMission().getAgentSection())
        {
	        p.setPitch(as.getAgentStart().getPlacement().getPitch());
	        p.setYaw(as.getAgentStart().getPlacement().getYaw());
	        as.getAgentStart().setPlacement(p);
        }
	}
	
	private float perturbProbability(float prob, float variance, Random rng)
	{
	    prob += (rng.nextFloat() - 0.5) * 2 * variance;
	    prob = prob < 0 ? 0 : (prob > 1 ? 1 : prob);
	    return prob;
	}

	private void initDimensionsAndBehaviour()
    {
    	// Get dimensions of snake:
        this.buildX = this.snakeParams.getSizeAndPosition().getXOrigin();
        this.buildY = this.snakeParams.getSizeAndPosition().getYOrigin();
        this.buildZ = this.snakeParams.getSizeAndPosition().getZOrigin();
        this.maxYPos = this.snakeParams.getSizeAndPosition().getYMax();
        this.minYPos = this.snakeParams.getSizeAndPosition().getYMin();
        
        this.chanceOfChangeOfDirection = perturbProbability(this.snakeParams.getTurnProbability().getValue().floatValue(), this.snakeParams.getTurnProbability().getVariance().floatValue(), this.randomBuilder);
        this.chanceOfGap = perturbProbability(this.snakeParams.getGapProbability().getValue().floatValue(), this.snakeParams.getGapProbability().getVariance().floatValue(), this.randomBuilder);
        this.chanceOfStairs = perturbProbability(this.snakeParams.getStairsProbability().getValue().floatValue(), this.snakeParams.getStairsProbability().getVariance().floatValue(), this.randomBuilder);

        this.speedInTicks = this.snakeParams.getSpeedInTicks();
        this.maxNumberOfStairs = this.snakeParams.getMaxStairLength();
        this.maxPathLength = this.snakeParams.getMaxLength();
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