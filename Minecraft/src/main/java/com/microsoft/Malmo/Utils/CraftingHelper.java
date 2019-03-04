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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlers.RewardForCollectingItemImplementation;
import com.microsoft.Malmo.MissionHandlers.RewardForDiscardingItemImplementation;

import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class CraftingHelper {
    private static Map<EntityPlayerMP, Integer> fuelCaches = new HashMap<EntityPlayerMP, Integer>();

    /**
     * Reset caches<br>
     * Needed to make sure the player starts with a fresh fuel stash.
     */
    public static void reset() {
        fuelCaches = new HashMap<EntityPlayerMP, Integer>();
    }

    /**
     * Attempt to return the raw ingredients required for this recipe.<br>
     * Ignores all shaping.
     *
     * @param recipe the IRecipe to dissect.
     * @return a list of ItemStacks, amalgamated so that all items of the same type are placed in the same stack.
     */
    public static List<ItemStack> getIngredients(IRecipe recipe) {
        // IRecipe helpfully has no method for inspecting the raw ingredients, so we need to do different things depending on the subclass.
        List<ItemStack> ingredients = new ArrayList<ItemStack>();
        if (recipe instanceof ShapelessRecipes) {
            List<?> items = (List<?>) ((ShapelessRecipes) recipe).recipeItems;
            for (Object obj : items) {
                if (obj instanceof ItemStack)
                    ingredients.add((ItemStack) obj);
            }
        } else if (recipe instanceof ShapelessOreRecipe) {
            NonNullList<Object> objs = ((ShapelessOreRecipe) recipe).getInput();
            for (Object o : objs) {
                if (o != null) {
                    if (o instanceof ItemStack)
                        ingredients.add((ItemStack) o);
                    else if (o instanceof List) {
                        List<?> stacks = (List<?>) o;
                        for (Object stack : stacks) {
                            if (stack instanceof ItemStack)
                                ingredients.add((ItemStack) stack);
                        }
                    }
                }
            }
        } else if (recipe instanceof ShapedRecipes) {
            ItemStack[] stack = ((ShapedRecipes) recipe).recipeItems;
            for (int i = 0; i < stack.length; i++) {
                if (stack[i] != null)
                    ingredients.add(stack[i]);
            }
        } else if (recipe instanceof ShapedOreRecipe) {
            Object[] items = ((ShapedOreRecipe) recipe).getInput();
            for (int i = 0; i < items.length; i++) {
                Object obj = items[i];
                if (obj != null) {
                    if (obj instanceof ItemStack)
                        ingredients.add((ItemStack) obj);
                    else if (obj instanceof List) {
                        List<?> stacks = (List<?>) items[i];
                        for (Object stack : stacks) {
                            if (stack instanceof ItemStack)
                                ingredients.add((ItemStack) stack);
                        }
                    }
                }
            }
        } else {
            return null;
        }
        return consolidateItemStacks(ingredients);
    }

    /**
     * Take a list of ItemStacks and amalgamate where possible.<br>
     *
     * @param inputStacks a list of ItemStacks
     * @return a list of ItemStacks, where all items of the same type are grouped into one stack.
     */
    public static List<ItemStack> consolidateItemStacks(List<ItemStack> inputStacks) {
        // Horrible n^2 method - we should do something nicer if this ever becomes a bottleneck.
        List<ItemStack> outputStacks = new ArrayList<ItemStack>();
        for (ItemStack sourceIS : inputStacks) {
            boolean bFound = false;
            for (ItemStack destIS : outputStacks) {
                if (destIS != null && sourceIS != null && itemStackIngredientsMatch(destIS, sourceIS)) {
                    bFound = true;
                    destIS.setCount(destIS.getCount() + sourceIS.getCount());
                }
            }
            if (!bFound)
                outputStacks.add(sourceIS.copy());
        }
        return outputStacks;
    }

    /**
     * Inspect a player's inventory to see whether they have enough items to form the supplied list of ItemStacks.<br>
     * The ingredients list MUST be amalgamated such that no two ItemStacks contain the same type of item.
     *
     * @param player
     * @param ingredients an amalgamated list of ingredients
     * @return true if the player's inventory contains sufficient quantities of all the required items.
     */
    public static boolean playerHasIngredients(EntityPlayerMP player, List<ItemStack> ingredients) {
        NonNullList<ItemStack> main = player.inventory.mainInventory;
        NonNullList<ItemStack> arm = player.inventory.armorInventory;

        for (ItemStack isIngredient : ingredients) {
            int target = isIngredient.getCount();
            for (int i = 0; i < main.size() + arm.size() && target > 0; i++) {
                ItemStack isPlayer = (i >= main.size()) ? arm.get(i - main.size()) : main.get(i);
                if (isPlayer != null && isIngredient != null && itemStackIngredientsMatch(isPlayer, isIngredient))
                    target -= isPlayer.getCount();
            }
            if (target > 0)
                return false;   // Don't have enough of this.
        }
        return true;
    }

    /**
     * Inspect a player's inventory to see whether they have enough items to form the supplied list of ItemStacks.<br>
     * The ingredients list MUST be amalgamated such that no two ItemStacks contain the same type of item.
     *
     * @param player
     * @param ingredients an amalgamated list of ingredients
     * @return true if the player's inventory contains sufficient quantities of all the required items.
     */
    public static boolean playerHasIngredients(EntityPlayerSP player, List<ItemStack> ingredients) {
        NonNullList<ItemStack> main = player.inventory.mainInventory;
        NonNullList<ItemStack> arm = player.inventory.armorInventory;

        for (ItemStack isIngredient : ingredients) {
            int target = isIngredient.getCount();
            for (int i = 0; i < main.size() + arm.size() && target > 0; i++) {
                ItemStack isPlayer = (i >= main.size()) ? arm.get(i - main.size()) : main.get(i);
                if (isPlayer != null && isIngredient != null && itemStackIngredientsMatch(isPlayer, isIngredient))
                    target -= isPlayer.getCount();
            }
            if (target > 0)
                return false;   // Don't have enough of this.
        }
        return true;
    }

    /**
     * Compare two ItemStacks and see if their items match - take wildcards into account, don't take stacksize into account.
     *
     * @param A ItemStack A
     * @param B ItemStack B
     * @return true if the stacks contain matching items.
     */
    private static boolean itemStackIngredientsMatch(ItemStack A, ItemStack B) {
        if (A == null && B == null)
            return true;
        if (A == null || B == null)
            return false;
        if (A.getMetadata() == OreDictionary.WILDCARD_VALUE || B.getMetadata() == OreDictionary.WILDCARD_VALUE)
            return A.getItem() == B.getItem();
        return ItemStack.areItemsEqual(A, B);
    }

    /**
     * Go through player's inventory and see how much fuel they have.
     *
     * @param player
     * @return the amount of fuel available in ticks
     */
    public static int totalBurnTimeInInventory(EntityPlayerMP player) {
        Integer fromCache = fuelCaches.get(player);
        int total = (fromCache != null) ? fromCache : 0;
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack is = player.inventory.mainInventory.get(i);
            total += is.getCount() * TileEntityFurnace.getItemBurnTime(is);
        }
        return total;
    }

    /**
     * Consume fuel from the player's inventory.<br>
     * Take it first from their cache, if present, and then from their inventory, starting
     * at the first slot and working upwards.
     *
     * @param player
     * @param burnAmount amount of fuel to burn, in ticks.
     */
    public static void burnInventory(EntityPlayerMP player, int burnAmount, ItemStack input) {
        if (!fuelCaches.containsKey(player))
            fuelCaches.put(player, -burnAmount);
        else
            fuelCaches.put(player, fuelCaches.get(player) - burnAmount);
        int index = 0;
        while (fuelCaches.get(player) < 0 && index < player.inventory.mainInventory.size()) {
            ItemStack is = player.inventory.mainInventory.get(index);
            if (is != null) {
                int burnTime = TileEntityFurnace.getItemBurnTime(is);
                if (burnTime != 0) {
                    // Consume item:
                    if (is.getCount() > 1)
                        is.setCount(is.getCount() - 1);
                    else {
                        // If this is a bucket of lava, we need to consume the lava but leave the bucket.
                        if (is.getItem() == Items.LAVA_BUCKET) {
                            // And if we're cooking wet sponge, we need to leave the bucket filled with water.
                            if (input.getItem() == Item.getItemFromBlock(Blocks.SPONGE) && input.getMetadata() == 1)
                                player.inventory.mainInventory.set(index, new ItemStack(Items.WATER_BUCKET));
                            else
                                player.inventory.mainInventory.set(index, new ItemStack(Items.BUCKET));
                        } else
                            player.inventory.mainInventory.get(index).setCount(0);
                        index++;
                    }
                    fuelCaches.put(player, fuelCaches.get(player) + burnTime);
                } else
                    index++;
            } else
                index++;
        }
    }

    /**
     * Manually attempt to remove ingredients from the player's inventory.<br>
     *
     * @param player
     * @param ingredients
     */
    public static void removeIngredientsFromPlayer(EntityPlayerMP player, List<ItemStack> ingredients) {
        NonNullList<ItemStack> main = player.inventory.mainInventory;
        NonNullList<ItemStack> arm = player.inventory.armorInventory;

        for (ItemStack isIngredient : ingredients) {
            int target = isIngredient.getCount();
            for (int i = 0; i < main.size() + arm.size() && target > 0; i++) {
                ItemStack isPlayer = (i >= main.size()) ? arm.get(i - main.size()) : main.get(i);
                if (itemStackIngredientsMatch(isPlayer, isIngredient)) {
                    if (target >= isPlayer.getCount()) {
                        // Consume this stack:
                        target -= isPlayer.getCount();
                        if (i >= main.size())
                            arm.get(i - main.size()).setCount(0);
                        else
                            main.get(i).setCount(0);
                    } else {
                        isPlayer.setCount(isPlayer.getCount() - target);
                        target = 0;
                    }
                }
            }
            ItemStack resultForReward = isIngredient.copy();
            RewardForDiscardingItemImplementation.LoseItemEvent event = new RewardForDiscardingItemImplementation.LoseItemEvent(resultForReward);
            MinecraftForge.EVENT_BUS.post(event);
        }
    }

    /**
     * Attempt to find all recipes that result in an item of the requested output.
     *
     * @param output the desired item, eg from Types.xsd - "diamond_pickaxe" etc - or as a Minecraft name - eg "tile.woolCarpet.blue"
     * @param variant if variants should be obeyed in constructing the recipes, i.e. if false, variant blind
     * @return a list of IRecipe objects that result in this item.
     */
    public static List<IRecipe> getRecipesForRequestedOutput(String output, boolean variant) {
        List<IRecipe> matchingRecipes = new ArrayList<IRecipe>();
        ItemStack target = MinecraftTypeHelper.getItemStackFromParameterString(output);
        List<?> recipes = CraftingManager.getInstance().getRecipeList();
        for (Object obj : recipes) {
            if (obj == null)
                continue;
            if (obj instanceof IRecipe) {
                ItemStack is = ((IRecipe) obj).getRecipeOutput();
                if (target == null)
                    continue;
                if (variant && ItemStack.areItemsEqual(is, target))
                    matchingRecipes.add((IRecipe) obj);
                else if (!variant && is.getItem() == target.getItem())
                    matchingRecipes.add((IRecipe) obj);
            }
        }
        return matchingRecipes;
    }

    /**
     * Attempt to find all recipes that result in an item of the requested output.
     *
     * @param output the desired item, eg from Types.xsd - "diamond_pickaxe" etc - or as a Minecraft name - eg "tile.woolCarpet.blue"
     * @param variant if variants should be obeyed in constructing the recipes, i.e. if false, variant blind
     * @return a list of IRecipe objects that result in this item.
     */
    public static List<IRecipe> getRecipesForRequestedOutput(ItemStack output, boolean variant) {
        List<IRecipe> matchingRecipes = new ArrayList<IRecipe>();
        List<?> recipes = CraftingManager.getInstance().getRecipeList();
        for (Object obj : recipes) {
            if (obj == null)
                continue;
            if (obj instanceof IRecipe) {
                ItemStack is = ((IRecipe) obj).getRecipeOutput();
                if (output == null)
                    continue;
                if (variant && ItemStack.areItemsEqual(is, output))
                    matchingRecipes.add((IRecipe) obj);
                else if (!variant && is.getItem() == output.getItem())
                    matchingRecipes.add((IRecipe) obj);
            }
        }
        return matchingRecipes;
    }

    /**
     * Attempt to find a smelting recipe that results in the requested output.
     *
     * @param output The output of the furnace burn
     * @return an ItemStack representing the required input.
     */
    public static ItemStack getSmeltingRecipeForRequestedOutput(String output) {
        ItemStack target = MinecraftTypeHelper.getItemStackFromParameterString(output);
        Iterator<?> furnaceIt = FurnaceRecipes.instance().getSmeltingList().keySet().iterator();
        while (furnaceIt.hasNext()) {
            ItemStack isInput = (ItemStack) furnaceIt.next();
            ItemStack isOutput = FurnaceRecipes.instance().getSmeltingList().get(isInput);
            if (itemStackIngredientsMatch(target, isOutput))
                return isInput;
        }
        return null;
    }

    /**
     * Attempt to craft the given recipe.<br>
     * This pays no attention to tedious things like using the right crafting table / brewing stand etc, or getting the right shape.<br>
     * It simply takes the raw ingredients out of the player's inventory, and inserts the output of the recipe, if possible.
     *
     * @param player the SERVER SIDE player that will do the crafting.
     * @param recipe the IRecipe we wish to craft.
     * @return true if the recipe had an output, and the player had the required ingredients to create it; false otherwise.
     */
    public static boolean attemptCrafting(EntityPlayerMP player, IRecipe recipe) {
        if (player == null || recipe == null)
            return false;

        ItemStack is = recipe.getRecipeOutput();

        List<ItemStack> ingredients = getIngredients(recipe);
        if (playerHasIngredients(player, ingredients)) {
            // We have the ingredients we need, so directly manipulate the inventory.
            // First, remove the ingredients:
            removeIngredientsFromPlayer(player, ingredients);
            // Now add the output of the recipe:
            ItemStack resultForInventory = is.copy();
            ItemStack resultForReward = is.copy();
            player.inventory.addItemStackToInventory(resultForInventory);
            RewardForCollectingItemImplementation.GainItemEvent event = new RewardForCollectingItemImplementation.GainItemEvent(resultForReward);
            event.setCause(1);
            MinecraftForge.EVENT_BUS.post(event);

            // Now trigger a craft event
            List<IRecipe> recipes = getRecipesForRequestedOutput(resultForReward, true);
            for (IRecipe iRecipe : recipes) {
                if (iRecipe instanceof ShapedRecipes) {
                    ShapedRecipes shapedRecipe = (ShapedRecipes) iRecipe;
                    InventoryCrafting craftMatrix;
                    if (shapedRecipe.recipeItems.length <= 4)
                        craftMatrix = new InventoryCrafting(player.inventoryContainer, 2, 2);
                    else
                        craftMatrix = new InventoryCrafting(player.inventoryContainer, 3, 3);
                    for (int i = 0; i < shapedRecipe.recipeItems.length; i++)
                        craftMatrix.setInventorySlotContents(i, shapedRecipe.recipeItems[i]);

                    MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, resultForReward, craftMatrix));
                    break;
                } else if (iRecipe instanceof ShapelessRecipes) {
                    ShapelessRecipes shapelessRecipe = (ShapelessRecipes) iRecipe;
                    InventoryCrafting craftMatrix;
                    if (shapelessRecipe.recipeItems.size() <= 4) {
                        craftMatrix = new InventoryCrafting(player.inventoryContainer, 2, 2);
                        for (int i = 0; i < shapelessRecipe.recipeItems.size(); i++)
                            craftMatrix.setInventorySlotContents(i, shapelessRecipe.recipeItems.get(i));
                    } else {
                        craftMatrix = new InventoryCrafting(player.inventoryContainer, 3, 3);
                        for (int i = 0; i < shapelessRecipe.recipeItems.size(); i++)
                            craftMatrix.setInventorySlotContents(i, shapelessRecipe.recipeItems.get(i));
                    }

                    MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, resultForReward, craftMatrix));
                    break;
                } else if (iRecipe instanceof ShapedOreRecipe) {
                    ShapedOreRecipe oreRecipe = (ShapedOreRecipe) iRecipe;
                    Object[] input = oreRecipe.getInput();
                    InventoryCrafting craftMatrix = new InventoryCrafting(player.inventoryContainer, 3, 3);
                    for (int i = 0; i < input.length; i++) {
                        if (input[i] instanceof ItemStack)
                            craftMatrix.setInventorySlotContents(i, (ItemStack) input[i]);
                        else if (input[i] instanceof NonNullList)
                            if (((NonNullList) input[i]).size() != 0)
                                craftMatrix.setInventorySlotContents(i, (ItemStack) ((NonNullList) input[i]).get(0));
                    }

                    MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, resultForReward, craftMatrix));
                }
            }

            return true;
        }
        return false;
    }

    /**
     * Attempt to smelt the given item.<br>
     * This returns instantly, callously disregarding such frivolous niceties as cooking times or the presence of a furnace.<br>
     * It will, however, consume fuel from the player's inventory.
     *
     * @param player
     * @param input  the raw ingredients we want to cook.
     * @return true if cooking was successful.
     */
    public static boolean attemptSmelting(EntityPlayerMP player, ItemStack input) {
        if (player == null || input == null)
            return false;
        List<ItemStack> ingredients = new ArrayList<ItemStack>();
        ingredients.add(input);
        ItemStack isOutput = FurnaceRecipes.instance().getSmeltingList().get(input);
        if (isOutput == null)
            return false;
        int cookingTime = 200;  // Seems to be hard-coded in TileEntityFurnace.
        if (playerHasIngredients(player, ingredients) && totalBurnTimeInInventory(player) >= cookingTime) {
            removeIngredientsFromPlayer(player, ingredients);
            burnInventory(player, cookingTime, input);

            ItemStack resultForInventory = isOutput.copy();
            ItemStack resultForReward = isOutput.copy();
            player.inventory.addItemStackToInventory(resultForInventory);
            RewardForCollectingItemImplementation.GainItemEvent event = new RewardForCollectingItemImplementation.GainItemEvent(resultForReward);
            event.setCause(2);
            MinecraftForge.EVENT_BUS.post(event);

            // Now trigger a smelt event
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemSmeltedEvent(player, resultForReward));
            return true;
        }
        return false;
    }

    /**
     * Little utility method for dumping out a list of all the Minecraft items, plus as many useful attributes as
     * we can find for them. This is primarily used by decision_tree_test.py but might be useful for real-world applications too.
     *
     * @param filename location to save the dumped list.
     * @throws IOException
     */
    public static void dumpItemProperties(String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream("..//..//build//install//Python_Examples//item_database.json");
        OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8");
        BufferedWriter writer = new BufferedWriter(osw);
        JsonArray itemTypes = new JsonArray();
        for (ResourceLocation i : Item.REGISTRY.getKeys()) {
            Item item = Item.REGISTRY.getObject(i);
            if (item != null) {
                JsonObject json = new JsonObject();
                json.addProperty("type", Item.REGISTRY.getNameForObject(item).toString().replace("minecraft:", ""));
                json.addProperty("damageable", item.isDamageable());
                json.addProperty("rendersIn3D", item.isFull3D());
                json.addProperty("repairable", item.isRepairable());
                CreativeTabs tab = item.getCreativeTab();
                json.addProperty("tab", ((tab != null) ? item.getCreativeTab().getTabLabel() : "none"));
                ItemStack is = item.getDefaultInstance();
                json.addProperty("stackable", is.isStackable());
                json.addProperty("enchantable", is.isItemEnchantable());
                json.addProperty("rare", (is.getRarity() == EnumRarity.RARE));    // Enum has four types, but only two (COMMON and RARE) appear to be used.
                json.addProperty("action", is.getItemUseAction().toString());
                json.addProperty("hasSubtypes", item.getHasSubtypes());
                json.addProperty("maxDamage", is.getMaxDamage());
                json.addProperty("maxUseDuration", is.getMaxItemUseDuration());
                json.addProperty("block", item instanceof ItemBlock);
                json.addProperty("hasContainerItem", item.hasContainerItem());
                if (item instanceof ItemBlock) {
                    ItemBlock ib = (ItemBlock) item;
                    Block b = ib.getBlock();
                    IBlockState bs = b.getDefaultState();
                    json.addProperty("slipperiness", b.slipperiness);
                    json.addProperty("hardness", bs.getBlockHardness(null, null));
                    json.addProperty("causesSuffocation", bs.causesSuffocation());
                    json.addProperty("canProvidePower", bs.canProvidePower());
                    json.addProperty("translucent", bs.isTranslucent());
                    Material mat = bs.getMaterial();
                    if (mat != null) {
                        json.addProperty("canBurn", mat.getCanBurn());
                        json.addProperty("isLiquid", mat.isLiquid());
                        json.addProperty("blocksMovement", mat.blocksMovement());
                        json.addProperty("needsNoTool", mat.isToolNotRequired());
                        json.addProperty("isReplaceable", mat.isReplaceable());
                        json.addProperty("pistonPushable", mat.getMobilityFlag() == EnumPushReaction.NORMAL);
                        json.addProperty("woodenMaterial", mat == Material.WOOD);
                        json.addProperty("ironMaterial", mat == Material.IRON);
                        json.addProperty("glassyMaterial", mat == Material.GLASS);
                        json.addProperty("clothMaterial", mat == Material.CLOTH);
                    }

                    boolean hasDirection = false;
                    boolean hasColour = false;
                    boolean hasVariant = false;
                    for (IProperty prop : bs.getProperties().keySet()) {
                        System.out.println(Item.REGISTRY.getNameForObject(item).toString() + " -- " + prop);
                        if (prop instanceof PropertyDirection)
                            hasDirection = true;
                        if (prop instanceof PropertyEnum && prop.getName().equals("color"))
                            hasColour = true;
                        if (prop instanceof PropertyEnum && prop.getName().equals("variant")) {
                            hasVariant = true;
                            json.addProperty("variant", bs.getValue(prop).toString());
                        }
                    }
                    json.addProperty("hasDirection", hasDirection);
                    json.addProperty("hasColour", hasColour);
                    json.addProperty("hasVariant", hasVariant);
                }
                itemTypes.add(json);
            }
        }
        writer.write(itemTypes.toString());
        writer.close();
    }

    /**
     * Little utility method for dumping out a list of all the recipes we understand.
     *
     * @param filename location to save the dumped list.
     * @throws IOException
     */
    public static void dumpRecipes(String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8");
        BufferedWriter writer = new BufferedWriter(osw);
        List<?> recipes = CraftingManager.getInstance().getRecipeList();
        for (Object obj : recipes) {
            if (obj == null)
                continue;
            if (obj instanceof IRecipe) {
                ItemStack is = ((IRecipe) obj).getRecipeOutput();
                if (is == null)
                    continue;
                String s = is.getCount() + "x" + is.getUnlocalizedName() + " = ";
                List<ItemStack> ingredients = getIngredients((IRecipe) obj);
                if (ingredients == null)
                    continue;
                boolean first = true;
                for (ItemStack isIngredient : ingredients) {
                    if (!first)
                        s += ", ";
                    s += isIngredient.getCount() + "x" + isIngredient.getUnlocalizedName();
                    s += "(" + isIngredient.getDisplayName() + ")";
                    first = false;
                }
                s += "\n";
                writer.write(s);
            }
        }
        Iterator<?> furnaceIt = FurnaceRecipes.instance().getSmeltingList().keySet().iterator();
        while (furnaceIt.hasNext()) {
            ItemStack isInput = (ItemStack) furnaceIt.next();
            ItemStack isOutput = (ItemStack) FurnaceRecipes.instance().getSmeltingList().get(isInput);
            String s = isOutput.getCount() + "x" + isOutput.getUnlocalizedName() + " = FUEL + " + isInput.getCount() + "x" + isInput.getUnlocalizedName() + "\n";
            writer.write(s);
        }
        writer.close();
    }
}