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

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.Schemas.DiscreteMovementCommand;
import com.microsoft.Malmo.Schemas.DiscreteMovementCommands;
import com.microsoft.Malmo.Schemas.MissionInit;

/**
 * Fairly dumb command handler that attempts to move the player one block N,S,E
 * or W.<br>
 */
public class DiscreteMovementCommandsImplementation extends CommandBase implements ICommandHandler
{
    public static final String MOVE_ATTEMPTED_KEY = "attemptedToMove";

    private boolean isOverriding;
    DiscreteMovementCommands params;

    public static class DiscretePartialMoveEvent extends Event
    {
        public final double x;
        public final double y;
        public final double z;

        public DiscretePartialMoveEvent(double x, double y, double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class UseActionMessage implements IMessage
    {
        public BlockPos pos;
        public ItemStack itemStack;
        public EnumFacing face;
        public boolean standOnPlacedBlock;
        public UseActionMessage()
        {
        }

        public UseActionMessage(BlockPos pos, ItemStack itemStack, EnumFacing face, boolean standOnPlacedBlock)
        {
            this.pos = pos;
            this.itemStack = itemStack;
            this.face = face;
            this.standOnPlacedBlock = standOnPlacedBlock;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            this.pos = new BlockPos( buf.readInt(), buf.readInt(), buf.readInt() );
            this.itemStack = ByteBufUtils.readItemStack(buf);
            this.face = EnumFacing.values()[buf.readInt()];
            this.standOnPlacedBlock = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(this.pos.getX());
            buf.writeInt(this.pos.getY());
            buf.writeInt(this.pos.getZ());
            ByteBufUtils.writeItemStack(buf, this.itemStack);
            buf.writeInt(this.face.ordinal());
            buf.writeBoolean(this.standOnPlacedBlock);
        }
    }

    public static class UseActionMessageHandler implements IMessageHandler<UseActionMessage, IMessage>
    {
        @Override
        public IMessage onMessage(final UseActionMessage message, final MessageContext ctx)
        {
            IThreadListener mainThread = null;
            if (ctx.side == Side.CLIENT)
                return null;    // Not interested.

            mainThread = MinecraftServer.getServer();
            mainThread.addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                    PlayerInteractEvent event = new PlayerInteractEvent(player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, message.pos, message.face, player.worldObj);
                    MinecraftForge.EVENT_BUS.post(event);
                    if (!event.isCanceled()) {
                        BlockPos pos = message.pos.add( message.face.getDirectionVec() );
                        Block b = Block.getBlockFromItem( message.itemStack.getItem() );
                        if( b != null ) {
                            IBlockState blockType = b.getStateFromMeta( message.itemStack.getMetadata() );
                            if (player.worldObj.setBlockState( pos, blockType ))
                            {
                                BlockSnapshot snapshot = new BlockSnapshot(player.worldObj, pos, blockType);
                                BlockEvent.PlaceEvent placeevent = new BlockEvent.PlaceEvent(snapshot, player.worldObj.getBlockState(message.pos), player);
                                MinecraftForge.EVENT_BUS.post(placeevent);
                                // We set the block, so remove it from the inventory.
                                if (!player.theItemInWorldManager.isCreative())
                                {
                                    if (player.inventory.getCurrentItem().stackSize > 1)
                                        player.inventory.getCurrentItem().stackSize--;
                                    else
                                        player.inventory.mainInventory[player.inventory.currentItem] = null;
                                }
                                if (message.standOnPlacedBlock)
                                {
                                    // Eg after a jump-use, the player might expect to stand on the block that was just placed.
                                    player.setPosition(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                                }
                            }
                        }
                    }
                }
            });
            return null;
        }
    }


    public static class AttackActionMessage implements IMessage
    {
        public BlockPos pos;
        public EnumFacing face;
        public AttackActionMessage()
        {
        }

        public AttackActionMessage(BlockPos hitPos, EnumFacing face)
        {
            this.pos = hitPos;
            this.face = face;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            this.pos = new BlockPos( buf.readInt(), buf.readInt(), buf.readInt() );
            this.face = EnumFacing.values()[buf.readInt()];
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(this.pos.getX());
            buf.writeInt(this.pos.getY());
            buf.writeInt(this.pos.getZ());
            buf.writeInt(this.face.ordinal());
        }
    }

