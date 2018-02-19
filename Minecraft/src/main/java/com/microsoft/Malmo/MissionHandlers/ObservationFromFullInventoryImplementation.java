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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.ObservationFromFullInventory;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Simple IObservationProducer class that returns a list of the full inventory, including the armour.
 */
public class ObservationFromFullInventoryImplementation extends ObservationFromServer
{
    public static class InventoryRequestMessage extends ObservationFromServer.ObservationRequestMessage
    {
        private boolean flat;
        private BlockPos pos;

        public InventoryRequestMessage()
        {
            this.flat = true;
            this.pos = null;
        }

        InventoryRequestMessage(boolean flat, BlockPos pos)
        {
            this.flat = flat;
            this.pos = pos;
        }

        @Override
        void restoreState(ByteBuf buf)
        {
            this.flat = buf.readBoolean();
            boolean readPos = buf.readBoolean();
            if (readPos)
                this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            else
                this.pos = null;
        }

        @Override
        void persistState(ByteBuf buf)
        {
            buf.writeBoolean(this.flat);
            buf.writeBoolean(this.pos != null);
            if (this.pos != null)
            {
                buf.writeInt(pos.getX());
                buf.writeInt(pos.getY());
                buf.writeInt(pos.getZ());
            }
        }

        public boolean isFlat() { return this.flat; }
        public BlockPos pos() { return this.pos; }
    }

    public static class InventoryRequestMessageHandler extends ObservationFromServer.ObservationRequestMessageHandler implements IMessageHandler<InventoryRequestMessage, IMessage>
    {
        @Override
        void buildJson(JsonObject json, EntityPlayerMP player, ObservationRequestMessage message)
        {
            // We want to output the inventory from:
            // a) the player
            // b) any chest-type objects the player is looking at.
            InventoryRequestMessage irm = (InventoryRequestMessage)message;
            IInventory foreignInv = null;
            if (irm.pos() != null)
            {
                TileEntity te = player.world.getTileEntity(irm.pos());
                if (te instanceof TileEntityLockableLoot)
                    foreignInv = (TileEntityLockableLoot)te;
                else if (te instanceof TileEntityEnderChest)
                    foreignInv = player.getInventoryEnderChest();
            }

            if (irm.isFlat())
            {
                // Write out the player's inventory in a flattened style.
                // (This is only included for backwards compatibility.)
                getInventoryJSON(json, "InventorySlot_", player.inventory, player.inventory.getSizeInventory());
                if (foreignInv != null)
                    getInventoryJSON(json, foreignInv.getName() + "Slot_", foreignInv, foreignInv.getSizeInventory());
            }
            else
            {
                // Newer approach - an array of objects.
                JsonArray arr = new JsonArray();
                getInventoryJSON(arr, player.inventory);
                if (foreignInv != null)
                    getInventoryJSON(arr, foreignInv);
                json.add("inventory", arr);
            }
            // Also add an entry for each type of inventory available.
            JsonArray arrInvs = new JsonArray();
            JsonObject jobjPlayer = new JsonObject();
            jobjPlayer.addProperty("name", getInventoryName(player.inventory));
            jobjPlayer.addProperty("size", player.inventory.getSizeInventory());
            arrInvs.add(jobjPlayer);
            if (foreignInv != null)
            {
                JsonObject jobjTell = new JsonObject();
                jobjTell.addProperty("name", getInventoryName(foreignInv));
                jobjTell.addProperty("size", foreignInv.getSizeInventory());
                arrInvs.add(jobjTell);
            }
            json.add("inventoriesAvailable", arrInvs);
            // Also add a field to show which slot in the hotbar is currently selected.
            json.addProperty("currentItemIndex", player.inventory.currentItem);
        }

        @Override
        public IMessage onMessage(InventoryRequestMessage message, MessageContext ctx)
        {
            return processMessage(message, ctx);
        }
    }

    private boolean flat;

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof ObservationFromFullInventory))
            return false;
        
        this.flat = ((ObservationFromFullInventory)params).isFlat();
        return true;
    }

    public static String getInventoryName(IInventory inv)
    {
        String invName = inv.getName();
        String prefix = "container.";
        if (invName.startsWith(prefix))
            invName = invName.substring(prefix.length());
        return invName;
    }

    public static void getInventoryJSON(JsonArray arr, IInventory inventory)
    {
        String invName = getInventoryName(inventory);
        for (int i = 0; i < inventory.getSizeInventory(); i++)
        {
            ItemStack is = inventory.getStackInSlot(i);
            if (is != null && !is.isEmpty())
            {
                JsonObject jobj = new JsonObject();
                DrawItem di = MinecraftTypeHelper.getDrawItemFromItemStack(is);
                String name = di.getType();
                if (di.getColour() != null)
                    jobj.addProperty("colour", di.getColour().value());
                if (di.getVariant() != null)
                    jobj.addProperty("variant", di.getVariant().getValue());
                jobj.addProperty("type", name);
                jobj.addProperty("index", i);
                jobj.addProperty("quantity", is.getCount());
                jobj.addProperty("inventory",  invName);
                arr.add(jobj);
            }
        }
    }

    public static void getInventoryJSON(JsonObject json, String prefix, IInventory inventory, int maxSlot)
    {
        int nSlots = Math.min(inventory.getSizeInventory(), maxSlot);
        for (int i = 0; i < nSlots; i++)
        {
            ItemStack is = inventory.getStackInSlot(i);
            if (is != null)
            {
                json.addProperty(prefix + i + "_size", is.getCount());
                DrawItem di = MinecraftTypeHelper.getDrawItemFromItemStack(is);
                String name = di.getType();
                if (di.getColour() != null)
                    json.addProperty(prefix + i + "_colour",  di.getColour().value());
                if (di.getVariant() != null)
                    json.addProperty(prefix + i + "_variant", di.getVariant().getValue());
                json.addProperty(prefix + i + "_item", name);
            }
        }
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        super.prepare(missionInit);
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
    }

    @Override
    public ObservationRequestMessage createObservationRequestMessage()
    {
        RayTraceResult rtr = Minecraft.getMinecraft().objectMouseOver;
        BlockPos pos = null;
        if (rtr != null && rtr.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            pos = rtr.getBlockPos();
        }
        return new InventoryRequestMessage(this.flat, pos);
    }
}