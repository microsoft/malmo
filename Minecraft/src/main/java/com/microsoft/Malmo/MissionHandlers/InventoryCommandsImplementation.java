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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.Schemas.InventoryCommand;
import com.microsoft.Malmo.Schemas.InventoryCommands;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Very basic control over inventory. Two commands are required: select and drop - each takes a slot.<br>
 * The effect is to swap the item stacks over - eg "select 10" followed by "drop 0" will swap the stacks
 * in slots 0 and 10.<br>
 * The hotbar slots are 0-8, so this mechanism allows an agent to move items in to/out of the hotbar.
 */
public class InventoryCommandsImplementation extends CommandGroup
{
    public static class InventoryChangeMessage implements IMessage
    {
        public ItemStack itemsGained = null;
        public ItemStack itemsLost = null;

        public InventoryChangeMessage()
        {
        }
        public InventoryChangeMessage(ItemStack itemsGained, ItemStack itemsLost)
        {
            this.itemsGained = itemsGained;
            this.itemsLost = itemsLost;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            boolean gainedItems = buf.readBoolean();
            if (gainedItems)
                this.itemsGained = ByteBufUtils.readItemStack(buf);
            boolean lostItems = buf.readBoolean();
            if (lostItems)
                this.itemsLost = ByteBufUtils.readItemStack(buf);
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeBoolean(this.itemsGained != null);
            if (this.itemsGained != null)
                ByteBufUtils.writeItemStack(buf, this.itemsGained);
            buf.writeBoolean(this.itemsLost != null);
            if (this.itemsLost != null)
                ByteBufUtils.writeItemStack(buf, this.itemsLost);
        }
    }
 
    public static class InventoryMessage implements IMessage
    {
        String invA;
        String invB;
        int slotA;
        int slotB;
        boolean combine;
        BlockPos containerPos;

        public InventoryMessage()
        {
        }

        public InventoryMessage(List<Object> params, boolean combine)
        {
            this.invA = (String)params.get(0);
            this.slotA = (Integer)params.get(1);
            this.invB = (String)params.get(2);
            this.slotB = (Integer)params.get(3);
            if (params.size() == 5)
                this.containerPos = (BlockPos)params.get(4);
            this.combine = combine;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            this.invA = ByteBufUtils.readUTF8String(buf);
            this.slotA = buf.readInt();
            this.invB = ByteBufUtils.readUTF8String(buf);
            this.slotB = buf.readInt();
            this.combine = buf.readBoolean();
            if (buf.readBoolean())
                this.containerPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            ByteBufUtils.writeUTF8String(buf, this.invA);
            buf.writeInt(this.slotA);
            ByteBufUtils.writeUTF8String(buf, this.invB);
            buf.writeInt(this.slotB);
            buf.writeBoolean(this.combine);
            buf.writeBoolean(this.containerPos != null);
            if (this.containerPos != null)
            {
                buf.writeInt(this.containerPos.getX());
                buf.writeInt(this.containerPos.getY());
                buf.writeInt(this.containerPos.getZ());
            }
        }
    }

