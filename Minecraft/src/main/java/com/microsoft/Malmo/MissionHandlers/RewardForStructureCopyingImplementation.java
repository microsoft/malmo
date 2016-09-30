package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.DrawBlockBasedObjectType;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardDensityForBuildAndBreak;
import com.microsoft.Malmo.Schemas.RewardForStructureCopying;
import com.microsoft.Malmo.Schemas.UnnamedGridDefinition;
import com.microsoft.Malmo.Utils.BlockDrawingHelper;
import com.microsoft.Malmo.Utils.BlockDrawingHelper.XMLBlockState;

public class RewardForStructureCopyingImplementation extends HandlerBase implements IRewardProducer
{
    private UnnamedGridDefinition sourceBounds;
    private UnnamedGridDefinition destBounds;
    private RewardForStructureCopying rscparams;
    private Vec3i delta;
    private int structureVolume;
    private RewardDensityForBuildAndBreak rewardDensity;
    private XMLBlockState blockTypeOnCorrectPlacement;
    private XMLBlockState blockTypeOnIncorrectPlacement;

    private float reward = 0.0F;
    private int dimension;
    private boolean structureHasBeenCompleted = false;
    private List<IBlockState> source;
    private List<IBlockState> dest;
    private int currentScore = -1;

    /**
     * Attempt to parse the given object as a set of parameters for this handler.
     *
     * @param params the parameter block to parse
     * @return true if the object made sense for this handler; false otherwise.
     */
    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof RewardForStructureCopying))
            return false;

        this.rscparams = (RewardForStructureCopying) params;

        this.sourceBounds = rscparams.getGoalStructureBounds();
        this.destBounds = rscparams.getPlayerStructureBounds();
        this.delta = new Vec3i(destBounds.getMin().getX() - sourceBounds.getMin().getX(),
                               destBounds.getMin().getY() - sourceBounds.getMin().getY(),
                               destBounds.getMin().getZ() - sourceBounds.getMin().getZ());
        
        this.structureVolume = volumeOfBounds(this.sourceBounds);
        assert(this.structureVolume == volumeOfBounds(this.destBounds));
        this.dest = new ArrayList<IBlockState>(Collections.nCopies(this.structureVolume, (IBlockState)null));
        this.source = new ArrayList<IBlockState>(Collections.nCopies(this.structureVolume, (IBlockState)null));
        
        this.rewardDensity = rscparams.getRewardDensity();

        DrawBlockBasedObjectType tickBlock = rscparams.getBlockTypeOnCorrectPlacement();
        DrawBlockBasedObjectType crossBlock = rscparams.getBlockTypeOnIncorrectPlacement();

        this.blockTypeOnCorrectPlacement = new XMLBlockState(tickBlock.getType(), tickBlock.getColour(), tickBlock.getFace(), tickBlock.getVariant());
        this.blockTypeOnIncorrectPlacement = new XMLBlockState(crossBlock.getType(), crossBlock.getColour(), crossBlock.getFace(), crossBlock.getVariant());

        this.dimension = rscparams.getDimension();
        return true;
    }

    private boolean blockInBounds(BlockPos pos, UnnamedGridDefinition bounds)
    {
        return pos.getX() >= bounds.getMin().getX() && pos.getX() < bounds.getMax().getX() &&
               pos.getZ() >= bounds.getMin().getZ() && pos.getZ() < bounds.getMax().getZ() &&
               pos.getY() >= bounds.getMin().getY() && pos.getY() < bounds.getMax().getY();
    }

    private int volumeOfBounds(UnnamedGridDefinition bounds)
    {
        return (bounds.getMax().getX() - bounds.getMin().getX()) *
               (bounds.getMax().getY() - bounds.getMin().getY()) *
               (bounds.getMax().getZ() - bounds.getMin().getZ());
    }

    /**
     * Get the reward value for the current Minecraft state.
     *
     * @param missionInit the MissionInit object for the currently running mission,
     * which may contain parameters for the reward requirements.
     * @param multidimReward
     */
    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward multidimReward)
    {
        if (this.currentScore == -1)
            this.currentScore = getSimilarityScoreAndRecolourBlocks(Minecraft.getMinecraft().theWorld); // Initialise score.

        if (this.rewardDensity == RewardDensityForBuildAndBreak.MISSION_END)
        {
            // Only send the reward at the very end of the mission.
            if (multidimReward.isFinalReward() && this.currentScore != 0)
            {
                float reward = (float)this.currentScore / (float)this.structureVolume;
                multidimReward.add(this.dimension, reward * this.rscparams.getRewardScale().floatValue());
            }
        }
        else
        {
            // Send reward immediately.
            if (this.reward != 0)
                multidimReward.add(this.dimension, this.reward);
            this.reward = 0;
        }
    }

    /**
     * Called once before the mission starts - use for any necessary initialisation.
     * @param missionInit
     */
    @Override
    public void prepare(MissionInit missionInit)
    {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);

        if (this.rscparams.getAddQuitProducer() != null)
        {
            // In order to trigger the end of the mission, we need to hook into the quit handlers.
            MissionBehaviour mb = parentBehaviour();
            final String quitDescription = this.rscparams.getAddQuitProducer().getDescription();
            mb.addQuitProducer(new IWantToQuit()
            {
                @Override
                public void prepare(MissionInit missionInit)
                {
                }
    
                @Override
                public String getOutcome()
                {
                    return quitDescription;
                }
    
                @Override
                public boolean doIWantToQuit(MissionInit missionInit)
                {
                    return RewardForStructureCopyingImplementation.this.structureHasBeenCompleted;
                }
    
                @Override
                public void cleanup()
                {
                }
            });
        }
    }

    /**
     * Called once after the mission ends - use for any necessary cleanup.
     */
    @Override
    public void cleanup() {
        MinecraftForge.EVENT_BUS.unregister(this);
        FMLCommonHandler.instance().bus().unregister(this);
        structureHasBeenCompleted = false;
    }

    private int blockPosToIndex(BlockPos pos, UnnamedGridDefinition gd)
    {
        // Flatten block pos into single dimension index.
        int depth = gd.getMax().getZ() - gd.getMin().getZ();
        int width = gd.getMax().getX() - gd.getMin().getX();
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

    private int getSimilarityScoreAndRecolourBlocks(World w)
    {
        int numExactMatchBlocks = 0;
        BlockDrawingHelper drawContext = new BlockDrawingHelper();
        drawContext.beginDrawing(w);

        for (int x = this.sourceBounds.getMin().getX(); x < this.sourceBounds.getMax().getX(); x++)
        {
            for (int y = this.sourceBounds.getMin().getY(); y < this.sourceBounds.getMax().getY(); y++)
            {
                for (int z = this.sourceBounds.getMin().getZ(); z < this.sourceBounds.getMax().getZ(); z++)
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

                    if (srcState.equals(dstState))
                    {
                        // They match.
                        numExactMatchBlocks++;
                        if (blockTypeOnCorrectPlacement != null && !w.isAirBlock(goalStructurePos))
                        {
                            // Mark both source and destination blocks for correct placement:
                            drawContext.setBlockState(w, playerStructurePos, blockTypeOnCorrectPlacement);
                            drawContext.setBlockState(w, goalStructurePos, blockTypeOnCorrectPlacement);
                        }
                    }
                    else
                    {
                        // Non-match.
                        if (!w.isAirBlock(playerStructurePos) && blockTypeOnIncorrectPlacement != null)
                        {
                            // Recolour the destination block only:
                            drawContext.setBlockState(w, playerStructurePos, blockTypeOnIncorrectPlacement);
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
        return numExactMatchBlocks;
    }

    private void updateAndCalcReward(World w)
    {
        int score = getSimilarityScoreAndRecolourBlocks(w);
        if (this.currentScore != score)
        {
            switch (this.rewardDensity)
            {
            case PER_BLOCK:
                this.reward += (score - this.currentScore) * this.rscparams.getRewardScale().floatValue();
                break;
            case MISSION_END:
                break;
            default:
                break;
            }
        }
        this.currentScore = score;
        if (this.currentScore == this.structureVolume)
        {
            if (!this.structureHasBeenCompleted && this.rewardDensity == RewardDensityForBuildAndBreak.PER_BLOCK)
            {
                // The structure has been completed - apply the reward bonus.
                // We check structureHasBeenCompleted here because we only want to do this once.
                // (Otherwise the agent can reap immense rewards by repeatedly breaking and re-adding the
                // final block.)
                this.reward += this.rscparams.getRewardForCompletion().floatValue();
            }
            this.structureHasBeenCompleted = true;
        }
    }

    @SubscribeEvent
    public void onBreakBlock(BreakEvent event)
    {
        if (blockInBounds(event.pos, this.destBounds))
        {
            this.dest.set(blockPosToIndex(event.pos, this.destBounds), Blocks.air.getDefaultState());
            updateAndCalcReward(event.world);
        }
    }
    
    @SubscribeEvent
    public void onPlaceBlock(PlaceEvent event)
    {
        if (blockInBounds(event.pos,this.destBounds))
        {
            this.dest.set(blockPosToIndex(event.pos, this.destBounds), event.state);
            updateAndCalcReward(event.world);
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        // Disallow creating or destroying events in the player structure:
        if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK && event.pos != null)
        {
            // Destroy block
            if (blockInBounds(event.pos, this.sourceBounds))
                event.setCanceled(true);
        }
        else if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && event.pos != null)
        {
            // Place block - need to work out *where* the block would be placed.
            // This code was cribbed from ItemBlock.onItemUse()
            IBlockState iblockstate = event.world.getBlockState(event.pos);
            Block block = iblockstate.getBlock();
            EnumFacing side = event.face;
            BlockPos pos = event.pos;
            if (block == Blocks.snow_layer && ((Integer)iblockstate.getValue(BlockSnow.LAYERS)).intValue() < 1)
            {
                side = EnumFacing.UP;
            }
            else if (!block.isReplaceable(event.world, pos))
            {
                pos = pos.offset(side);
            }
            if (blockInBounds(pos, this.sourceBounds))
                event.setCanceled(true);
        }
    }
}
