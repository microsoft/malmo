package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import com.microsoft.Malmo.Schemas.InventoryCommand;
import com.microsoft.Malmo.Schemas.InventoryCommands;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Very basic control over inventory. Two commands are required: select and drop - each takes a slot.<br>
 * The effect is to swap the item stacks over - eg "select 10" followed by "drop 0" will swap the stacks
 * in slots 0 and 10.<br>
 * The hotbar slots are 0-8, so this mechanism allows an agent to move items in to/out of the hotbar.
 */
public class InventoryCommandsImplementation extends CommandBase
{
    private boolean isOverriding;
    private int sourceSlotIndex = 0;
    
    @Override
    public boolean parseParameters(Object params)
    {
    	if (params == null || !(params instanceof InventoryCommands))
    		return false;
    	
    	InventoryCommands iparams = (InventoryCommands)params;
    	setUpAllowAndDenyLists(iparams.getModifierList());
    	return true;
    }
    
    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        if (verb.equalsIgnoreCase(InventoryCommand.SELECT_INVENTORY_ITEM.value()))
        {
            if (parameter != null && parameter.length() != 0)
            {
                this.sourceSlotIndex = Integer.valueOf(parameter);
                return true;
            }
        }
        else if (verb.equalsIgnoreCase(InventoryCommand.DROP_INVENTORY_ITEM.value()))
        {
            if (parameter != null && parameter.length() != 0)
            {
                int slot = Integer.valueOf(parameter);
                if (slot == this.sourceSlotIndex)
                {
                    return true;    // No-op.
                }
                InventoryPlayer inv = Minecraft.getMinecraft().thePlayer.inventory;
                ItemStack srcStack = inv.getStackInSlot(this.sourceSlotIndex);
                ItemStack dstStack = inv.getStackInSlot(slot);
                inv.setInventorySlotContents(this.sourceSlotIndex, dstStack);
                inv.setInventorySlotContents(slot, srcStack);
                return true;
            }
        }
        return false;
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
