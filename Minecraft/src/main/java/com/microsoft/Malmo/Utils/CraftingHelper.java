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

package com.microsoft.Malmo.Utils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.microsoft.Malmo.MissionHandlers.RewardForCollectingItemImplementation;
import com.microsoft.Malmo.MissionHandlers.RewardForDiscardingItemImplementation;

public class CraftingHelper
{
    /** Attempt to return the raw ingredients required for this recipe.<br>
     * Ignores all shaping.
     * @param recipe the IRecipe to dissect.
     * @return a list of ItemStacks, amalgamated so that all items of the same type are placed in the same stack.
     */
    public static List<ItemStack> getIngredients(IRecipe recipe)
    {
        // IRecipe helpfully has no method for inspecting the raw ingredients, so we need to do different things depending on the subclass.
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
    
    /** Take a list of ItemStacks and amalgamate where possible.<br>
     * @param inputStacks a list of ItemStacks
     * @return a list of ItemStacks, where all items of the same type are grouped into one stack.
     */
    public static List<ItemStack> consolidateItemStacks(List<ItemStack> inputStacks)
    {
        // Horrible n^2 method - we should do something nicer if this ever becomes a bottleneck.
        List<ItemStack> outputStacks = new ArrayList<ItemStack>();
        for (ItemStack sourceIS : inputStacks)
        {
            boolean bFound = false;
            for (ItemStack destIS : outputStacks)
            {
                if (destIS != null && sourceIS != null && itemStackIngredientsMatch(destIS, sourceIS))
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
    
    /** Inspect a player's inventory to see whether they have enough items to form the supplied list of ItemStacks.<br>
     * The ingredients list MUST be amalgamated such that no two ItemStacks contain the same type of item.
     * @param player
     * @param ingredients an amalgamated list of ingredients
     * @return true if the player's inventory contains sufficient quantities of all the required items.
     */
    public static boolean playerHasIngredients(EntityPlayerMP player, List<ItemStack> ingredients)
    {
        ItemStack[] main = player.inventory.mainInventory;
        ItemStack[] arm = player.inventory.armorInventory;

        for (ItemStack isIngredient : ingredients)
        {
            int target = isIngredient.stackSize;
            for (int i = 0; i < main.length + arm.length && target > 0; i++)
            {
                ItemStack isPlayer = (i >= main.length) ? arm[i - main.length] : main[i];
                if (isPlayer != null && isIngredient != null && itemStackIngredientsMatch(isPlayer, isIngredient))
                    target -= isPlayer.stackSize;
            }
            if (target > 0)
                return false;   // Don't have enough of this.
        }
        return true;
    }

    /** Compare two ItemStacks and see if their items match - take wildcards into account, don't take stacksize into account.
     * @param A ItemStack A
     * @param B ItemStack B
     * @return true if the stacks contain matching items.
     */
    private static boolean itemStackIngredientsMatch(ItemStack A, ItemStack B)
    {
        if (A == null && B == null)
            return true;
        if (A == null || B == null)
            return false;
        if (A.getMetadata() == OreDictionary.WILDCARD_VALUE || B.getMetadata() == OreDictionary.WILDCARD_VALUE)
            return A.getItem() == B.getItem();
        return ItemStack.areItemsEqual(A, B);
    }

    /** Manually attempt to remove ingredients from the player's inventory.<br>
     * @param player
     * @param ingredients
     */
    public static void removeIngredientsFromPlayer(EntityPlayerMP player, List<ItemStack> ingredients)
    {
        ItemStack[] main = player.inventory.mainInventory;
        ItemStack[] arm = player.inventory.armorInventory;

        for (ItemStack isIngredient : ingredients)
        {
            int target = isIngredient.stackSize;
            for (int i = 0; i < main.length + arm.length && target > 0; i++)
            {
                ItemStack isPlayer = (i >= main.length) ? arm[i - main.length] : main[i];
                if (isPlayer != null && isIngredient != null && itemStackIngredientsMatch(isPlayer, isIngredient))
                {
                    if (target >= isPlayer.stackSize)
                    {
                        // Consume this stack:
                        target -= isPlayer.stackSize;
                        if (i >= main.length)
                            arm[i - main.length] = null;
                        else
                            main[i] = null;
                    }
                    else
                    {
                        isPlayer.stackSize -= target;
                        target = 0;
                    }
                }
            }
            ItemStack resultForReward = isIngredient.copy();
            RewardForDiscardingItemImplementation.LoseItemEvent event = new RewardForDiscardingItemImplementation.LoseItemEvent(resultForReward);
            MinecraftForge.EVENT_BUS.post(event);
        }
    }

    /** Attempt to find all recipes that result in an item of the requested output.
     * @param output the desired item, eg from Types.xsd - "diamond_pickaxe" etc - or as a Minecraft name - eg "tile.woolCarpet.blue"
     * @return a list of IRecipe objects that result in this item.
     */
    public static List<IRecipe> getRecipesForRequestedOutput(String output)
    {
        List<IRecipe> matchingRecipes = new ArrayList<IRecipe>();
        ItemStack target = MinecraftTypeHelper.getItemStackFromParameterString(output);
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
                if (ItemStack.areItemsEqual(is, target))
                {
                    matchingRecipes.add((IRecipe)obj);
                }
            }
        }
        return matchingRecipes;
    }

    /** Attempt to craft the given recipe.<br>
     * This pays no attention to tedious things like using the right crafting table / brewing stand etc, or getting the right shape.<br>
     * It simply takes the raw ingredients out of the player's inventory, and inserts the output of the recipe, if possible.
     * @param player the SERVER SIDE player that will do the crafting.
     * @param recipe the IRecipe we wish to craft.
     * @return true if the recipe had an output, and the player had the required ingredients to create it; false otherwise.
     */
    public static boolean attemptCrafting(EntityPlayerMP player, IRecipe recipe)
    {
        if (player == null || recipe == null)
            return false;

        ItemStack is = recipe.getRecipeOutput();
        if (is == null)
            return false;

        List<ItemStack> ingredients = getIngredients(recipe);
        if (playerHasIngredients(player, ingredients))
        {
            // We have the ingredients we need, so directly manipulate the inventory.
            // First, remove the ingredients:
            removeIngredientsFromPlayer(player, ingredients);
            // Now add the output of the recipe:

            ItemStack resultForInventory = is.copy();
            ItemStack resultForReward = is.copy();
            player.inventory.addItemStackToInventory(resultForInventory);
            RewardForCollectingItemImplementation.GainItemEvent event = new RewardForCollectingItemImplementation.GainItemEvent(resultForReward);
            MinecraftForge.EVENT_BUS.post(event);

            return true;
        }
        return false;
    }

    /** Little utility method for dumping out a list of all the recipes we understand.
     * @param filename location to save the dumped list.
     * @throws IOException
     */
    public static void dumpRecipes(String filename) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(filename);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8");
        BufferedWriter writer = new BufferedWriter(osw);

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
                String s = is.stackSize + "x" + is.getUnlocalizedName() + " = ";
                List<ItemStack> ingredients = getIngredients((IRecipe)obj);
                boolean first = true;
                for (ItemStack isIngredient : ingredients)
                {
                    if (!first)
                        s += ", ";
                    s += isIngredient.stackSize + "x" + isIngredient.getUnlocalizedName();
                    s += "(" + isIngredient.getDisplayName() + ")";
                    first = false;
                }
                s += "\n";
                writer.write(s);
            }
        }
        writer.close();
    }
}