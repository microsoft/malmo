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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Blueprint.BlockBlueprint;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.BuildBattleDecorator;
import com.microsoft.Malmo.Schemas.DrawBlockBasedObjectType;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.UnnamedGridDefinition;
import com.microsoft.Malmo.Utils.BlockDrawingHelper;
import com.microsoft.Malmo.Utils.BlockDrawingHelper.XMLBlockState;

public class BuildBattleDecoratorImplementation extends HandlerBase implements IWorldDecorator
{
    private UnnamedGridDefinition sourceBounds;
    private UnnamedGridDefinition destBounds;
    private BuildBattleDecorator params;
    private Vec3i delta;
    private int structureVolume;
    private XMLBlockState blockTypeOnCorrectPlacement;
    private XMLBlockState blockTypeOnIncorrectPlacement;

    private boolean structureHasBeenCompleted = false;
    private List<IBlockState> source;
    private List<IBlockState> dest;
    private int currentScore = 0;
    private boolean valid = true;
    private boolean initialised = false;
    private HashMap<BlockPos, IBlockState> structureMap = new HashMap<BlockPos, IBlockState>();
    private HashMap<Integer, IBlockState> invalidBlocksMap = new HashMap<Integer, IBlockState>();

    /**
     * Attempt to parse the given object as a set of parameters for this handler.
     *
     * @param params the parameter block to parse
     * @return true if the object made sense for this handler; false otherwise.
     */
    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof BuildBattleDecorator))
            return false;

        this.params = (BuildBattleDecorator) params;

        invalidBlocksMap.put(2, Blocks.COBBLESTONE.getDefaultState());
        invalidBlocksMap.put(3, Blocks.GLASS.getDefaultState());
        invalidBlocksMap.put(4, Blocks.LOG.getDefaultState());
        invalidBlocksMap.put(5, Blocks.PLANKS.getDefaultState());
        invalidBlocksMap.put(6, Blocks.STONE.getDefaultState());
        invalidBlocksMap.put(7, Blocks.STONEBRICK.getDefaultState());
        invalidBlocksMap.put(8, Blocks.WOOL.getDefaultState());
        invalidBlocksMap.put(9, Blocks.DIRT.getDefaultState());

        this.sourceBounds = this.params.getGoalStructureBounds();
        this.destBounds = this.params.getPlayerStructureBounds();
        this.delta = new Vec3i(destBounds.getMin().getX() - sourceBounds.getMin().getX(),
                               destBounds.getMin().getY() - sourceBounds.getMin().getY(),
                               destBounds.getMin().getZ() - sourceBounds.getMin().getZ());
        
        this.structureVolume = volumeOfBounds(this.sourceBounds);
        assert(this.structureVolume == volumeOfBounds(this.destBounds));
        this.dest = new ArrayList<IBlockState>(Collections.nCopies(this.structureVolume, (IBlockState)null));
        this.source = new ArrayList<IBlockState>(Collections.nCopies(this.structureVolume, (IBlockState)null));
        
        DrawBlockBasedObjectType tickBlock = this.params.getBlockTypeOnCorrectPlacement();
        DrawBlockBasedObjectType crossBlock = this.params.getBlockTypeOnIncorrectPlacement();

        this.blockTypeOnCorrectPlacement = (tickBlock != null) ? new XMLBlockState(tickBlock.getType(), tickBlock.getColour(), tickBlock.getFace(), tickBlock.getVariant()) : null;
        this.blockTypeOnIncorrectPlacement = (crossBlock != null) ? new XMLBlockState(crossBlock.getType(), crossBlock.getColour(), crossBlock.getFace(), crossBlock.getVariant()) : null;
        return true;
    }

    private IBlockState getBlueprintBlockState(IBlockState blockState) {
        BlockBlueprint.EnumBlockType blockType = this.getBlueprintBlockType(blockState);
        IBlockState blueprintBlockState = BlockBlueprint.BLOCKS.get(blockType)
            .getDefaultState();
        //    .withProperty(BlockBlueprint.VARIANT, blockType);
        return blueprintBlockState;
    }

    private BlockBlueprint.EnumBlockType getBlueprintBlockType(IBlockState blockState) {
        String blockName = Block.REGISTRY
            .getNameForObject(blockState.getBlock())
            .getResourcePath();
        BlockBlueprint.EnumBlockType blockType = 
            BlockBlueprint.EnumBlockType.fromString(blockName);
        return blockType;
    }

    private void createBlueprintBlock(World world, BlockPos sp, BlockPos dp) {
        IBlockState sourceBlockState = this.getDestBlockState(world, sp);
        if (!(sourceBlockState.getBlock() instanceof BlockAir)) {
            IBlockState blueprintBlockState = this.getBlueprintBlockState(sourceBlockState);
            world.setBlockState(dp, blueprintBlockState, 3);
        }
    }

    @Override
    public void buildOnWorld(MissionInit missionInit, World world) throws DecoratorException
    {
        // this.buildBlockToGhostMap();
        // BlockDrawingHelper drawContext = new BlockDrawingHelper();
        // drawContext.beginDrawing(world);

        for (int x = sourceBounds.getMin().getX(); x < sourceBounds.getMax().getX(); x++) {
            for (int y = Math.max(0, sourceBounds.getMin().getY()); y <= sourceBounds.getMax().getY(); y++) {
                for (int z = sourceBounds.getMin().getZ(); z < sourceBounds.getMax().getZ(); z++) {
                    BlockPos sp = new BlockPos(x, y, z);
                    BlockPos dp = sp.add(this.delta);
                    try {
                        this.structureMap.put(dp, this.getBlueprintBlockState(this.getDestBlockState(world, sp)));
                        if (y == 1) {
                            world.setBlockState(dp, Blocks.DIRT.getDefaultState(), 3);
                        } else {
                            this.createBlueprintBlock(world, sp, dp);
                        }
                    } catch (Exception e) {
                        world.setBlockState(dp, this.getDestBlockState(world, sp), 3);
                    }
                }
            }
        }

        // drawContext.endDrawing(world);
    }

    @Override
    public boolean getExtraAgentHandlersAndData(List<Object> handlers, Map<String, String> data)
    {
        return false;
    }

    @Override
    public void update(World world)
    {
    	if (!this.initialised)
    	{
			this.initialised = true;
			updateAndScorePlayerVolume(world, false);
    	}
    	else if (!this.valid)
            updateAndScorePlayerVolume(world, true);
    }

    private boolean blockInBounds(BlockPos pos, UnnamedGridDefinition bounds)
    {
        return pos.getX() >= bounds.getMin().getX() && pos.getX() <= bounds.getMax().getX() &&
               pos.getZ() >= bounds.getMin().getZ() && pos.getZ() <= bounds.getMax().getZ() &&
               pos.getY() >= bounds.getMin().getY() && pos.getY() <= bounds.getMax().getY();
    }

    private int volumeOfBounds(UnnamedGridDefinition bounds)
    {
        return (1 + bounds.getMax().getX() - bounds.getMin().getX()) *
               (1 + bounds.getMax().getY() - bounds.getMin().getY()) *
               (1 + bounds.getMax().getZ() - bounds.getMin().getZ());
    }

    private int blockPosToIndex(BlockPos pos, UnnamedGridDefinition gd)
    {
        // Flatten block pos into single dimension index.
        int depth = 1 + gd.getMax().getZ() - gd.getMin().getZ();
        int width = 1 + gd.getMax().getX() - gd.getMin().getX();
        int ind = (pos.getX() - gd.getMin().getX()) +
               (pos.getZ() - gd.getMin().getZ()) * width +
               (pos.getY() - gd.getMin().getY()) * width * depth;
        return ind;
    }
    
    private IBlockState getSourceBlockState(World w, BlockPos pos)
    {
        int ind = blockPosToIndex(pos, this.sourceBounds);

        if (ind < 0 || ind >= this.structureVolume)
            return null;    // Out of bounds.
        
        IBlockState state = this.source.get(ind);
        if (state == null)
        {
            state = w.getBlockState(pos);
            this.source.set(ind, state);
        }
        return state;
    }

    private IBlockState getDestBlockState(World w, BlockPos pos)
    {
        int ind = blockPosToIndex(pos, this.destBounds);
        if (ind < 0 || ind >= this.structureVolume)
            return null;    // Out of bounds.

        IBlockState state = this.dest.get(ind);
        if (state == null)
        {
            state = w.getBlockState(pos);
            this.dest.set(ind, state);
        }
        return state;
    }

    private boolean isBlockValid(World w, BlockPos pos)
    {
        try {

            IBlockState sourceState = getSourceBlockState(w, pos.subtract(this.delta));
            IBlockState testBlock = getDestBlockState(w, pos);

            int sourceBlueprintId = this.getBlueprintBlockType(sourceState).getBlockId();
            int testBlueprintId = this.getBlueprintBlockType(testBlock).getBlockId();

            return sourceBlueprintId == testBlueprintId;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    private void updateAndScorePlayerVolume(World w, boolean updateReward)
    {
        int wrongBlocks = 0;
        int rightBlocks = 0;
        int totalMatchingBlocks = 0;
        BlockDrawingHelper drawContext = new BlockDrawingHelper();
        drawContext.beginDrawing(w);

        for (int x = this.sourceBounds.getMin().getX(); x <= this.sourceBounds.getMax().getX(); x++)
        {
            for (int y = this.sourceBounds.getMin().getY(); y <= this.sourceBounds.getMax().getY(); y++)
            {
                for (int z = this.sourceBounds.getMin().getZ(); z <= this.sourceBounds.getMax().getZ(); z++)
                {
                    BlockPos goalStructurePos = new BlockPos(x, y, z);
                    BlockPos playerStructurePos = goalStructurePos.add(this.delta);
                    // We don't compare the world's block states, since we re-colour them to give
                    // feedback on right / wrong blocks.
                    // Instead, query our internal representations:
                    IBlockState srcState = getSourceBlockState(w, goalStructurePos);
                    IBlockState dstState = getDestBlockState(w, playerStructurePos);
                    if (srcState == null || dstState == null)
                        continue;   // Shouldn't happen unless we've had an out-of-bounds error somehow.
                    boolean destAir = w.isAirBlock(playerStructurePos);
                    if (srcState.equals(dstState))
                    {
                        // They match. We count this if the dest block is not air.
                        if (!destAir)
                            rightBlocks++;
                        if (blockTypeOnCorrectPlacement != null && !w.isAirBlock(goalStructurePos))
                        {
                            // Mark both source and destination blocks for correct placement:
                            drawContext.setBlockState(w, playerStructurePos, blockTypeOnCorrectPlacement);
                            drawContext.setBlockState(w, goalStructurePos, blockTypeOnCorrectPlacement);
                        }
                        totalMatchingBlocks++;
                    }
                    else
                    {
                        // Non-match. We call this wrong if the dest block is not air.
                        if (!destAir)
                        {
                            wrongBlocks++;
                            if (blockTypeOnIncorrectPlacement != null)
                            {
                                // Recolour the destination block only:
                                drawContext.setBlockState(w, playerStructurePos, blockTypeOnIncorrectPlacement);
                            }
                        }
                        // Check the source block - if it was previously correct, and has become incorrect,
                        // then we will need to reset the world's blockstate:
                        IBlockState actualState = w.getBlockState(goalStructurePos);
                        if (!actualState.equals(srcState))
                            drawContext.setBlockState(w, goalStructurePos, new XMLBlockState(srcState));
                    }
                }
            }
        }
        drawContext.endDrawing(w);
        int score = rightBlocks - wrongBlocks;
        boolean sendData = false;
        boolean sendCompletionBonus = false;
        int reward = 0;

        if (updateReward && score != this.currentScore)
        {
            reward = score - this.currentScore;
            sendData = true;
        }
        this.currentScore = score;

        if (totalMatchingBlocks == this.structureVolume)
        {
            if (!this.structureHasBeenCompleted)
            {
                // The structure has been completed - send the reward bonus.
                // We check structureHasBeenCompleted here because we only want to do this once.
                // (Otherwise the agent can game the rewards by repeatedly breaking and re-adding the
                // final block.)
                if (updateReward)
                    sendCompletionBonus = true;
            }
            this.structureHasBeenCompleted = true;
        }
        this.valid = true;

        if (sendData)
        {
            HashMap<String,String> data = new HashMap<String, String>();
            data.put("reward", Integer.toString(reward));
            data.put("completed", Boolean.toString(sendCompletionBonus));
            MalmoMod.safeSendToAll(MalmoMessageType.SERVER_BUILDBATTLEREWARD, data);
        }
    }

    @SubscribeEvent
    public void onBreakBlock(BreakEvent event)
    {
        if (blockInBounds(event.getPos(), this.destBounds))
        {
            this.valid  = false;
            this.dest.set(blockPosToIndex(event.getPos(), this.destBounds), Blocks.AIR.getDefaultState());
        }
    }

    @SubscribeEvent
    public void onPlaceBlock(PlaceEvent event)
    {
        if (blockInBounds(event.getPos() ,this.destBounds))
        {
            this.valid = false;
            this.dest.set(blockPosToIndex(event.getPos(), this.destBounds), event.getState());

            if (this.structureMap.containsKey(event.getPos()) && !this.isBlockValid(event.getWorld(), event.getPos())) {
                int blockId = this.getBlueprintBlockType(event.getState()).getBlockId();
                event.getWorld().setBlockState(event.getPos(), this.invalidBlocksMap.get(blockId));
            }
        }
    }

    @SubscribeEvent
    public void onHarvestDrops(HarvestDropsEvent event) {
        if (blockInBounds(event.getPos(), this.destBounds)) {
            BlockPos sp = event.getPos().subtract(this.delta);
            if (this.structureMap.containsKey(event.getPos())) {
                event.getWorld().setBlockState(event.getPos(), this.structureMap.get(event.getPos()));
                // return;
            }

            try {
                this.createBlueprintBlock(event.getWorld(), sp, event.getPos());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        // Disallow creating or destroying events in the player structure:
        if (event instanceof PlayerInteractEvent.LeftClickBlock)
        {
            // Destroy block
            if (blockInBounds(event.getPos(), this.sourceBounds))
                event.setCanceled(true);
        }
        else if (event instanceof PlayerInteractEvent.RightClickBlock)
        {
            // Place block - need to work out *where* the block would be placed.
            // This code was cribbed from ItemBlock.onItemUse()
            IBlockState iblockstate = event.getWorld().getBlockState(event.getPos());
            Block block = iblockstate.getBlock();
            EnumFacing side = event.getFace();
            BlockPos pos = event.getPos();
            if (block == Blocks.SNOW_LAYER && ((Integer)iblockstate.getValue(BlockSnow.LAYERS)).intValue() < 1)
            {
                side = EnumFacing.UP;
            }
            else if (!block.isReplaceable(event.getWorld(), pos))
            {
                pos = pos.offset(side);
            }
            if (blockInBounds(pos, this.sourceBounds))
                event.setCanceled(true);
        }
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
