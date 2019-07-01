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

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Schemas.*;
import com.microsoft.Malmo.Schemas.MissionInit;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;



import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Place commands allow agents to place blocks in the world without having to worry about inventory management.
 */
public class PlaceCommandsImplementation extends CommandBase implements ICommandHandler {
    private boolean isOverriding;


    public static class PlaceMessage implements IMessage
    {
        public BlockPos pos;
        public ItemStack itemStack;
        public Integer itemSlot;
        public net.minecraft.util.EnumFacing face;
        public net.minecraft.util.math.Vec3d hitVec;

        public PlaceMessage()
        {
        }

        public PlaceMessage(BlockPos pos, ItemStack itemStack, int itemSlot, net.minecraft.util.EnumFacing face, net.minecraft.util.math.Vec3d hitVec)
        {
            this.pos = pos;
            this.itemStack = itemStack;
            this.itemSlot = itemSlot;
            this.face = face;
            this.hitVec = hitVec;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            this.pos = new BlockPos( buf.readInt(), buf.readInt(), buf.readInt() );
            this.itemStack = net.minecraftforge.fml.common.network.ByteBufUtils.readItemStack(buf);
            this.itemSlot = buf.readInt();
            this.face = net.minecraft.util.EnumFacing.values()[buf.readInt()];
            this.hitVec = new net.minecraft.util.math.Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(this.pos.getX());
            buf.writeInt(this.pos.getY());
            buf.writeInt(this.pos.getZ());
            net.minecraftforge.fml.common.network.ByteBufUtils.writeItemStack(buf, this.itemStack);
            buf.writeInt(this.itemSlot);
            buf.writeInt(this.face.ordinal());
            buf.writeDouble(this.hitVec.xCoord);
            buf.writeDouble(this.hitVec.yCoord);
            buf.writeDouble(this.hitVec.zCoord);
        }
    }

    public static class PlaceMessageHandler implements IMessageHandler<PlaceMessage, IMessage> {
        @Override
        public IMessage onMessage(PlaceMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null)
                return null;

            BlockPos pos = message.pos.add( message.face.getDirectionVec() );
            Block b = Block.getBlockFromItem( message.itemStack.getItem() );
            if( b != null ) {
                net.minecraft.block.state.IBlockState blockType = b.getStateFromMeta( message.itemStack.getMetadata() );
                if (player.world.setBlockState( pos, blockType ))
                {
                    net.minecraftforge.common.util.BlockSnapshot snapshot = new net.minecraftforge.common.util.BlockSnapshot(player.world, pos, blockType);
                    net.minecraftforge.event.world.BlockEvent.PlaceEvent placeevent = new net.minecraftforge.event.world.BlockEvent.PlaceEvent(snapshot, player.world.getBlockState(message.pos), player);
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(placeevent);
                    // We set the block, so remove it from the inventory.
                    if (!player.isCreative())
                    {
                        if (player.inventory.mainInventory.get(message.itemSlot).getCount() > 1)
                            player.inventory.mainInventory.get(message.itemSlot).setCount(player.inventory.mainInventory.get(message.itemSlot).getCount() - 1);
                        else
                            player.inventory.mainInventory.get(message.itemSlot).setCount(0);
                    }
                }
            }

            return null;
            /////////////////////////////////////////////


        }
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        if (!verb.equalsIgnoreCase("place"))
            return false;

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null)
            return false;

        Item item = Item.getByNameOrId(parameter);
        Block block = Block.getBlockFromItem(item);
        if (item == null || item.getRegistryName() == null || block.getRegistryName() == null)
            return false;

        InventoryPlayer inv = player.inventory;
        boolean blockInInventory = false;
        ItemStack stackInInventory = null;
        int stackIndex = -1;
        for (int i = 0; !blockInInventory && i < inv.getSizeInventory(); i++) {
            Item stack = inv.getStackInSlot(i).getItem();
            if (stack.getRegistryName() != null && stack.getRegistryName().equals(item.getRegistryName())) {
                stackInInventory = inv.getStackInSlot(i);
                stackIndex = i;
                blockInInventory = true;
            }
        }

        // We don't have that block in our inventories
        if (!blockInInventory)
            return true;

        RayTraceResult mop = Minecraft.getMinecraft().objectMouseOver;
        if (mop.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos pos = mop.getBlockPos().add(mop.sideHit.getDirectionVec());
            // Can we place this block here?
            AxisAlignedBB axisalignedbb = block.getDefaultState().getCollisionBoundingBox(player.world, pos);
            if (axisalignedbb == null || player.world.checkNoEntityCollision(axisalignedbb.offset(pos), null)) {
                MalmoMod.network.sendToServer(new PlaceMessage(mop.getBlockPos(), new ItemStack(block), stackIndex, mop.sideHit, mop.hitVec));
            }
        }

        return true;
    }

    @Override
    public boolean parseParameters(Object params) {
        if (!(params instanceof PlaceCommands))
            return false;

        PlaceCommands pParams = (PlaceCommands) params;
        setUpAllowAndDenyLists(pParams.getModifierList());
        return true;
    }

    @Override
    public void install(MissionInit missionInit) {
    }

    @Override
    public void deinstall(MissionInit missionInit) {
    }

    @Override
    public boolean isOverriding() {
        return this.isOverriding;
    }

    @Override
    public void setOverriding(boolean b) {
        this.isOverriding = b;
    }
}
