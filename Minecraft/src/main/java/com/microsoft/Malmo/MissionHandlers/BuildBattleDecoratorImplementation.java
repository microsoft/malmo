package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
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

import com.microsoft.Malmo.MalmoMod;
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

    @Override
    public void buildOnWorld(MissionInit missionInit) throws DecoratorException
    {
        // Does nothing at the moment, though we could add an option to draw the bounds of the arenas here,
        // if it seems to be something people want.
    }

    @Override
    public boolean getExtraAgentHandlers(List<Object> handlers)
    {
        return false;
    }

    @Override
    public void update(World world)
    {
        if (!this.valid)
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
        if (blockInBounds(event.pos, this.destBounds))
        {
            this.valid  = false;
            this.dest.set(blockPosToIndex(event.pos, this.destBounds), Blocks.air.getDefaultState());
        }
    }

    @SubscribeEvent
    public void onPlaceBlock(PlaceEvent event)
    {
        if (blockInBounds(event.pos,this.destBounds))
        {
            this.valid = false;
            this.dest.set(blockPosToIndex(event.pos, this.destBounds), event.state);
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

    @Override
    public void prepare(MissionInit missionInit)
    {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        updateAndScorePlayerVolume(MinecraftServer.getServer().getEntityWorld(), false);
    }

    @Override
    public void cleanup()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
        FMLCommonHandler.instance().bus().unregister(this);
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