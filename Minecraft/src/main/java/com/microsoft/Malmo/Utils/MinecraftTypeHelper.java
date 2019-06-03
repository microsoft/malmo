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

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLever.EnumOrientation;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import com.microsoft.Malmo.Schemas.BlockType;
import com.microsoft.Malmo.Schemas.Colour;
import com.microsoft.Malmo.Schemas.DrawBlock;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.EntityTypes;
import com.microsoft.Malmo.Schemas.Facing;
import com.microsoft.Malmo.Schemas.FlowerTypes;
import com.microsoft.Malmo.Schemas.HalfTypes;
import com.microsoft.Malmo.Schemas.MonsterEggTypes;
import com.microsoft.Malmo.Schemas.ShapeTypes;
import com.microsoft.Malmo.Schemas.StoneTypes;
import com.microsoft.Malmo.Schemas.Variation;
import com.microsoft.Malmo.Schemas.WoodTypes;

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
        Block block = (Block)Block.REGISTRY.getObject(new ResourceLocation( s ));
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
        Item item = (Item)Item.REGISTRY.getObject(new ResourceLocation(s)); // Minecraft returns null when it doesn't recognise the string
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
        for (IProperty prop : bs.getProperties().keySet())
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
        for (IProperty prop : bs.getProperties().keySet())
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
        String target = part.toUpperCase();
        for (int i = 0; i < Colour.values().length; i++)
        {
            String col = Colour.values()[i].name().replace("_",  "");
            if (col.equals(target))
                return Colour.values()[i];
        }
        return null;
    }
    
    public static Facing attemptToGetAsFacing(String part)
    {
        Facing face = null;
        try
        {
            face = Facing.valueOf(part);
        }
        catch (Exception e)
        {
            // Does nothing.
        }
        return face;
    }

    /** Attempt to parse string as a Variation, allowing for block properties having different names to the enum values<br>
     * (eg blue_orchid vs orchidBlue etc.)
     * @param part the string (potentially in the 'wrong' format, eg 'orchidBlue')
     * @param is the ItemStack from which this string came (eg from is.getUnlocalisedName)
     * @return a Variation, if one exists, that matches the part string passed in, or one of the ItemStacks current property values.
     */
    public static Variation attemptToGetAsVariant(String part, ItemStack is)
    {
        if (is.getItem() instanceof ItemBlock)
        {
            // Unlocalised name doesn't always match the names we use in types.xsd
            // (which are the names displayed by Minecraft when using the F3 debug etc.)
            ItemBlock ib = (ItemBlock)(is.getItem());
            IBlockState bs = ib.block.getStateFromMeta(is.getMetadata());
            for (IProperty prop : bs.getProperties().keySet())
            { 
                Comparable<?> comp = bs.getValue(prop);
                Variation var = attemptToGetAsVariant(comp.toString());
                if (var != null)
                    return var;
            }
            return null;
        }
        else
            return attemptToGetAsVariant(part);
    }

    /** Attempt to parse string as a Variation
     * @param part string token to parse
     * @return the BlockVariant enum value for the requested variant, or null if it wasn't valid.
     */
    public static Variation attemptToGetAsVariant(String part)
    {
        // Annoyingly JAXB won't bind Variation as an enum, so we have to do this manually.
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
        try
        {
            ShapeTypes var = ShapeTypes.fromValue(part);
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
            HalfTypes var = HalfTypes.fromValue(part);
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

    /** Extract the type, variation and facing attributes of a blockstate and return them in a new DrawBlock object.<br>
     * @param state the IBlockState to be examined
     * @return A DrawBlock object
     */
    public static DrawBlock getDrawBlockFromBlockState(IBlockState state, List<IProperty> extraProperties)
    {
        if (state == null)
            return null;

        DrawBlock block = new DrawBlock();
        Object blockName = Block.REGISTRY.getNameForObject(state.getBlock());
        if (blockName instanceof ResourceLocation)
        {
            String name = ((ResourceLocation)blockName).getResourcePath();
            BlockType type = BlockType.fromValue(name);
            block.setType(type);
        }

        Colour col = null;
        Variation var = null;
        Facing face = null;

        // Add properties:
        for (IProperty prop : state.getProperties().keySet())
        {
            String propVal = state.getValue(prop).toString();
            boolean matched = false;
            // Try colour first:
            if (col == null)
            {
                col = attemptToGetAsColour(propVal);
                if (col != null)
                    matched = true;
            }
            // Then variant:
            if (!matched && var == null)
            {
                var = attemptToGetAsVariant(propVal);
                if (var != null)
                    matched = true;
            }
            // Then facing:
            if (!matched && face == null)
            {
                face = attemptToGetAsFacing(propVal);
                if (face != null)
                    matched = true;
            }
            if (!matched)
            {
                if (extraProperties != null)
                    extraProperties.add(prop);
            }
        }
        if (col != null)
            block.setColour(col);
        if (var != null)
            block.setVariant(var);
        if (face != null)
            block.setFace(face);
        return block;
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
                itemParts.add(ItemMonsterPlacer.getNamedIdFrom(is).toString());
            }
            // First part will be "tile" or "item".
            // Second part will be the item itself (eg "dyePowder" or "stainedGlass" etc).
            // Third part will be the variant, colour etc.
            Colour col = null;
            Variation var = null;
            for (int part = 2; part < itemParts.size(); part++)
            {
                String section = itemParts.get(part);
                // First see if this matches a colour:
                if (col == null)
                {
                    col = attemptToGetAsColour(section);
                    if (col == null && var == null) // If it wasn't a colour, check to see if it was a variant:
                        var = attemptToGetAsVariant(section, is);
                }
                else if (var == null)
                    var = attemptToGetAsVariant(section, is);
            }
            di.setColour(col);
            di.setVariant(var);
        }
        // Use the item registry name for the item - this is what we use in Types.XSD
        Object obj = Item.REGISTRY.getNameForObject(is.getItem());
        String publicName;
        if (obj instanceof ResourceLocation)
            publicName = ((ResourceLocation)obj).getResourcePath();
        else
            publicName = obj.toString();
        di.setType(publicName);
        return di;
    }

    /** Take a string of parameters, delimited by spaces, and create an ItemStack from it.
     * @param parameters the item name, variation, colour etc of the required item, separated by spaces.
     * @return an Itemstack for these parameters, or null if unrecognised.
     */
    public static ItemStack getItemStackFromParameterString(String parameters)
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
            return null;  // Dunno what to do, really.

        String itemName = params.get(0);
        DrawItem di = new DrawItem();
        di.setColour(col);
        di.setVariant(var);
        di.setType(itemName);
        return getItemStackFromDrawItem(di);
    }

    public static ItemStack getItemStackFromDrawItem(DrawItem i)
    {
        ItemStack itemStack = null;
        // First see if this is an item:
        Item item = MinecraftTypeHelper.ParseItemType(i.getType(), false);
        if (item == null)
        {
            // No, so is it a block type?
            IBlockState block = MinecraftTypeHelper.ParseBlockType(i.getType());
            if (block != null)
            {
                // It is - apply the modifications:
                block = BlockDrawingHelper.applyModifications(block, i.getColour(), i.getFace(), i.getVariant());
                // And try to return as an item:
                if (block != null && block.getBlock() != null && Item.getItemFromBlock(block.getBlock()) != null)
                {
                    itemStack = new ItemStack(block.getBlock(), 1, block.getBlock().getMetaFromState(block));
                }
            }
        }
        else
        {
            if (item.getHasSubtypes() && (i.getColour() != null || i.getVariant() != null))
            {
                // Attempt to find the subtype for this colour/variant - made tricky
                // because Items don't provide any nice property stuff like Blocks do...
                NonNullList<ItemStack> subItems = NonNullList.create();
                item.getSubItems(item, null, subItems);

                for (ItemStack is : subItems)
                {
                    String fullName = is.getUnlocalizedName();
                    if (is.getItem() instanceof ItemMonsterPlacer)
                    {
                        fullName += "." + ItemMonsterPlacer.getNamedIdFrom(is).toString();  // Special handling for eggs
                    }
                    String[] parts = fullName.split("\\.");
                    for (int p = 0; p < parts.length; p++)
                    {
                        Variation v = attemptToGetAsVariant(parts[p], is);
                        Colour c = attemptToGetAsColour(parts[p]);
                        if ((v != null && i.getVariant() != null && v.getValue().equals(i.getVariant().getValue())) || (c != null && i.getColour() != null && c == i.getColour()))
                        {
                            // This is it??
                            return is;
                        }
                    }
                }
            }
            itemStack = new ItemStack(item);
        }
        return itemStack;
    }

	/** Select the request variant of the Minecraft block, if applicable
	 * @param state The block to be varied
	 * @param variant The new variation
	 * @return A new blockstate which is the requested variant of the original, if such a variant exists; otherwise it returns the original block.
	 */
    static IBlockState applyVariant(IBlockState state, Variation variant)
    {
        // Try the variant property first - if that fails, look for other properties that match the supplied variant.
        boolean relaxRequirements = false;
        for (int i = 0; i < 2; i++)
        {
            for (IProperty prop : state.getProperties().keySet())
            {
                if ((prop.getName().equals("variant") || relaxRequirements) && prop.getValueClass().isEnum())
                {
                    Object[] values = prop.getValueClass().getEnumConstants();
                    for (Object obj : values)
                    {
                        if (obj != null && obj.toString().equalsIgnoreCase(variant.getValue()))
                        {
                            return state.withProperty(prop, (Comparable) obj);
                        }
                    }
                }
            }
            relaxRequirements = true;   // Failed to set the variant, so try again with other properties.
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
	    for (IProperty prop : state.getProperties().keySet())
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
        for (IProperty prop : state.getProperties().keySet())
        {
            if (prop.getName().equals("facing"))
            {
                if (prop.getValueClass() == EnumFacing.class)
                {
                    EnumFacing current = (EnumFacing) state.getValue(prop);
                    if (!current.getName().equalsIgnoreCase(facing.name()))
                    {
                        return state.withProperty(prop, EnumFacing.valueOf(facing.name()));
                    }
                }
                else if (prop.getValueClass() == EnumOrientation.class)
                {
                    EnumOrientation current = (EnumOrientation) state.getValue(prop);
                    if (!current.getName().equalsIgnoreCase(facing.name()))
                    {
                        return state.withProperty(prop, EnumOrientation.valueOf(facing.name()));
                    }
                }
            }
        }
        return state;
    }

    /** Does essentially the same as entity.getName(), but without pushing the result
     * through the translation layer. This ensures the result matches what we use in Types.XSD,
     * and prevents things like "entity.ShulkerBullet.name" being returned, where there is no
     * translation provided in the .lang file.
     * @param e The entity
     * @return The entity's name.
     */
    public static String getUnlocalisedEntityName(Entity e)
    {
        String name;
        if (e.hasCustomName())
        {
            name = e.getCustomNameTag();
        }
        else if (e instanceof EntityPlayer)
        {
            name = e.getName(); // Just returns the user name
        }
        else
        {
            name = EntityList.getEntityString(e);
            if (name == null)
                name = "unknown";
        }
        return name;
    }
}
