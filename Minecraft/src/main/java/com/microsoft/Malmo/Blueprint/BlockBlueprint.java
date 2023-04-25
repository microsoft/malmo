package com.microsoft.Malmo.Blueprint;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.microsoft.Malmo.MalmoMod;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class BlockBlueprint extends Block {
    public static final String NAME = "blueprint_block";
    public static final String BLUEPRINT_CONFIGS = "blueprint";
    public static Map<EnumBlockType, BlockBlueprint> BLOCKS;

    public static boolean BLUEPRINT_VISIBLE = true;

    private EnumBlockType blockType;

    public BlockBlueprint(EnumBlockType blockType) {
        super(Material.CLAY);
        this.translucent = true;
        setCreativeTab(CreativeTabs.MISC);
        this.blockType = blockType;
    }

    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        if (BLUEPRINT_VISIBLE) {
            return super.getRenderType(state);
        } else {
            return EnumBlockRenderType.INVISIBLE;
        }
    }

    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer()
    {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return false;
    }

    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
    }

    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    public boolean isFullCube(IBlockState state) {
        return false;
    }

    public static void register() {
        BLOCKS = new HashMap<EnumBlockType, BlockBlueprint>();
        for (EnumBlockType blockType: EnumBlockType.values()) {
            BlockBlueprint block = (BlockBlueprint)
                (new BlockBlueprint(blockType))
                .setBlockUnbreakable()
                .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "blueprint_block"
                    + "." + blockType.getName());
            BLOCKS.put(blockType, block);
            ResourceLocation resourceLocation = new ResourceLocation(
                MalmoMod.MODID, blockType.getName() + "_" + BlockBlueprint.NAME);
            GameRegistry.register(block, resourceLocation);
            new ItemBlock(block).setRegistryName(block.getRegistryName());
        }

        BLUEPRINT_VISIBLE = MalmoMod.instance.getModPermanentConfigFile().get(
            BLUEPRINT_CONFIGS, "visible", true).getBoolean();
    }

    public enum EnumBlockType implements IStringSerializable {
        DIRT(2, "dirt"),
        COBBLESTONE(3, "cobblestone"),
        GLASS(4, "glass"),
        LOG(5, "log"),
        PLANKS(6, "planks"),
        STONE(7, "stone"),
        STONEBRICK(8, "stonebrick"),
        WOOL(9, "wool");

        private int blockId;
        private String name;

        private EnumBlockType(int blockId, String name) {
            this.blockId = blockId;
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        public int getBlockId() {
            return this.blockId;
        }

        public String getUnlocalizedName()
        {
            return this.name;
        }

        public String toString() {
            return this.name;
        }

        public static EnumBlockType fromString(String blockTypeStr) {
            for (EnumBlockType blockType: EnumBlockType.values()) {
                if (blockType.getName().equals(blockTypeStr)) {
                    return blockType;
                }
            }
            throw new IllegalArgumentException("invalid block type: " + blockTypeStr);
        }

        public static EnumBlockType fromBlockId(int blockId) {
            for (EnumBlockType blockType: EnumBlockType.values()) {
                if (blockType.getBlockId() == blockId) {
                    return blockType;
                }
            }
            throw new IllegalArgumentException("invalid block ID: " + blockId);
        }
    }
}
