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

package main.java.com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.Schemas.*;
import com.microsoft.Malmo.Schemas.MissionInit;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

/**
 * @author Brandon Houghton, Carnegie Mellon University
 * <p>
 * Equip commands allow agents to equip any item in their inventory worry about slots or hotbar location.
 */
public class EquipCommandsImplementation extends CommandBase implements ICommandHandler {
    private boolean isOverriding;

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null)
            return false;

        if (!verb.equalsIgnoreCase("equip"))
            return false;

        Item item = Item.getByNameOrId(parameter);
        if (item == null || item.getRegistryName() == null)
            return true;

        InventoryPlayer inv = player.inventory;
        boolean itemInInventory = false;
        ItemStack stackInInventory = null;
        int stackIndex = -1;
        for (int i = 0; !itemInInventory && i < inv.getSizeInventory(); i++) {
            Item stack = inv.getStackInSlot(i).getItem();
            if (stack.getRegistryName() != null && stack.getRegistryName().equals(item.getRegistryName())) {
                stackInInventory = inv.getStackInSlot(i);
                stackIndex = i;
                itemInInventory = true;
            }
        }

        // We don't have that item in our inventories
        if (!itemInInventory)
            return true;  // Returning true here as this handler should capture the place command

        // Swap current hotbar item with found inventory item (if not the same)
        int hotbarIdx = player.inventory.currentItem;
        ItemStack prevEquip = inv.getStackInSlot(hotbarIdx).copy();
        inv.setInventorySlotContents(hotbarIdx, stackInInventory);
        inv.setInventorySlotContents(stackIndex, prevEquip);

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
