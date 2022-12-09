package com.microsoft.Malmo.Blueprint;

import javax.annotation.Nullable;

import com.jcraft.jorbis.Block;
import com.microsoft.Malmo.MalmoMod;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.BlockSlime;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

public class BlockBlueprint extends BlockSlime {
    public static final String NAME = "blueprint_block";
    public static final IProperty<EnumBlockType> BLOCK_TYPE =
        PropertyEnum.create("block_type", EnumBlockType.class);
    public static BlockBlueprint BLOCK;

    public BlockBlueprint() {
        super();
        this.translucent = true;
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BLOCK_TYPE);
    }

	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(BLOCK_TYPE, EnumBlockType.fromBlockId(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(BLOCK_TYPE).getBlockId();
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

    @Override
    public String getUnlocalizedName() {
        return "tile." + MalmoMod.RESOURCE_PREFIX + BlockBlueprint.NAME;
    }

    public static void register() {
        ResourceLocation resourceLocation = 
            new ResourceLocation(MalmoMod.MODID, BlockBlueprint.NAME);
        BLOCK = new BlockBlueprint();
        GameRegistry.register(BLOCK, resourceLocation);
    }

    public enum EnumBlockType implements IStringSerializable {
        DIRT(2, "dirt"),
        COBBLESTONE(3, "cobblestone"),
        GLAS(4, "glass"),
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
