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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.microsoft.Malmo.Schemas.Colour;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.EntityTypes;
import com.microsoft.Malmo.Schemas.Facing;
import com.microsoft.Malmo.Schemas.FlowerTypes;
import com.microsoft.Malmo.Schemas.StoneTypes;
import com.microsoft.Malmo.Schemas.Variation;
import com.microsoft.Malmo.Schemas.WoodTypes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockFlower.EnumFlowerType;
import net.minecraft.block.BlockLever.EnumOrientation;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

/**
 *  Utility functions for dealing with Minecraft block types, item types, etc.
 */
public class MinecraftTypeHelper
{
    /**
     * Attempts to parse the block type string.
     * @param s The string to parse.
     * @return The block type, or null if the string is not recognised.
     */
    public static IBlockState ParseBlockType( String s )
    {
        if( s == null )
            return null; 
        Block block = (Block)Block.blockRegistry.getObject(new ResourceLocation( s ));
        if( block instanceof BlockAir && !s.equals("air") ) // Minecraft returns BlockAir when it doesn't recognise the string
            return null; // unrecognised string
        return block.getDefaultState();
    }

    /**
     * Attempts to parse the item type string.
     * @param s The string to parse.
     * @param checkBlocks if string can't be parsed as an item, attempt to parse as a block, if checkBlocks is true
     * @return The item type, or null if the string is not recognised.
     */
    public static Item ParseItemType( String s, boolean checkBlocks )
    {
        if (s == null)
            return null;
        Item item = (Item)Item.itemRegistry.getObject(new ResourceLocation(s)); // Minecraft returns null when it doesn't recognise the string
        if (item == null && checkBlocks)
        {
            // Maybe this is a request for a block item?
            IBlockState block = MinecraftTypeHelper.ParseBlockType(s);
            item = (block != null && block.getBlock() != null) ? Item.getItemFromBlock(block.getBlock()) : null;
        }
        return item;
    }

    /** Test whether this block has a colour attribute which matches the list of allowed colours
     * @param bs blockstate to test
     * @param allowedColours list of allowed Colour enum values
     * @return true if the block matches.
     */
    public static boolean blockColourMatches(IBlockState bs, List<Colour> allowedColours)
    {
        for (IProperty prop : (java.util.Set<IProperty>) bs.getProperties().keySet())
        {
            if (prop.getName().equals("color") && prop.getValueClass() == net.minecraft.item.EnumDyeColor.class)
            {
                // The block in question has a colour, so check it is a specified one:
                net.minecraft.item.EnumDyeColor current = (net.minecraft.item.EnumDyeColor)bs.getValue(prop);
                for (Colour col : allowedColours)
                {
                    if (current.getName().equalsIgnoreCase(col.name()))
                        return true;
                }
            }
        }
        return false;
    }