    public static class InventoryMessageHandler implements IMessageHandler<InventoryMessage, IMessage>
    {
        @Override
        public InventoryChangeMessage onMessage(final InventoryMessage message, MessageContext ctx)
        {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            IThreadListener mainThread = (WorldServer)ctx.getServerHandler().playerEntity.world;
            mainThread.addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    ItemStack[] changes = null;
                    if (message.combine)
                        changes = combineSlots(player, message.invA, message.slotA, message.invB, message.slotB, message.containerPos);
                    else
                        changes = swapSlots(player, message.invA, message.slotA, message.invB, message.slotB, message.containerPos);
                    if (changes != null)
                        MalmoMod.network.sendTo(new InventoryChangeMessage(changes[0], changes[1]), player);
                }
            });
            return null;
        }
    }

    public static class InventoryChangeMessageHandler implements IMessageHandler<InventoryChangeMessage, IMessage>
    {
        @Override
        public IMessage onMessage(InventoryChangeMessage message, MessageContext ctx)
        {
            if (message.itemsGained != null)
            {
                RewardForCollectingItemImplementation.GainItemEvent event = new RewardForCollectingItemImplementation.GainItemEvent(message.itemsGained);
                MinecraftForge.EVENT_BUS.post(event);
            }
            if (message.itemsLost != null)
            {
                RewardForDiscardingItemImplementation.LoseItemEvent event = new RewardForDiscardingItemImplementation.LoseItemEvent(message.itemsLost);
                MinecraftForge.EVENT_BUS.post(event);
            }
            return null;
        }
    }

    InventoryCommandsImplementation()
    {
        setShareParametersWithChildren(true);   // Pass our parameter block on to the following children:
        this.addCommandHandler(new CommandForHotBarKeysImplementation());
    }

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);

        if (params == null || !(params instanceof InventoryCommands))
            return false;

        InventoryCommands iparams = (InventoryCommands) params;
        setUpAllowAndDenyLists(iparams.getModifierList());
        return true;
    }

    static ItemStack[] combineSlots(EntityPlayerMP player, String invDst, int dst, String invAdd, int add, BlockPos containerPos)
    {
        IInventory container = null;
        String containerName = "";
        if (containerPos != null)
        {
            TileEntity te = player.world.getTileEntity(containerPos);
            if (te != null && te instanceof TileEntityLockableLoot)
            {
                containerName = ObservationFromFullInventoryImplementation.getInventoryName((IInventory)te);
                container = (IInventory)te;
            }
            else if (te != null && te instanceof TileEntityEnderChest)
            {
                containerName = ObservationFromFullInventoryImplementation.getInventoryName(player.getInventoryEnderChest());
                container = player.getInventoryEnderChest();
            }
        }
        IInventory dstInv = invDst.equals("inventory") ? player.inventory : (invDst.equals(containerName) ? container : null);
        IInventory addInv = invAdd.equals("inventory") ? player.inventory : (invAdd.equals(containerName) ? container : null);
        if (dstInv == null || addInv == null)
            return null; // Source or dest container not available.

        ItemStack dstStack = dstInv.getStackInSlot(dst);
        ItemStack addStack = addInv.getStackInSlot(add);

        if (addStack == null)
            return null; // Combination is a no-op.

        ItemStack[] returnStacks = null;

        if (dstStack == null) // Do a straight move - nothing to combine with.
        {
            if (dstInv != addInv)
            {
                // Items are moving between our inventory and the foreign inventory - may need to trigger
                // rewards for collecting / discarding.
                returnStacks = new ItemStack[2];
                ItemStack stackBeingLost = (addInv == player.inventory) ? addStack : null;
                ItemStack stackBeingGained = (dstInv == player.inventory) ? addStack : null;
                if (stackBeingGained != null)
                    returnStacks[0] = stackBeingGained.copy();
                if (stackBeingLost != null)
                    returnStacks[1] = stackBeingLost.copy();
            }
            dstInv.setInventorySlotContents(dst, addStack);
            addInv.setInventorySlotContents(add, null);
            return returnStacks;
        }

        // Check we can combine. This logic comes from InventoryPlayer.storeItemStack():
        boolean itemsMatch = dstStack.getItem() == addStack.getItem();
        boolean dstCanStack = dstStack.isStackable() && dstStack.getCount() < dstStack.getMaxStackSize() && dstStack.getCount() < dstInv.getInventoryStackLimit();
        boolean subTypesMatch = !dstStack.getHasSubtypes() || dstStack.getMetadata() == addStack.getMetadata();
        boolean tagsMatch = ItemStack.areItemStackTagsEqual(dstStack, addStack);
        if (itemsMatch && dstCanStack && subTypesMatch && tagsMatch)
        {
            // We can combine, so figure out how much we have room for:
            int limit = Math.min(dstStack.getMaxStackSize(), dstInv.getInventoryStackLimit());
            int room = limit - dstStack.getCount();
            ItemStack itemsTransferred = dstStack.copy();
            if (addStack.getCount() > room)
            {
                // Not room for all of it, so shift across as much as possible.
                addStack.shrink(room);
                dstStack.grow(room);
                itemsTransferred.setCount(room);
            }
            else
            {
                // Room for the whole lot, so empty out the add slot.
                dstStack.grow(addStack.getCount());
                itemsTransferred.setCount(addStack.getCount());
                addInv.removeStackFromSlot(add);//setInventorySlotContents(add, null);
            }
            if (dstInv != addInv)
            {
                // Items are moving between our inventory and the foreign inventory - may need to trigger
                // rewards for collecting / discarding.
                returnStacks = new ItemStack[2];
                if (dstInv == player.inventory)
                    returnStacks[0] = itemsTransferred; // We're gaining them
                else
                    returnStacks[1] = itemsTransferred; // We're losing them
            }
        }
        return returnStacks;
    }

    static ItemStack[] swapSlots(EntityPlayerMP player, String lhsInv, int lhs, String rhsInv, int rhs, BlockPos containerPos)
    {
        IInventory container = null;
        String containerName = "";
        if (containerPos != null)
        {
            TileEntity te = player.world.getTileEntity(containerPos);
            if (te != null && te instanceof TileEntityLockableLoot)
            {
                containerName = ObservationFromFullInventoryImplementation.getInventoryName((IInventory)te);
                container = (IInventory)te;
            }
            else if (te != null && te instanceof TileEntityEnderChest)
            {
                containerName = ObservationFromFullInventoryImplementation.getInventoryName(player.getInventoryEnderChest());
                container = player.getInventoryEnderChest();
            }

        }
        IInventory lhsInventory = lhsInv.equals("inventory") ? player.inventory : (lhsInv.equals(containerName) ? container : null);
        IInventory rhsInventory = rhsInv.equals("inventory") ? player.inventory : (rhsInv.equals(containerName) ? container : null);
        if (lhsInventory == null || rhsInventory == null)
            return null; // Source or dest container not available.
        if (rhs < 0 || lhs < 0)
            return null; // Out of bounds.
        if (lhs >= lhsInventory.getSizeInventory() || rhs >= rhsInventory.getSizeInventory())
            return null; // Out of bounds.

        ItemStack srcStack = lhsInventory.getStackInSlot(lhs);
        ItemStack dstStack = rhsInventory.getStackInSlot(rhs);
        lhsInventory.setInventorySlotContents(lhs, dstStack);
        rhsInventory.setInventorySlotContents(rhs, srcStack);
        if (lhsInventory != rhsInventory)
        {
            // Items have moved between our inventory and the foreign inventory - may need to trigger
            // rewards for collecting / discarding.
            ItemStack[] returnStacks = new ItemStack[2];
            ItemStack stackBeingLost = (lhsInventory == player.inventory) ? srcStack : dstStack;
            ItemStack stackBeingGained = (lhsInventory == player.inventory) ? dstStack : srcStack;
            if (stackBeingGained != null)
                returnStacks[0] = stackBeingGained.copy();
            if (stackBeingLost != null)
                returnStacks[1] = stackBeingLost.copy();
            return returnStacks;
        }
        return null;
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        if (verb.equalsIgnoreCase(InventoryCommand.SWAP_INVENTORY_ITEMS.value()))
        {
            if (parameter != null && parameter.length() != 0)
            {
                List<Object> params = new ArrayList<Object>();
                if (getParameters(parameter, params))
                {
                    // All okay, so create a swap message for the server:
                    MalmoMod.network.sendToServer(new InventoryMessage(params, false));
                    return true;
                }
                else
                    return false;   // Duff parameters.
            }
        }
        else if (verb.equalsIgnoreCase(InventoryCommand.COMBINE_INVENTORY_ITEMS.value()))
        {
            if (parameter != null && parameter.length() != 0)
            {
                List<Object> params = new ArrayList<Object>();
                if (getParameters(parameter, params))
                {
                    // All okay, so create a combine message for the server:
                    MalmoMod.network.sendToServer(new InventoryMessage(params, true));
                    return true;
                }
                else
                    return false;   // Duff parameters.
            }
        }
        else if (verb.equalsIgnoreCase(InventoryCommand.DISCARD_CURRENT_ITEM.value()))
        {
            // This we can do on the client side:
            Minecraft.getMinecraft().player.dropItem(false);  // false means just drop one item - true means drop everything in the current stack.
            return true;
        }
        return super.onExecute(verb, parameter, missionInit);
    }

    private boolean getParameters(String parameter, List<Object> parsedParams)
    {
        String[] params = parameter.split(" ");
        if (params.length != 2)
        {
            System.out.println("Malformed parameter string (" + parameter + ") - expected <x> <y>");
            return false;   // Error - incorrect number of parameters.
        }
        String[] lhsParams = params[0].split(":");
        String[] rhsParams = params[1].split(":");
        Integer lhsIndex, rhsIndex;
        String lhsName, rhsName, lhsStrIndex, rhsStrIndex;
        boolean checkContainers = false;
        if (lhsParams.length == 2)
        {
            lhsName = lhsParams[0];
            lhsStrIndex = lhsParams[1];
            checkContainers = true;
        }
        else if (lhsParams.length == 1)
        {
            lhsName = "inventory";
            lhsStrIndex = lhsParams[0];
        }
        else
        {
            System.out.println("Malformed parameter string (" + params[0] + ")");
            return false;
        }
        if (rhsParams.length == 2)
        {
            rhsName = rhsParams[0];
            rhsStrIndex = rhsParams[1];
            checkContainers = true;
        }
        else if (rhsParams.length == 1)
        {
            rhsName = "inventory";
            rhsStrIndex = rhsParams[0];
        }
        else
        {
            System.out.println("Malformed parameter string (" + params[1] + ")");
            return false;
        }

        try
        {
            lhsIndex = Integer.valueOf(lhsStrIndex);
            rhsIndex = Integer.valueOf(rhsStrIndex);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Malformed parameter string (" + parameter + ") - " + e.getMessage());
            return false;
        }
        if (lhsIndex == null || rhsIndex == null)
        {
            System.out.println("Malformed parameter string (" + parameter + ")");
            return false;   // Error - incorrect parameters.
        }
        BlockPos containerPos = null;
        if (checkContainers)
        {
            String containerName = "";
            RayTraceResult rtr = Minecraft.getMinecraft().objectMouseOver;
            if (rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK)
            {
                containerPos = rtr.getBlockPos();
                TileEntity te = Minecraft.getMinecraft().world.getTileEntity(containerPos);
                if (te instanceof TileEntityLockableLoot)
                    containerName = ObservationFromFullInventoryImplementation.getInventoryName((IInventory)te);
                else if (te instanceof TileEntityEnderChest)
                    containerName = ObservationFromFullInventoryImplementation.getInventoryName(Minecraft.getMinecraft().player.getInventoryEnderChest());
            }
            boolean containerMatches = (lhsName.equals("inventory") || lhsName.equals(containerName)) && (rhsName.equals("inventory") || rhsName.equals(containerName));
            if (!containerMatches)
            {
                System.out.println("Missing container requested in parameter string (" + parameter + ")");
                return false;
            }
        }

        parsedParams.add(lhsName);
        parsedParams.add(lhsIndex);
        parsedParams.add(rhsName);
        parsedParams.add(rhsIndex);
        if (containerPos != null)
            parsedParams.add(containerPos);
        return true;
    }

    @Override
    public boolean isFixed() { return true; }
}