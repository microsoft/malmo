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
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.Schemas.BlockOrItemSpec;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithReward;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.Variation;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

public abstract class RewardForItemBase extends RewardBase
{
    List<ItemRewardMatcher> rewardMatchers = new ArrayList<ItemRewardMatcher>();
    
    public static class ItemMatcher
    {
        List<String> allowedItemTypes = new ArrayList<String>();
        BlockOrItemSpec matchSpec;

        ItemMatcher(BlockOrItemSpec spec)
        {
            this.matchSpec = spec;

            for (String itemType : spec.getType())
            {
                Item item = MinecraftTypeHelper.ParseItemType(itemType, true);
                if (item != null)
                    this.allowedItemTypes.add(item.getUnlocalizedName());
            }
        }

        boolean matches(ItemStack stack)
        {
            String item = stack.getItem().getUnlocalizedName();
            if (item.equals("log") || item.equals("log2")){
                if (!this.allowedItemTypes.contains("log") && !this.allowedItemTypes.contains("log2"))
                    return false;
            }
            else if (!this.allowedItemTypes.contains(item))
                return false;

            // Our item type matches, but we may need to compare block attributes too:
            DrawItem di = MinecraftTypeHelper.getDrawItemFromItemStack(stack);
            if (this.matchSpec.getColour() != null && !this.matchSpec.getColour().isEmpty()) // We have a colour list, so check colour matches:
            {
                if (di.getColour() == null)
                    return false;   // The item we are matching against has no colour attribute.
                if (!this.matchSpec.getColour().contains(di.getColour()))
                    return false;   // The item we are matching against is the wrong colour.
            }
            if (this.matchSpec.getVariant() != null && !this.matchSpec.getVariant().isEmpty()) // We have a variant list, so check variant matches@:
            {
                if (di.getVariant() == null)
                    return false;   // The item we are matching against has no variant attribute.
                for (Variation v : this.matchSpec.getVariant())
                {
                    if (v.getValue().equals(di.getVariant().getValue()))
                        return true;
                }
                return false;   // The item we are matching against is the wrong variant.
            }
            return true;
        }
    }

    public class ItemRewardMatcher extends ItemMatcher
    {
        float reward;
        String distribution;

        ItemRewardMatcher(BlockOrItemSpecWithReward spec)
        {
            super(spec);
            this.reward = spec.getReward().floatValue();
            this.distribution = spec.getDistribution();
        }

        float reward()
        {
            return this.reward;
        }
        
        String distribution()
        {
            return this.distribution;
        }
    }

    protected void addItemSpecToRewardStructure(BlockOrItemSpecWithReward is)
    {
        this.rewardMatchers.add(new ItemRewardMatcher(is));
    }

    protected void accumulateReward(int dimension, ItemStack stack)
    {
        for (ItemRewardMatcher matcher : this.rewardMatchers)
        {
            if (matcher.matches(stack))
            {
                addAndShareCachedReward(dimension, stack.getCount() * matcher.reward(), matcher.distribution());
            }
        }
    }

    protected static void sendItemStackToClient(EntityPlayerMP player, MalmoMessageType message, ItemStack is)
    {
        ByteBuf buf = Unpooled.buffer();
        ByteBufUtils.writeItemStack(buf, is);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(0, bytes);
        String data = DatatypeConverter.printBase64Binary(bytes);
        MalmoMod.MalmoMessage msg = new MalmoMod.MalmoMessage(message, data);
        MalmoMod.network.sendTo(msg, player);
    }
}
