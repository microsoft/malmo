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
public class InventoryCommandsImplementation extends CommandGroup
{
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
    	
    	InventoryCommands iparams = (InventoryCommands)params;
    	setUpAllowAndDenyLists(iparams.getModifierList());
    	return true;
    }
    
    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        if (verb.equalsIgnoreCase(InventoryCommand.SWAP_INVENTORY_ITEMS.value()))
        {
            if (parameter != null && parameter.length() != 0)
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

                // All okay, so perform the swap:
                ItemStack srcStack = inv.getStackInSlot(lhs);
                ItemStack dstStack = inv.getStackInSlot(rhs);
                inv.setInventorySlotContents(lhs, dstStack);
                inv.setInventorySlotContents(rhs, srcStack);
                return true;
            }
        }
        else if (verb.equalsIgnoreCase(InventoryCommand.DISCARD_CURRENT_ITEM.value()))
        {
            Minecraft.getMinecraft().thePlayer.dropOneItem(false);  // false means just drop one item - true means drop everything in the current stack.
            return true;
        }
        return super.onExecute(verb, parameter, missionInit);
    }
}