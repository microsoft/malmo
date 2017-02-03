package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.DrawBlockBasedObjectType;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.MovingTargetDecorator;
import com.microsoft.Malmo.Schemas.Pos;
import com.microsoft.Malmo.Schemas.UnnamedGridDefinition;
import com.microsoft.Malmo.Utils.BlockDrawingHelper;
import com.microsoft.Malmo.Utils.BlockDrawingHelper.XMLBlockState;

public class MovingTargetDecoratorImplementation extends HandlerBase implements IWorldDecorator
{
    private Random rng;
    private MovingTargetDecorator targetParams;
    private UnnamedGridDefinition arenaBounds;
    private XMLBlockState blockType;
    private ArrayDeque<BlockPos> path = new ArrayDeque<BlockPos>();
    private ArrayDeque<IBlockState> originalPath = new ArrayDeque<IBlockState>();
    private BlockPos startPos;
    private int timeSinceLastUpdate = 0;
    private int speedInTicks = 10;
    private int pathSize = 2;

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof MovingTargetDecorator))
            return false;
        this.targetParams = (MovingTargetDecorator)params;
        this.arenaBounds = this.targetParams.getArenaBounds();
        DrawBlockBasedObjectType targetBlock = this.targetParams.getBlockType();
        this.blockType = (targetBlock != null) ? new XMLBlockState(targetBlock.getType(), targetBlock.getColour(), targetBlock.getFace(), targetBlock.getVariant()) : null;
        Pos pos = this.targetParams.getStartPos();
        int xPos = pos.getX().intValue();
        int yPos = pos.getY().intValue();
        int zPos = pos.getZ().intValue();
        // Check start pos lies within arena:
        xPos = Math.min(this.arenaBounds.getMax().getX(), Math.max(this.arenaBounds.getMin().getX(),  xPos));
        yPos = Math.min(this.arenaBounds.getMax().getY(), Math.max(this.arenaBounds.getMin().getY(),  yPos));
        zPos = Math.min(this.arenaBounds.getMax().getZ(), Math.max(this.arenaBounds.getMin().getZ(),  zPos));
        this.startPos = new BlockPos(xPos, yPos, zPos);
        createRNG();
        return true;
    }

    @Override
    public void buildOnWorld(MissionInit missionInit) throws DecoratorException
    {
        this.path.add(this.startPos);
        World world = MinecraftServer.getServer().getEntityWorld();
        this.originalPath.add(world.getBlockState(this.startPos));
        BlockDrawingHelper drawContext = new BlockDrawingHelper();
        drawContext.beginDrawing(world);
        drawContext.setBlockState(world, this.startPos, this.blockType);
        drawContext.endDrawing(world);
    }

    @Override
    public boolean getExtraAgentHandlers(List<Object> handlers)
    {
        return false;
    }

    @Override
    public void update(World world)
    {
        this.timeSinceLastUpdate++;
        if (this.timeSinceLastUpdate < this.speedInTicks)
            return;
        this.timeSinceLastUpdate = 0;
        BlockPos posHead = this.path.peekFirst();
        BlockPos posTail = this.path.peekLast();
        // For now, only move in 2D - can make this more flexible later.
        ArrayList<BlockPos> possibleMovesForward = new ArrayList<BlockPos>();
        ArrayList<BlockPos> possibleMovesBackward = new ArrayList<BlockPos>();
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                if (z != 0 && x != 0)
                    continue;   // No diagonal moves.
                if (z == 0 && x == 0)
                    continue;   // Don't allow no-op.
                // Check this is a valid move...
                BlockPos candidateHeadPos = new BlockPos(posHead.getX() + x, posHead.getY(), posHead.getZ() + z);
                BlockPos candidateTailPos = new BlockPos(posTail.getX() + x, posTail.getY(), posTail.getZ() + z);
                if (isValid(candidateHeadPos))
                    possibleMovesForward.add(candidateHeadPos);
                if (isValid(candidateTailPos))
                    possibleMovesBackward.add(candidateTailPos);
            }
        }
        // Choose whether to move the "head" or the "tail"
        ArrayList<BlockPos> candidates = null;
        boolean forwards = true;
        if (possibleMovesBackward.isEmpty())
        {
            candidates = possibleMovesForward;
            forwards = true;
        }
        else if (possibleMovesForward.isEmpty())
        {
            candidates = possibleMovesBackward;
            forwards = false;
        }
        else
        {
            forwards = this.rng.nextDouble() < 0.5;
            candidates = forwards ? possibleMovesForward : possibleMovesBackward;
        }
        if (!candidates.isEmpty())
        {
            BlockDrawingHelper drawContext = new BlockDrawingHelper();
            drawContext.beginDrawing(world);

            BlockPos newPos = candidates.get(this.rng.nextInt(candidates.size()));
            if (forwards)
            {
                // Add the new head:
                this.originalPath.addFirst(world.getBlockState(newPos));
                drawContext.setBlockState(world, newPos, this.blockType);
                this.path.addFirst(newPos);
                // Move the tail?
                if (this.path.size() > this.pathSize)
                {
                    drawContext.setBlockState(world, posTail, new XMLBlockState(this.originalPath.removeLast()));
                    this.path.removeLast();
                }
            }
            else
            {
                // Backwards - add the new tail:
                this.originalPath.addLast(world.getBlockState(newPos));
                drawContext.setBlockState(world, newPos, this.blockType);
                this.path.addLast(newPos);
                // Move the head?
                if (this.path.size() > this.pathSize)
                {
                    drawContext.setBlockState(world, posHead, new XMLBlockState(this.originalPath.removeFirst()));
                    this.path.removeFirst();
                }
            }
            drawContext.endDrawing(world);
        }
    }

    private boolean isValid(BlockPos pos)
    {
        // Also check the current block is "permeable"...
        return blockInBounds(pos, this.arenaBounds);
    }

    private boolean blockInBounds(BlockPos pos, UnnamedGridDefinition bounds)
    {
        return pos.getX() >= bounds.getMin().getX() && pos.getX() <= bounds.getMax().getX() &&
               pos.getZ() >= bounds.getMin().getZ() && pos.getZ() <= bounds.getMax().getZ() &&
               pos.getY() >= bounds.getMin().getY() && pos.getY() <= bounds.getMax().getY();
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
    }

    @Override
    public void cleanup()
    {
    }

    private void createRNG()
    {
        // Initialise a RNG for this scene:
        long seed = 0;
        if (this.targetParams.getSeed() == null || this.targetParams.getSeed().equals("random"))
            seed = System.currentTimeMillis();
        else
            seed = Long.parseLong(this.targetParams.getSeed());
        this.rng = new Random(seed);
    }
}