    public static class AttackActionMessageHandler implements IMessageHandler<AttackActionMessage, IMessage>
    {
        @Override
        public IMessage onMessage(final AttackActionMessage message, final MessageContext ctx)
        {
            IThreadListener mainThread = null;
            if (ctx.side == Side.CLIENT)
                return null;    // Not interested.

            mainThread = MinecraftServer.getServer();
            mainThread.addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                    IBlockState iblockstate = player.worldObj.getBlockState(message.pos);
                    Block block = iblockstate.getBlock();
                    if (block.getMaterial() != Material.air)
                    {
                        PlayerInteractEvent event = new PlayerInteractEvent(player, PlayerInteractEvent.Action.LEFT_CLICK_BLOCK, message.pos, message.face, player.worldObj);
                        MinecraftForge.EVENT_BUS.post(event);
                        if (!event.isCanceled())
                        {
                            boolean dropBlock = false;
                            // We do things this way, rather than pass true for dropBlock in world.destroyBlock,
                            // because we want this to take instant effect - we don't want the intermediate stage
                            // of spawning a free-floating item that the player must pick up.
                            java.util.List<ItemStack> items = block.getDrops(player.worldObj, message.pos, iblockstate, 0);
                            player.worldObj.destroyBlock( message.pos, dropBlock );
                            for (ItemStack item : items)
                            {
                                if (!player.inventory.addItemStackToInventory(item)) {
                                   Block.spawnAsEntity(player.worldObj, message.pos, item); // Didn't fit in inventory, so spawn it.
                                }
                            }
                            BlockEvent.BreakEvent breakevent = new BlockEvent.BreakEvent(player.worldObj, message.pos, iblockstate, player);
                            MinecraftForge.EVENT_BUS.post(breakevent);
                        }
                    }
                }
            });
            return null;
        }
    }

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof DiscreteMovementCommands))
            return false;

        this.params = (DiscreteMovementCommands)params;
        setUpAllowAndDenyLists(this.params.getModifierList());
        return true;
    }

    private int getDirectionFromYaw(float yaw)
    {
        // Initialise direction:
        int direction = (int)((yaw + 45.0f) / 90.0f);
        return (direction + 4) % 4;
    }

    private DiscreteMovementCommand verbToCommand(String verb)
    {
        for (DiscreteMovementCommand com : DiscreteMovementCommand.values())
        {
            if (verb.equalsIgnoreCase(com.value()))
                return com;
        }
        return null;
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        boolean handled = false;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null)
        {
            int z = 0;
            int x = 0;
            int y = 0;
            DiscreteMovementCommand command = verbToCommand(verb);
            if (command == null)
                return false;   // Did not recognise this command.

            switch (command)
            {
            case MOVENORTH:
            case JUMPNORTH:
                z = -1;
                break;
            case MOVESOUTH:
            case JUMPSOUTH:
                z = 1;
                break;
            case MOVEEAST:
            case JUMPEAST:
                x = 1;
                break;
            case MOVEWEST:
            case JUMPWEST:
                x = -1;
                break;
            case MOVE:
            case JUMPMOVE:
            case STRAFE:
            case JUMPSTRAFE:
                if (parameter != null && parameter.length() != 0)
                {
                    float velocity = Float.valueOf(parameter);
                    int offset = (velocity > 0) ? 1 : ((velocity < 0) ? -1 : 0);
                    int direction = getDirectionFromYaw(player.rotationYaw);
                    // For strafing, add one to direction:
                    if (command == DiscreteMovementCommand.STRAFE || command == DiscreteMovementCommand.JUMPSTRAFE)
                        direction = (direction + 1) % 4;
                    switch (direction)
                    {
                    case 0: // North
                        z = offset;
                        break;
                    case 1: // East
                        x = -offset;
                        break;
                    case 2: // South
                        z = -offset;
                        break;
                    case 3: // West
                        x = offset;
                        break;
                    }
                break;
            }
            case TURN:
                if (parameter != null && parameter.length() != 0)
                {
                    float yawDelta = Float.valueOf(parameter);
                    int direction = getDirectionFromYaw(player.rotationYaw);
                    direction += (yawDelta > 0) ? 1 : ((yawDelta < 0) ? -1 : 0);
                    direction = (direction + 4) % 4;
                    player.rotationYaw = direction * 90;
                    player.onUpdate();
                    // Send a message that the ContinuousMovementCommands can pick up on:
                    Event event = new CommandForWheeledRobotNavigationImplementation.ResetPitchAndYawEvent(true, player.rotationYaw, false, 0);
                    MinecraftForge.EVENT_BUS.post(event);
                    handled = true;
                }
                break;
            case LOOK:
                if (parameter != null && parameter.length() != 0)
                {
                    float pitchDelta = Float.valueOf(parameter);
                    player.rotationPitch += (pitchDelta < 0) ? -45 : ((pitchDelta > 0) ? 45 : 0);
                    player.onUpdate();
                    // Send a message that the ContinuousMovementCommands can pick up on:
                    Event event = new CommandForWheeledRobotNavigationImplementation.ResetPitchAndYawEvent(false, 0, true, player.rotationPitch);
                    MinecraftForge.EVENT_BUS.post(event);
                    handled = true;
                }
                break;
            case ATTACK:
                {
                    MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
                    if( mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ) {
                        BlockPos hitPos = mop.getBlockPos();
                        EnumFacing face = mop.sideHit;
                        IBlockState iblockstate = player.worldObj.getBlockState(hitPos);
                        Block block = iblockstate.getBlock();
                        if (block.getMaterial() != Material.air)
                        {
                            MalmoMod.network.sendToServer(new AttackActionMessage(hitPos, face));
                            // Trigger a reward for collecting the block
                            java.util.List<ItemStack> items = block.getDrops(player.worldObj, hitPos, iblockstate, 0);
                            for (ItemStack item : items)
                            {
                                RewardForCollectingItemImplementation.GainItemEvent event = new RewardForCollectingItemImplementation.GainItemEvent(item);
                                MinecraftForge.EVENT_BUS.post(event);
                            }
                        }
                    }
                    handled = true;
                    break;
                }
            case USE:
            case JUMPUSE:
                {
                    MovingObjectPosition mop = getObjectMouseOver(command);
                    if( mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK )
                    {
                        if( player.getCurrentEquippedItem() != null ) {
                            ItemStack itemStack = player.getCurrentEquippedItem();
                            Block b = Block.getBlockFromItem( itemStack.getItem() );
                            if( b != null ) {
                                BlockPos pos = mop.getBlockPos().add( mop.sideHit.getDirectionVec() );
                                // Can we place this block here?
                                AxisAlignedBB axisalignedbb = b.getCollisionBoundingBox(player.worldObj, pos, b.getDefaultState());
                                Entity exceptedEntity = (command == DiscreteMovementCommand.USE) ? null : player;
                                // (Not ideal, but needed by jump-use to allow the player to place a block where their feet would be.)
                                if (axisalignedbb == null || player.worldObj.checkNoEntityCollision(axisalignedbb, exceptedEntity))
                                {
                                    boolean standOnBlockPlaced = (command == DiscreteMovementCommand.JUMPUSE && mop.getBlockPos().equals(new BlockPos(player.posX, player.posY - 1, player.posZ)));
                                    MalmoMod.network.sendToServer(new UseActionMessage(mop.getBlockPos(), itemStack, mop.sideHit, standOnBlockPlaced));
                                    // Trigger a reward for discarding the block
                                    ItemStack droppedItemStack = new ItemStack(itemStack.getItem(), 1, itemStack.getItemDamage());
                                    RewardForDiscardingItemImplementation.LoseItemEvent event = new RewardForDiscardingItemImplementation.LoseItemEvent(droppedItemStack);
                                    MinecraftForge.EVENT_BUS.post(event);
                                }
                            }
                        }
                    }
                    handled = true;
                    break;
                }
            case JUMP:
                break;  // Handled below.
            }

            // Handle jumping cases:
            if (command == DiscreteMovementCommand.JUMP ||
                command == DiscreteMovementCommand.JUMPNORTH ||
                command == DiscreteMovementCommand.JUMPEAST ||
                command == DiscreteMovementCommand.JUMPSOUTH ||
                command == DiscreteMovementCommand.JUMPWEST ||
                command == DiscreteMovementCommand.JUMPMOVE ||
                command == DiscreteMovementCommand.JUMPUSE ||
                command == DiscreteMovementCommand.JUMPSTRAFE)
                y = 1;

            if (this.params.isAutoJump() && y == 0 && (z != 0 || x != 0))
            {
                // Do we need to jump?
                if (!player.worldObj.getCollidingBoundingBoxes(player, player.getEntityBoundingBox().offset(x, 0, z)).isEmpty())
                    y = 1;
            }

            if (z != 0 || x != 0 || y != 0)
            {
                // Attempt to move the entity:
                player.moveEntity(x, y, z);
                player.onUpdate();
                if (this.params.isAutoFall())
                {
                    // Did we step off a block? If so, attempt to fast-forward our fall.
                    int bailCountdown=256;  // Give up after this many attempts
                    // (This is needed because, for example, if the player is caught in a web, the downward movement will have no effect.)
                    while (!player.onGround && !player.capabilities.isFlying && bailCountdown > 0)
                    {
                        // Fast-forward downwards.
                        player.moveEntity(0, Math.floor(player.posY-0.0000001) - player.posY, 0);
                        player.onUpdate();
                        bailCountdown--;
                    }
                }

                // Now check where we ended up:
                double newX = player.posX;
                double newZ = player.posZ;

                // Are we still in the centre of a square, or did we get shunted?
                double desiredX = Math.floor(newX) + 0.5;
                double desiredZ = Math.floor(newZ) + 0.5;
                double deltaX = desiredX - newX;
                double deltaZ = desiredZ - newZ;
                if (deltaX * deltaX + deltaZ * deltaZ > 0.001)
                {
                    // Need to re-centralise.
                    // Before we do that, fire off a message - this will give the TouchingBlockType handlers
                    // a chance to react to the current position:
                    DiscretePartialMoveEvent event = new DiscretePartialMoveEvent(player.posX, player.posY, player.posZ);
                    MinecraftForge.EVENT_BUS.post(event);
                    // Now adjust the player:
                    player.moveEntity(deltaX, 0, deltaZ);
                    player.onUpdate();
                }
                // Now set the last tick pos values, to turn off inter-tick positional interpolation:
                player.lastTickPosX = player.posX;
                player.lastTickPosY = player.posY;
                player.lastTickPosZ = player.posZ;

                try
                {
                    MalmoMod.getPropertiesForCurrentThread().put(MOVE_ATTEMPTED_KEY, true);
                }
                catch (Exception e)
                {
                    // TODO - proper error reporting.
                    System.out.println("Failed to access properties for the client thread after discrete movement - reward may be incorrect.");
                }
                handled = true;
            }
        }
        return handled;
    }

    private MovingObjectPosition getObjectMouseOver(DiscreteMovementCommand command)
    {
        MovingObjectPosition mop = null;
        if (command.equals(DiscreteMovementCommand.USE))
            mop = Minecraft.getMinecraft().objectMouseOver;
        else if (command.equals(DiscreteMovementCommand.JUMPUSE))
        {
            long partialTicks = 0;  //Minecraft.timer.renderPartialTicks
            Entity viewer = Minecraft.getMinecraft().thePlayer;
            double blockReach = Minecraft.getMinecraft().playerController.getBlockReachDistance();
            Vec3 eyePos = viewer.getPositionEyes(partialTicks);
            Vec3 lookVec = viewer.getLook(partialTicks);
            int yOffset = 1;    // For the jump
            Vec3 searchVec = eyePos.addVector(lookVec.xCoord * blockReach, yOffset + lookVec.yCoord * blockReach, lookVec.zCoord * blockReach);
            mop = Minecraft.getMinecraft().theWorld.rayTraceBlocks(eyePos, searchVec, false, false, false);
        }
        return mop;
    }

    @Override
    public void install(MissionInit missionInit)
    {
    }

    @Override
    public void deinstall(MissionInit missionInit)
    {
    }

    @Override
    public boolean isOverriding()
    {
        return this.isOverriding;
    }

    @Override
    public void setOverriding(boolean b)
    {
        this.isOverriding = b;
    }
}
