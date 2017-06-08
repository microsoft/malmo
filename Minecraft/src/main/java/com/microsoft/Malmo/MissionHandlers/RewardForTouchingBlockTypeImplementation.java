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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.Behaviour;
import com.microsoft.Malmo.Schemas.BlockSpecWithRewardAndBehaviour;
import com.microsoft.Malmo.Schemas.BlockType;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForTouchingBlockType;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;
import com.microsoft.Malmo.Utils.PositionHelper;

public class RewardForTouchingBlockTypeImplementation extends RewardBase implements IRewardProducer {
    private class BlockMatcher {
        boolean hasFired = false;
        BlockSpecWithRewardAndBehaviour spec;
        ArrayList<String> allowedBlockNames;
        ArrayList<BlockPos> firedBlocks = new ArrayList<BlockPos>();
        long lastFired;
        
        BlockMatcher(BlockSpecWithRewardAndBehaviour spec) {
            this.spec = spec;

            // Get the allowed blocks:
            // (Convert from the enum name to the unlocalised name.)
            this.allowedBlockNames = new ArrayList<String>();
            List<BlockType> allowedTypes = spec.getType();
            if (allowedTypes != null) {
                for (BlockType bt : allowedTypes) {
                    Block b = Block.getBlockFromName(bt.value());
                    this.allowedBlockNames.add(b.getUnlocalizedName());
                }
            }
        }

        boolean applies(BlockPos bp) {
            switch (this.spec.getBehaviour()) {
            case ONCE_ONLY:
                return !this.hasFired;

            case ONCE_PER_BLOCK:
                return !this.firedBlocks.contains(bp);

            case ONCE_PER_TIME_SPAN:
                return this.spec.getCooldownInMs().floatValue() < System.currentTimeMillis() - this.lastFired;

            case CONSTANT:
                return true;
            }
            return true;
        }

        boolean matches(BlockPos bp, IBlockState bs) {
            boolean match = false;

            // See whether the blockstate matches our specification:
            for (String allowedbs : this.allowedBlockNames) {
                if (allowedbs.equals(bs.getBlock().getUnlocalizedName()))
                    match = true;
            }

            // This type of block is a match, but does the colour match?
            if (match && this.spec.getColour() != null && !this.spec.getColour().isEmpty())
                match = MinecraftTypeHelper.blockColourMatches(bs, this.spec.getColour());

            // Matches type and colour, but does the variant match?
            if (match && this.spec.getVariant() != null && !this.spec.getVariant().isEmpty())
                match = MinecraftTypeHelper.blockVariantMatches(bs, this.spec.getVariant());

            if (match)
            {
                // We're firing.
                this.hasFired = true;
                this.lastFired = System.currentTimeMillis();
                if (this.spec.getBehaviour() == Behaviour.ONCE_PER_BLOCK)
                    this.firedBlocks.add(bp);
            }

            return match;
        }

        float reward() {
            return this.spec.getReward().floatValue();
        }
    }

    ArrayList<BlockMatcher> matchers = new ArrayList<BlockMatcher>();
    private RewardForTouchingBlockType params;

    @Override
    public boolean parseParameters(Object params) {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForTouchingBlockType))
            return false;

        this.params = (RewardForTouchingBlockType) params;
        for (BlockSpecWithRewardAndBehaviour spec : this.params.getBlock())
            this.matchers.add(new BlockMatcher(spec));

        return true;
    }

    @SubscribeEvent
    public void onDiscretePartialMoveEvent(DiscreteMovementCommandsImplementation.DiscretePartialMoveEvent event)
    {
        MultidimensionalReward reward = new MultidimensionalReward();
        calculateReward(reward);
        addCachedReward(reward);
    }

    private void calculateReward(MultidimensionalReward reward)
    {
        // Determine what blocks we are touching.
        // This code is largely cribbed from Entity, where it is used to fire the Block.onEntityCollidedWithBlock methods.
        EntityPlayerSP player = Minecraft.getMinecraft().player;

        List<BlockPos> touchingBlocks = PositionHelper.getTouchingBlocks(player);
        for (BlockPos pos : touchingBlocks) {
            IBlockState iblockstate = player.world.getBlockState(pos);
            for (BlockMatcher bm : this.matchers) {
                if (bm.applies(pos) && bm.matches(pos, iblockstate))
                {
                    float reward_value = bm.reward();
                    float adjusted_reward = adjustAndDistributeReward(reward_value, this.params.getDimension(), bm.spec.getDistribution());
                    reward.add( this.params.getDimension(), adjusted_reward );
                }
            }
        }
    }

    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward)
    {
        super.getReward(missionInit, reward);
        calculateReward(reward);
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        super.prepare(missionInit);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