    /** Test whether this block has a variant attribute which matches the list of allowed variants
     * @param bs the blockstate to test
     * @param allowedVariants list of allowed Variant enum values
     * @return true if the block matches.
     */
    public static boolean blockVariantMatches(IBlockState bs, List<Variation> allowedVariants)
    {
        for (IProperty prop : (java.util.Set<IProperty>) bs.getProperties().keySet())
        {
            if (prop.getName().equals("variant") && prop.getValueClass().isEnum())
            {
                Object current = bs.getValue(prop);
                if (current != null)
                {
                    for (Variation var : allowedVariants)
                    {
                        if (var.getValue().equalsIgnoreCase(current.toString()))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    /** Attempt to parse string as a Colour
     * @param part string token to parse
     * @return the Colour enum value for the requested colour, or null if it wasn't valid.
     */
    public static Colour attemptToGetAsColour(String part)
    {
        Colour col = null;
        try
        {
            col = Colour.valueOf(part.toUpperCase());
        }
        catch (Exception e)
        {
            // Does nothing.
        }
        return col;
    }

    /** Attempt to parse string as a BlockVariant
     * @param part string token to parse
     * @return the BlockVariant enum value for the requested variant, or null if it wasn't valid.
     */
    public static Variation attemptToGetAsVariant(String part)
    {
        // Annoyingly JAXB won't bind Variantion as an enum, so we have to do this manually.
        // TODO - can we do something more clever... eg make StoneTypes, WoodTypes etc inherit from an XSD baseclass,
        // and have an object in the schemas that returns a list, so we can just iterate...
        try
        {
            StoneTypes var = StoneTypes.valueOf(part.toUpperCase());
            if (var != null)
            {
                Variation bv = new Variation();
                bv.setValue(var.value());
                return bv;
            }
        }
        catch (Exception e)
        {
            // Does nothing.
        }
        try
        {
            WoodTypes var = WoodTypes.valueOf(part.toUpperCase());
            if (var != null)
            {
                Variation bv = new Variation();
                bv.setValue(var.value());
                return bv;
            }
        }
        catch (Exception e)
        {
            // Does nothing.
        }
        try
        {
            FlowerTypes var = FlowerTypes.fromValue(part);
            if (var != null)
            {
                Variation bv = new Variation();
                bv.setValue(var.value());
                return bv;
            }
        }
        catch (Exception e)
        {
            // Does nothing.
        }
        try
        {
            EntityTypes var = EntityTypes.fromValue(part);
            if (var != null)
            {
                Variation bv = new Variation();
                bv.setValue(var.value());
                return bv;
            }
        }
        catch (Exception e)
        {
            // Does nothing.
        }
        try
        {
            MonsterEggTypes var = MonsterEggTypes.fromValue(part);
            if (var != null)
            {
                Variation bv = new Variation();
                bv.setValue(var.value());
                return bv;
            }
        }
        catch (Exception e)
        {
            // Does nothing.
        }
        return null;
    }

    /** Attempt to break the item on this itemstack into a type/variant/colour which we can use for communication with the Malmo platform.
     * @param is the ItemStack containing the item we are attempting to deconstruct.
     * @return an XML DrawItem object containing the item's type, variant, colour etc.
     */
    public static DrawItem getDrawItemFromItemStack(ItemStack is)
    {
        if (is == null)
            return null;

        DrawItem di = new DrawItem();
        String name = is.getUnlocalizedName();  // Get unlocalised name from the stack, not the stack's item - this ensures we keep the metadata.
        if (is.getHasSubtypes())
        {
            // If the item has subtypes, then there are varieties - eg different colours, types, etc.
            // Attempt to map from these subtypes back to variant/colour.
            // Do this by decomposing the unlocalised name:
            List<String> itemParts = new ArrayList<String>(Arrays.asList(name.split("\\.")));
            if (is.getItem() instanceof ItemMonsterPlacer)
            {
                // Special case for eggs:
                itemParts.add(ItemMonsterPlacer.getEntityName(is));
            }
            // First part will be "tile" or "item".
            // Second part will be the item itself (eg "dyePowder" or "stainedGlass" etc).
            // Third part will be the variant, colour etc.
            Colour col = null;
            Variation var = null;
            for (int part = 2; part < itemParts.size(); part++)
            {
                // First see if this matches a colour:
                if (col == null)
                {
                    col = attemptToGetAsColour(itemParts.get(part));
                    if (col == null && var == null) // If it wasn't a colour, check to see if it was a variant:
                        var = attemptToGetAsVariant(itemParts.get(part));
                }
                else if (var == null)
                    var = attemptToGetAsVariant(itemParts.get(part));
            }
            di.setColour(col);
            di.setVariant(var);
        }
        // Use the item registry name for the item - this is what we use in Types.XSD
        Object obj = Item.itemRegistry.getNameForObject(is.getItem());
        String publicName = obj.toString();
        di.setType(publicName + "__" + metadata);
        return di;
    }

    /** Take an item name and attempt to turn it into a Minecraft unlocalised name.
     * @param itemName eg from Types.xsd - "diamond_pickaxe", "gold_block" etc.
     * @return the Minecraft internal unlocalised name - eg "item.diamondPickaxe", "tile.goldBlock" etc.
     */
    public static String getUnlocalizedNameFromString(String parameters)
    {
        // Split into parameters:
        List<String> params = new ArrayList<String>(Arrays.asList(parameters.split(" ")));
        Colour col = null;
        Variation var = null;

        // See if any parameters appear to be a colour:
        Iterator<String> it = params.iterator();
        while (it.hasNext() && col == null)
        {
            col = MinecraftTypeHelper.attemptToGetAsColour(it.next());
            if (col != null)
                it.remove();    // This parameter was a colour - we've parsed it, so remove it.
        }

        // See if any parameters appear to be a variant:
        it = params.iterator();
        while (it.hasNext() && var == null)
        {
            var = MinecraftTypeHelper.attemptToGetAsVariant(it.next());
            if (var != null)
                it.remove();    // This parameter was a variant - we've parsed it, so remove it.
        }

        // Hopefully we have at most one parameter left, which will be the type.
        if (params.size() == 0)
            return parameters;  // Dunno what to do, really.

        String itemName = params.get(0);
        String minecraftName = "";
        // Attempt to parse as a block:
        IBlockState block = MinecraftTypeHelper.ParseBlockType(itemName);
        if (block != null)
        {
            block = BlockDrawingHelper.applyModifications(block, col, null, var);
            minecraftName = block.getBlock().getUnlocalizedName();
        }
        else
        {
            // Attempt to parse as an item:
            DrawItem di = new DrawItem();
            di.setColour(col);
            di.setVariant(var);
            di.setType(itemName);
            ItemStack item = getItemStackFromDrawItem(di);
            if (item != null)
                minecraftName = item.getUnlocalizedName();
            else
            {
                // Assume we were given a minecraft description to begin with - eg "tile.carpet.white", or whatever.
                minecraftName = itemName;
            }
        }
        return minecraftName;
    }

    public static ItemStack getItemStackFromDrawItem(DrawItem i)
    {
        // First see if this is a block item:
        ItemStack itemStack = null;
        IBlockState block = MinecraftTypeHelper.ParseBlockType(i.getType());
        if (block != null)
        {
            // It is - apply the modifications:
            block = BlockDrawingHelper.applyModifications(block, i.getColour(), i.getFace(), i.getVariant());
            // And try to return as an item:
            itemStack = (block != null && block.getBlock() != null) ? new ItemStack(block.getBlock(), 1, block.getBlock().getMetaFromState(block)) : null;
        }
        // If that failed, try to get the straight item:
        if (itemStack == null)
        {
            Item item = MinecraftTypeHelper.ParseItemType(i.getType(), false);
            if (item != null && item.getHasSubtypes() && (i.getColour() != null || i.getVariant() != null))
            {
                // Attempt to find the subtype for this colour/variant - made tricky
                // because Items don't provide any nice property stuff like Blocks do...
                List<ItemStack> subItems = new ArrayList<ItemStack>();
                item.getSubItems(item, null, subItems);
                String match = (i.getColour() != null) ? i.getColour().value().toLowerCase() : i.getVariant().getValue().toLowerCase();
                
                for (ItemStack is : subItems)
                {
                    String fullName = is.getUnlocalizedName();
                    String[] parts = fullName.split("\\.");
                    for (int p = 0; p < parts.length; p++)
                    {
                        if (parts[p].equals(match))
                        {
                            // This is it??
                            return is;
                        }
                    }
                    System.out.println(parts);
                }
            }
            itemStack = new ItemStack(item);
        }
        return itemStack;
    }

	/** Select the request variant of the Minecraft block, if applicable
	 * @param state The block to be varied
	 * @param colour The new variation
	 * @return A new blockstate which is the requested variant of the original, if such a variant exists; otherwise it returns the original block.
	 */
	static IBlockState applyVariant(IBlockState state, Variation variant)
	{
	    for (IProperty prop : (java.util.Set<IProperty>)state.getProperties().keySet())
	    {
	        if (prop.getName().equals("variant") && prop.getValueClass().isEnum())
	        {
	            Object[] values = prop.getValueClass().getEnumConstants();
	            for (Object obj : values)
	            {
	                if (obj != null && obj.toString().equalsIgnoreCase(variant.getValue()))
	                {
	                    return state.withProperty(prop, (Comparable)obj);
	                }
	            }
	        }
	    }
	    return state;
	}

	/** Recolour the Minecraft block
	 * @param state The block to be recoloured
	 * @param colour The new colour
	 * @return A new blockstate which is a recoloured version of the original
	 */
	static IBlockState applyColour(IBlockState state, Colour colour)
	{
	    for (IProperty prop : (java.util.Set<IProperty>)state.getProperties().keySet())
	    {
	        if (prop.getName().equals("color") && prop.getValueClass() == net.minecraft.item.EnumDyeColor.class)
	        {
	            net.minecraft.item.EnumDyeColor current = (net.minecraft.item.EnumDyeColor)state.getValue(prop);
	            if (!current.getName().equalsIgnoreCase(colour.name()))
	            {
	                return state.withProperty(prop, EnumDyeColor.valueOf(colour.name()));
	            }
	        }
	    }
	    return state;
	}

	/** Change the facing attribute of the Minecraft block
	 * @param state The block to be edited
	 * @param facing The new direction (N/S/E/W/U/D)
	 * @return A new blockstate with the facing attribute edited
	 */
	static IBlockState applyFacing(IBlockState state, Facing facing)
	{
	    for (IProperty prop : (java.util.Set<IProperty>)state.getProperties().keySet())
	    {
	        if (prop.getName().equals("facing"))
	        {
	        	if(prop.getValueClass() == EnumFacing.class)
	        	{
	        		EnumFacing current = (EnumFacing)state.getValue(prop);
	                if (!current.getName().equalsIgnoreCase(facing.name()))
	                {
	                    return state.withProperty(prop, EnumFacing.valueOf(facing.name()));
	                }
	        	}
	        	else if(prop.getValueClass() == EnumOrientation.class)
	        	{
	        		EnumOrientation current = (EnumOrientation)state.getValue(prop);
	                if (!current.getName().equalsIgnoreCase(facing.name()))
	                {
	                    return state.withProperty(prop, EnumOrientation.valueOf(facing.name()));
	                }
	        	}
	        }
	    }
	    return state;
	}
}
