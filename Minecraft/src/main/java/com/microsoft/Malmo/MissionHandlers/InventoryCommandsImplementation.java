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
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.microsoft.Malmo.MalmoMod;
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
    public static class InventoryMessage implements IMessage
    {
        int slotA;
        int slotB;
        boolean combine;

        public InventoryMessage()
        {
        }

        public InventoryMessage(int a, int b, boolean combine)
        {
            this.slotA = a;
            this.slotB = b;
            this.combine = combine;
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            this.slotA = buf.readInt();
            this.slotB = buf.readInt();
            this.combine = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(this.slotA);
            buf.writeInt(this.slotB);
            buf.writeBoolean(this.combine);
        }
    }

    public static class InventoryMessageHandler implements IMessageHandler<InventoryMessage, IMessage>
    {
        @Override
        public IMessage onMessage(InventoryMessage message, MessageContext ctx)
        {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (message.combine)
                combineSlots(player, message.slotA, message.slotB);
            else
                swapSlots(player, message.slotA, message.slotB);
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

    static void combineSlots(EntityPlayerMP player, int dst, int add)
    {
        InventoryPlayer inv = player.inventory;
        ItemStack dstStack = inv.getStackInSlot(dst);
        ItemStack addStack = inv.getStackInSlot(add);

        if (addStack == null)
            return;    // Combination is a no-op.

        if (dstStack == null)   // Do a straight move - nothing to combine with.
        {
            inv.setInventorySlotContents(dst, addStack);
            inv.setInventorySlotContents(add, null);
            return;
        }

        // Check we can combine. This logic comes from InventoryPlayer.storeItemStack():
        boolean itemsMatch = dstStack.getItem() == addStack.getItem();
        boolean dstCanStack = dstStack.isStackable() && dstStack.stackSize < dstStack.getMaxStackSize() && dstStack.stackSize < inv.getInventoryStackLimit();
        boolean subTypesMatch = !dstStack.getHasSubtypes() || dstStack.getMetadata() == addStack.getMetadata();
        boolean tagsMatch = ItemStack.areItemStackTagsEqual(dstStack, addStack);
        if (itemsMatch && dstCanStack && subTypesMatch && tagsMatch)
        {
            // We can combine, so figure out how much we have room for:
            int limit = Math.min(dstStack.getMaxStackSize(), inv.getInventoryStackLimit());
            int room = limit - dstStack.stackSize;
            if (addStack.stackSize > room)
            {
                // Not room for all of it, so shift across as much as possible.
                addStack.stackSize -= room;
                dstStack.stackSize += room;
            }
            else
            {
                // Room for the whole lot, so empty out the add slot.
                dstStack.stackSize += addStack.stackSize;
                inv.setInventorySlotContents(add, null);
            }
        }
    }

    static void swapSlots(EntityPlayerMP player, int lhs, int rhs)
    {
        InventoryPlayer inv = player.inventory;
        ItemStack srcStack = inv.getStackInSlot(lhs);
        ItemStack dstStack = inv.getStackInSlot(rhs);
        inv.setInventorySlotContents(lhs, dstStack);
        inv.setInventorySlotContents(rhs, srcStack);
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        if (verb.equalsIgnoreCase(InventoryCommand.SWAP_INVENTORY_ITEMS.value()))
        {
            if (parameter != null && parameter.length() != 0)
            {
                List<Integer> params = new ArrayList<Integer>();
                if (getParameters(parameter, params))
                {
                    // All okay, so create a swap message for the server:
                    MalmoMod.network.sendToServer(new InventoryMessage(params.get(0), params.get(1), false));
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
                List<Integer> params = new ArrayList<Integer>();
                if (getParameters(parameter, params))
                {
                    // All okay, so create a combine message for the server:
                    MalmoMod.network.sendToServer(new InventoryMessage(params.get(0), params.get(1), true));
                    return true;
                }
                else
                    return false;   // Duff parameters.
            }
        }
        else if (verb.equalsIgnoreCase(InventoryCommand.DISCARD_CURRENT_ITEM.value()))
        {
            // This we can do on the client side:
            Minecraft.getMinecraft().thePlayer.dropOneItem(false);  // false means just drop one item - true means drop everything in the current stack.
            return true;
        }
        return super.onExecute(verb, parameter, missionInit);
    }

    private boolean getParameters(String parameter, List<Integer> parsedParams)
    {
        String[] params = parameter.split(" ");
        if (params.length != 2)
        {
            System.out.println("Malformed parameter string (" + parameter + ") - expected <x> <y>");
            return false;   // Error - incorrect number of parameters.
        }
        Integer lhs, rhs;
        try
        {
            lhs = Integer.valueOf(params[0]);
            rhs = Integer.valueOf(params[1]);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Malformed parameter string (" + parameter + ") - " + e.getMessage());
            return false;
        }
        if (lhs == null || rhs == null)
        {
            System.out.println("Malformed parameter string (" + parameter + ")");
            return false;   // Error - incorrect parameters.
        }
        InventoryPlayer inv = Minecraft.getMinecraft().thePlayer.inventory;
        if (lhs < 0 || lhs >= inv.getSizeInventory() || rhs < 0 || rhs >= inv.getSizeInventory())
        {
            System.out.println("Inventory swap parameters out of bounds - must be between 0 and " + (inv.getSizeInventory() - 1));
            return false;   // Out of bounds.
        }
        parsedParams.add(lhs);
        parsedParams.add(rhs);
        return true;
    }

    @Override
    public boolean isFixed() { return true; }
}