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

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.Schemas.DiscreteMovementCommand;
import com.microsoft.Malmo.Schemas.DiscreteMovementCommands;
import com.microsoft.Malmo.Schemas.MissionInit;

import io.netty.buffer.ByteBuf;

/**
 * Fairly dumb command handler that attempts to move the player one block N,S,E
 * or W.<br>
 */
public class DiscreteMovementCommandsImplementation extends CommandBase implements ICommandHandler
{
    public static final String MOVE_ATTEMPTED_KEY = "attemptedToMove";

    private boolean isOverriding;
    private int direction = -1;

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
        String parameters;
        public UseActionMessage()
        {
        }
    
        public UseActionMessage(String parameters)
        {
            this.parameters = parameters;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            this.parameters = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            ByteBufUtils.writeUTF8String(buf, this.parameters);
        }
    }

    public static class UseActionMessageHandler implements IMessageHandler<UseActionMessage, IMessage>
    {
        @Override
        public IMessage onMessage(UseActionMessage message, MessageContext ctx)
        {
            MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
            if( mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK )
            {
                EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                if( player.getCurrentEquippedItem() != null ) {
                    ItemStack itemStack = player.getCurrentEquippedItem();
                    Block b = Block.getBlockFromItem( itemStack.getItem() );
                    if( b != null ) {
                        BlockPos pos = mop.getBlockPos().add( mop.sideHit.getDirectionVec() );
                        // Can we place this block here?
                        AxisAlignedBB axisalignedbb = b.getCollisionBoundingBox(player.worldObj, pos, b.getDefaultState());
                        if (axisalignedbb == null || player.worldObj.checkNoEntityCollision(axisalignedbb, null))
                        {
                            // Yes!
                            IBlockState blockType = b.getStateFromMeta( itemStack.getMetadata() );
                            if (player.worldObj.setBlockState( pos, blockType ))
                            {
                                // We set the block, so remove it from the inventory.
                                if (player.inventory.getCurrentItem().stackSize > 1)
                                    player.inventory.getCurrentItem().stackSize--;
                                else
                                    player.inventory.mainInventory[player.inventory.currentItem] = null;
                            }
                        }
                    }
                }
            }
            return null;
        }
    }


    public static class AttackActionMessage implements IMessage
    {
        String parameters;
        public AttackActionMessage()
        {
        }
    
        public AttackActionMessage(String parameters)
        {
            this.parameters = parameters;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            this.parameters = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            ByteBufUtils.writeUTF8String(buf, this.parameters);
        }
    }

    public static class AttackActionMessageHandler implements IMessageHandler<AttackActionMessage, IMessage>
    {
        @Override
        public IMessage onMessage(AttackActionMessage message, MessageContext ctx)
        {
            MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
            if( mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ) {
                BlockPos hitPos = mop.getBlockPos();
                EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                IBlockState iblockstate = player.worldObj.getBlockState(hitPos);
                Block block = iblockstate.getBlock();
                if (block.getMaterial() != Material.air)
                {
                    boolean dropBlock = false;
                    // We do things this way, rather than pass true for dropBlock in world.destroyBlock,
                    // because we want this to take instant effect - we don't want the intermediate stage
                    // of spawning a free-floating item that the player must pick up.
                    java.util.List<ItemStack> items = block.getDrops(player.worldObj, hitPos, iblockstate, 0);
                    player.worldObj.destroyBlock( hitPos, dropBlock );
                    for (ItemStack item : items)
                    {
                        if (!player.inventory.addItemStackToInventory(item))
                            Block.spawnAsEntity(player.worldObj, hitPos, item); // Didn't fit in inventory, so spawn it.
                    }
                }
            }
            return null;
        }
    }

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof DiscreteMovementCommands))
            return false;

        DiscreteMovementCommands dmparams = (DiscreteMovementCommands)params;
        setUpAllowAndDenyLists(dmparams.getModifierList());
        return true;
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        boolean handled = false;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null)
        {
            if (this.direction == -1)
            {
                // Initialise direction:
                this.direction = (int)((player.rotationYaw + 45.0f) / 90.0f);
                this.direction = (this.direction + 4) % 4;
            }
            int z = 0;
            int x = 0;
            int y = 0;
            if (verb.equalsIgnoreCase(DiscreteMovementCommand.MOVENORTH.value()))
            {
                z = -1;
            }
            else if (verb.equalsIgnoreCase(DiscreteMovementCommand.MOVESOUTH.value()))
            {
                z = 1;
            }
            else if (verb.equalsIgnoreCase(DiscreteMovementCommand.MOVEEAST.value()))
            {
                x = 1;
            }
            else if (verb.equalsIgnoreCase(DiscreteMovementCommand.MOVEWEST.value()))
            {
                x = -1;
            }
            else if (verb.equalsIgnoreCase(DiscreteMovementCommand.JUMP.value()))
            {
                y = 1;
            }
            else if (verb.equalsIgnoreCase(DiscreteMovementCommand.MOVE.value()))
            {
                if (parameter != null && parameter.length() != 0)
                {
                    float velocity = Float.valueOf(parameter);
                    int offset = (velocity > 0) ? 1 : ((velocity < 0) ? -1 : 0);
                    switch (this.direction)
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
                }
            }
            else if (verb.equalsIgnoreCase(DiscreteMovementCommand.TURN.value()))
            {
                if (parameter != null && parameter.length() != 0)
                {
                    float yawDelta = Float.valueOf(parameter);
                    this.direction += (yawDelta > 0) ? 1 : ((yawDelta < 0) ? -1 : 0);
                    this.direction = (this.direction + 4) % 4;
                    player.rotationYaw = this.direction * 90;
                    player.onUpdate();
                    handled = true;
                }
            }
            else if (verb.equalsIgnoreCase(DiscreteMovementCommand.LOOK.value()))
            {
                if (parameter != null && parameter.length() != 0)
                {
                    float pitchDelta = Float.valueOf(parameter);
                    player.rotationPitch += (pitchDelta < 0) ? -45 : ((pitchDelta > 0) ? 45 : 0);
                    player.onUpdate();
                    handled = true;
                }
            }
            else if (verb.equalsIgnoreCase(DiscreteMovementCommand.ATTACK.value()))
            {
                MalmoMod.network.sendToServer(new AttackActionMessage(parameter));
                handled = true;
            }
            else if (verb.equalsIgnoreCase(DiscreteMovementCommand.USE.value()))
            {
                MalmoMod.network.sendToServer(new UseActionMessage(parameter));
                handled = true;
            }

            if (z != 0 || x != 0 || y != 0)
            {
                // Attempt to move the entity:
                player.moveEntity(x, y, z);
                player.onUpdate();
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
