package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.BlockType;
import com.microsoft.Malmo.Schemas.Colour;
import com.microsoft.Malmo.Schemas.DrawBlock;
import com.microsoft.Malmo.Schemas.DrawBlockBasedObjectType;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.MovingTargetDecorator;
import com.microsoft.Malmo.Schemas.Pos;
import com.microsoft.Malmo.Schemas.UnnamedGridDefinition;
import com.microsoft.Malmo.Schemas.Variation;
import com.microsoft.Malmo.Utils.BlockDrawingHelper;
import com.microsoft.Malmo.Utils.BlockDrawingHelper.XMLBlockState;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

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
    private boolean mustWaitTurn = false;
    private boolean isOurTurn = false;
    private String guid = UUID.randomUUID().toString();

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
        if (this.targetParams.getUpdateSpeed() == null || this.targetParams.getUpdateSpeed().equals("turnbased"))
        {
            this.mustWaitTurn = true;
        }
        else
        {
            this.speedInTicks = Integer.parseInt(this.targetParams.getUpdateSpeed());
        }
        createRNG();
        return true;
    }

    @Override
    public void buildOnWorld(MissionInit missionInit, World world) throws DecoratorException
    {
        this.path.add(this.startPos);
        this.originalPath.add(world.getBlockState(this.startPos));
        BlockDrawingHelper drawContext = new BlockDrawingHelper();
        drawContext.beginDrawing(world);
        drawContext.setBlockState(world, this.startPos, this.blockType);
        drawContext.endDrawing(world);
    }

    @Override
    public boolean getExtraAgentHandlersAndData(List<Object> handlers, Map<String, String> data)
    {
        return false;
    }

    private boolean pinchedByPlayer(World world)
    {
        for (BlockPos bp : this.path)
        {
            //Block b = world.getBlockState(bp).getBlock();
            //AxisAlignedBB aabb = b.getCollisionBoundingBox(world, bp, b.getDefaultState());
            //aabb.expand(0, 1, 0);
            BlockPos bp2 = new BlockPos(bp.getX()+1, bp.getY()+2, bp.getZ()+1);
            AxisAlignedBB aabb = new AxisAlignedBB(bp, bp2);
            List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, aabb);
            for (Entity ent : entities)
                if (ent instanceof EntityPlayer)
                    return true;
        }
        return false;
    }

    @Override
    public void update(World world)
    {
        if (this.mustWaitTurn && !this.isOurTurn)   // Using the turn scheduler?
            return;
        if (!this.mustWaitTurn)
        {
            // Not using turn scheduler - using update speed
            this.timeSinceLastUpdate++;
            if (this.timeSinceLastUpdate < this.speedInTicks)
                return;
        }
        this.timeSinceLastUpdate = 0;
        this.isOurTurn = false; // We're taking it right now.
        if (!pinchedByPlayer(world))
        {
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
                    if (isValid(world, candidateHeadPos))
                        possibleMovesForward.add(candidateHeadPos);
                    if (isValid(world, candidateTailPos))
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
        if (this.mustWaitTurn)
        {
            // Let server know we have finished.
            Map<String, String> data = new HashMap<String, String>();
            data.put("agentname", this.guid);
            MalmoMod.network.sendToServer(new MalmoMod.MalmoMessage(MalmoMessageType.CLIENT_TURN_TAKEN, 0, data));
        }
    }

    private boolean isValid(World world, BlockPos pos)
    {
        // In bounds?
        if (!blockInBounds(pos, this.arenaBounds))
            return false;
        // Already in path?
        if (this.path.contains(pos))
            return false;
        // Does there need to be air above the target?
        if (this.targetParams.isRequiresAirAbove() && !world.isAirBlock(pos.up()))
            return false;
        // Check the current block is "permeable"...
        IBlockState block = world.getBlockState(pos);
        List<IProperty> extraProperties = new ArrayList<IProperty>();
        DrawBlock db = MinecraftTypeHelper.getDrawBlockFromBlockState(block, extraProperties);

        boolean typesMatch = this.targetParams.getPermeableBlocks().getType().isEmpty();
        for (BlockType bt : this.targetParams.getPermeableBlocks().getType())
        {
            if (db.getType() == bt)
            {
                typesMatch = true;
                break;
            }
        }
        if (!typesMatch)
            return false;

        if (db.getColour() != null)
        {
            boolean coloursMatch = this.targetParams.getPermeableBlocks().getColour().isEmpty();
            for (Colour col : this.targetParams.getPermeableBlocks().getColour())
            {
                if (db.getColour() == col)
                {
                    coloursMatch = true;
                    break;
                }
            }
            if (!coloursMatch)
                return false;
        }

        if (db.getVariant() != null)
        {
            boolean variantsMatch = this.targetParams.getPermeableBlocks().getVariant().isEmpty();
            for (Variation var : this.targetParams.getPermeableBlocks().getVariant())
            {
                if (db.getVariant() == var)
                {
                    variantsMatch = true;
                    break;
                }
            }
            if (!variantsMatch)
                return false;
        }
        return true;
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

    @Override
    public boolean targetedUpdate(String nextAgentName)
    {
        if (this.mustWaitTurn && nextAgentName == this.guid)
        {
            this.isOurTurn = true;
            return true;
        }
        return false;
    }

    @Override
    public void getTurnParticipants(ArrayList<String> participants, ArrayList<Integer> participantSlots)
    {
        if (this.mustWaitTurn)
        {
            participants.add(this.guid);
            participantSlots.add(0);    // We want to go first!
        }
    }
}