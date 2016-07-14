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

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
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
     * @return The item type, or null if the string is not recognised.
     */
    public static Item ParseItemType( String s )
    {
        if (s == null)
            return null;
        Item item = (Item)Item.itemRegistry.getObject(new ResourceLocation(s)); // Minecraft returns null when it doesn't recognise the string
        if (item == null)
        {
            // Maybe this is a request for a block item?
            IBlockState block = MinecraftTypeHelper.ParseBlockType(s);
            item = (block != null && block.getBlock() != null) ? Item.getItemFromBlock(block.getBlock()) : null;
        }
        return item;
    }
}
