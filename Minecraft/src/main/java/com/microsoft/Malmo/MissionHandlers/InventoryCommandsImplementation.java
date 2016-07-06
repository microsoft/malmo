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

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Schemas.InventoryCommand;
import com.microsoft.Malmo.Schemas.InventoryCommands;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

/** Very basic control over inventory. Two commands are required: select and drop - each takes a slot.<br>
 * The effect is to swap the item stacks over - eg "select 10" followed by "drop 0" will swap the stacks
 * in slots 0 and 10.<br>
 * The hotbar slots are 0-8, so this mechanism allows an agent to move items in to/out of the hotbar.
 */
public class InventoryCommandsImplementation extends CommandGroup
{
    private int sourceSlotIndex = 0;

    public static class CraftMessage implements IMessage
    {
        String parameters;
        public CraftMessage()
        {
        }
    
        public CraftMessage(String parameters)
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

    public static class CraftMessageHandler implements IMessageHandler<CraftMessage, IMessage>
    {
        @Override
        public IMessage onMessage(CraftMessage message, MessageContext ctx)
        {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            
            IBlockState block = MinecraftTypeHelper.ParseBlockType(message.parameters);
            Item item = MinecraftTypeHelper.ParseItemType(message.parameters);
            String target = "";
            if (block != null)
                target = block.getBlock().getUnlocalizedName();
            else if (item != null)
                target = item.getUnlocalizedName();
            else target = message.parameters;
            List<?> recipes = CraftingManager.getInstance().getRecipeList();
            for (Object obj : recipes)
            {
                if (obj == null)
                    continue;
                if (obj instanceof IRecipe)
                {
                    ItemStack is = ((IRecipe)obj).getRecipeOutput();
                    if (is == null)
                        continue;
                    String s = is.getUnlocalizedName();
                    System.out.println(s);
                    if (s.equals(target))
                    {
                        List<ItemStack> ingredients = getIngredients((IRecipe)obj);
                        if (playerHasIngredients(player, ingredients))
                        {
                            for (ItemStack isIngredient : ingredients)
                            {
                                for (int i = 0; i < isIngredient.stackSize; i++)
                                {
                                    player.inventory.consumeInventoryItem(isIngredient.getItem());
                                }
                            }
                            player.inventory.addItemStackToInventory(is.copy());
                            return null;
                        }
                    }
                }
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
        else if (verb.equalsIgnoreCase(InventoryCommand.DISCARD_CURRENT_ITEM.value()))
        {
            Minecraft.getMinecraft().thePlayer.dropOneItem(false);  // false means just drop one item - true means drop everything in the current stack.
            return true;
        }
        else if (verb.equalsIgnoreCase(InventoryCommand.CRAFT_ITEM.value()))
        {
            MalmoMod.network.sendToServer(new CraftMessage(parameter));
            return true;
        }
        return super.onExecute(verb, parameter, missionInit);
    }

    static List<ItemStack> getIngredients(IRecipe recipe)
    {
        List<ItemStack> ingredients = new ArrayList<ItemStack>();
        if (recipe instanceof ShapelessRecipes)
        {
            List<?> items = (List<?>)((ShapelessRecipes)recipe).recipeItems;
            for (Object obj : items)
            {
                if (obj instanceof ItemStack)
                    ingredients.add((ItemStack)obj);
            }
        }
        else if (recipe instanceof ShapelessOreRecipe)
        {
            ArrayList<Object> objs = ((ShapelessOreRecipe)recipe).getInput();
            for (Object o : objs)
            {
                if (o != null)
                {
                    if (o instanceof ItemStack)
                        ingredients.add((ItemStack)o);
                    else if (o instanceof List)
                    {
                        List<?> stacks = (List<?>)o;
                        for (Object stack : stacks)
                        {
                            if (stack instanceof ItemStack)
                                ingredients.add((ItemStack)stack);
                        }
                    }
                }
            }
        }
        else if (recipe instanceof ShapedRecipes)
        {
            ItemStack[] stack = ((ShapedRecipes)recipe).recipeItems;
            for (int i = 0; i < stack.length; i++)
            {
                if (stack[i] != null)
                    ingredients.add(stack[i]);
            }
        }
        else if (recipe instanceof ShapedOreRecipe)
        {
            Object[] items = ((ShapedOreRecipe)recipe).getInput();
            for (int i = 0; i < items.length; i++)
            {
                Object obj = items[i];
                if (obj != null)
                {
                    if (obj instanceof ItemStack)
                        ingredients.add((ItemStack)obj);
                    else if (obj instanceof List)
                    {
                        List<?> stacks = (List<?>)items[i];
                        for (Object stack : stacks)
                        {
                            if (stack instanceof ItemStack)
                                ingredients.add((ItemStack)stack);
                        }
                    }
                }
            }
        }
        else
        {
            return null;
        }
        return consolidateItemStacks(ingredients);
    }
    
    static List<ItemStack> consolidateItemStacks(List<ItemStack> inputStacks)
    {
        // Horrible n^2 method
        List<ItemStack> outputStacks = new ArrayList<ItemStack>();
        for (ItemStack sourceIS : inputStacks)
        {
            boolean bFound = false;
            for (ItemStack destIS : outputStacks)
            {
                if (ItemStack.areItemsEqual(sourceIS,  destIS))
                {
                    bFound = true;
                    destIS.stackSize += sourceIS.stackSize;
                }
            }
            if (!bFound)
                outputStacks.add(sourceIS.copy());
        }
        return outputStacks;
    }
    
    static boolean playerHasIngredients(EntityPlayerMP player, List<ItemStack> ingredients)
    {
        ItemStack[] main = player.inventory.mainInventory;
        ItemStack[] arm = player.inventory.armorInventory;

        for (ItemStack isIngredient : ingredients)
        {
            int target = isIngredient.stackSize;
            for (int i = 0; i < main.length + arm.length && target > 0; i++)
            {
                ItemStack isPlayer = (i >= main.length) ? arm[i - main.length] : main[i];
                if (isPlayer != null && isIngredient != null && isPlayer.getItem() == isIngredient.getItem())
                    target -= isPlayer.stackSize;
            }
            if (target > 0)
                return false;   // Don't have enough of this.
        }
        return true;
    }
}