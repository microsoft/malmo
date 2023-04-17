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
import net.minecraft.block.BlockStone;
import net.minecraft.world.IBlockAccess;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockPlanks;
import net.minecraft.item.ItemBlock;


public class BlockBlueprint extends BlockSlime {
    public static final String NAME = "blueprint_block";
    public static final IProperty<EnumBlockType> VARIANT =
        PropertyEnum.create("variant", EnumBlockType.class);
    public static BlockBlueprint BLOCK;

    public BlockBlueprint() {
        super();
        this.translucent = true;
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(VARIANT, EnumBlockType.fromBlockId(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(VARIANT).getBlockId();
	}

    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        // return false;
        return true;
    }

    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
    }

    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    public boolean isFullCube(IBlockState state) {
        return false;
    }
    
    // @Override
    // public String getLocalizedName()
    // {
    //     return I18n.translateToLocal(this.getUnlocalizedName() + "." + BlockBlueprint.EnumBlockType.DIRT.getUnlocalizedName() + ".name");
    // }

    public static void register() {
        ResourceLocation resourceLocation = 
            new ResourceLocation(MalmoMod.MODID, BlockBlueprint.NAME);
        BLOCK = new BlockBlueprint();
        GameRegistry.register(BLOCK, resourceLocation);

        // register the item block
        new ItemBlock(BLOCK).setRegistryName(BLOCK.getRegistryName());

    }

    public enum EnumBlockType implements IStringSerializable {
        DIRT(2, "dirt", true),
        COBBLESTONE(3, "cobblestone", true),
        GLASS(4, "glass", true),
        LOG(5, "log", true),
        PLANKS(6, "planks", true),
        STONE(7, "stone", true),
        STONEBRICK(8, "stonebrick", true),
        WOOL(9, "wool", true);

        private int blockId;
        private String name;
        private String unlocalizedName;
        private boolean isNatural;

        private EnumBlockType(int blockId, String p_i46383_5_, boolean p_i46383_6_) {
            this(blockId, p_i46383_5_, p_i46383_5_, p_i46383_6_);
        }

        private EnumBlockType(int blockId, String name, String unlocalizedName, boolean isNatural) {
            this.blockId = blockId;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
            this.isNatural = isNatural;
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
            return this.unlocalizedName;
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
