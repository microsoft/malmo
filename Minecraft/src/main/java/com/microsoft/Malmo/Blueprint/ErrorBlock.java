package com.microsoft.Malmo.Blueprint;
import java.util.Map;
import java.util.HashMap;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import java.util.Random;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;

import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.util.IStringSerializable;

import com.microsoft.Malmo.MalmoMod;

public class ErrorBlock extends Block {
    public static final String NAME = "error_block";
    public static final String BLUEPRINT_CONFIGS = "error";
    public static Map<EnumBlockType, ErrorBlock> BLOCKS;

    public static boolean BLUEPRINT_VISIBLE = true;

    private EnumBlockType blockType;

    public static Map<EnumBlockType, Block> dropMap = new HashMap<EnumBlockType, Block>();
    static {
        dropMap.put(EnumBlockType.DIRT, Blocks.DIRT);
        dropMap.put(EnumBlockType.GRASS, Blocks.DIRT);
        dropMap.put(EnumBlockType.COBBLESTONE, Blocks.COBBLESTONE);
        dropMap.put(EnumBlockType.GLASS, Blocks.GLASS);
        dropMap.put(EnumBlockType.LOG, Blocks.LOG);
        dropMap.put(EnumBlockType.PLANKS, Blocks.PLANKS);
        dropMap.put(EnumBlockType.STONE, Blocks.STONE);
        dropMap.put(EnumBlockType.STONEBRICK, Blocks.STONEBRICK);
        dropMap.put(EnumBlockType.WOOL, Blocks.WOOL);
    }

    public ErrorBlock(EnumBlockType blockType, Material materialIn) {
        super(materialIn);
        if (blockType == EnumBlockType.GLASS) {
            this.translucent = true;
            this.lightOpacity = 255;
        }

        setCreativeTab(CreativeTabs.MISC);
        this.blockType = blockType;
    }

    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return this.blockType.getBlockLayer();
    }

    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return true;
    }

    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        return false;
    }

    public boolean isFullCube(IBlockState state) {
        return this.blockType.isFullCube(state);
    }

    public boolean isOpaqueCube(IBlockState state) {
        if (this.blockType == EnumBlockType.GLASS) {
            return false;
        }

        return true;
    }

    public boolean isToolEffective(String type, IBlockState state) {
        return type != null & type.equals(this.blockType.getHarvestTool());
    }

    public boolean canSilkHarvest() {
        return true;
    }

    public EnumBlockType getBlockType() {
        return this.blockType;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        System.out.println("Dropping item!");
        return Item.getItemFromBlock(dropMap.get(this.getBlockType()));
    }

    protected ItemStack getSilkTouchDrop(IBlockState state) {
        Item item = Item.getItemFromBlock(dropMap.get(this.getBlockType()));
        return new ItemStack(item, 1, 0);
    }

    public static void register() {
        BLOCKS = new HashMap<EnumBlockType, ErrorBlock>();
        // register dirt
        ErrorBlock[] blocks = new ErrorBlock[9];
        blocks[0] = (ErrorBlock) (new ErrorBlock(EnumBlockType.DIRT, Material.GROUND))
            .setHardness(0.5F)
            .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "error_block"
                + "." + EnumBlockType.DIRT.getName());
        BLOCKS.put(EnumBlockType.DIRT, blocks[0]);
        
        // register cobblestone
        blocks[1] = (ErrorBlock) (new ErrorBlock(EnumBlockType.COBBLESTONE, Material.ROCK))
            .setHardness(2.0F)
            .setResistance(10.0F)
            .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "error_block"
                + "." + EnumBlockType.COBBLESTONE.getName());
        
        blocks[1].setHarvestLevel("pickaxe", 1);
        BLOCKS.put(EnumBlockType.COBBLESTONE, blocks[1]);
        
        // register glass
        blocks[2] = (ErrorBlock) (new ErrorBlock(EnumBlockType.GLASS, Material.GLASS))
            .setHardness(0.3F)
            .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "error_block"
                + "." + EnumBlockType.GLASS.getName())
            .setLightOpacity(1);
        BLOCKS.put(EnumBlockType.GLASS, blocks[2]);
        
        // register log
        blocks[3] = (ErrorBlock) (new ErrorBlock(EnumBlockType.LOG, Material.WOOD))
            .setHardness(2.0F)
            .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "error_block"
                + "." + EnumBlockType.LOG.getName());
        BLOCKS.put(EnumBlockType.LOG, blocks[3]);

        // register planks
        blocks[4] = (ErrorBlock) (new ErrorBlock(EnumBlockType.PLANKS, Material.WOOD))
            .setHardness(2.0F)
            .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "error_block"
                + "." + EnumBlockType.PLANKS.getName());
        BLOCKS.put(EnumBlockType.PLANKS, blocks[4]);
        
        // register stone
        blocks[5] = (ErrorBlock) (new ErrorBlock(EnumBlockType.STONE, Material.ROCK))
            .setHardness(1.5F)
            .setResistance(10.0F)
            .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "error_block"
                + "." + EnumBlockType.STONE.getName());
        BLOCKS.put(EnumBlockType.STONE, blocks[5]);
        
        // register stonebrick
        blocks[6] = (ErrorBlock) (new ErrorBlock(EnumBlockType.STONEBRICK, Material.ROCK))
            .setHardness(1.5F)
            .setResistance(10.0F)
            .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "error_block"
                + "." + EnumBlockType.STONEBRICK.getName());
        BLOCKS.put(EnumBlockType.STONEBRICK, blocks[6]);
        
        // register wool
        blocks[7] = (ErrorBlock) (new ErrorBlock(EnumBlockType.WOOL, Material.CLOTH))
            .setHardness(0.8F)
            .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "error_block"
                + "." + EnumBlockType.WOOL.getName());
        BLOCKS.put(EnumBlockType.WOOL, blocks[7]);

        // register grass
        blocks[8] = (ErrorBlock) (new ErrorBlock(EnumBlockType.GRASS, Material.GRASS))
            .setHardness(0.6F)
            .setUnlocalizedName(MalmoMod.RESOURCE_PREFIX + "error_block"
                + "." + EnumBlockType.GRASS.getName());
        BLOCKS.put(EnumBlockType.GRASS, blocks[8]);

        for (ErrorBlock block : blocks) {
            System.out.println("Registering block: " + block.getBlockType().getName());
            ResourceLocation resourceLocation = new ResourceLocation(
                MalmoMod.MODID, block.getBlockType().getName() + "_" + ErrorBlock.NAME);
            GameRegistry.register(block, resourceLocation);
            
            new ItemBlock(block).setRegistryName(block.getRegistryName());
        }

        BLUEPRINT_VISIBLE = true;
    }

    public enum EnumBlockType implements IStringSerializable {
        DIRT(2, "dirt", Material.GROUND, "shovel"),
        COBBLESTONE(3, "cobblestone", Material.ROCK, "pickaxe"),
        GLASS(4, "glass", Material.GLASS, null),
        LOG(5, "log", Material.WOOD, "axe"),
        PLANKS(6, "planks", Material.WOOD, "axe"),
        STONE(7, "stone", Material.ROCK, "pickaxe"),
        STONEBRICK(8, "stonebrick", Material.ROCK, "pickaxe"),
        WOOL(9, "wool", Material.CLOTH, "shears"),
        GRASS(10, "grass", Material.GRASS, "shovel");

        private int blockId;
        private String name;
        private Material materialIn;
        private String harvestTool;

        private EnumBlockType(int blockId, String name, Material materialIn, String harvestTool) {
            this.blockId = blockId;
            this.name = name;
            this.materialIn = materialIn;
            this.harvestTool = harvestTool;
        }

        @SideOnly(Side.CLIENT)
        public BlockRenderLayer getBlockLayer() {
            if (this == EnumBlockType.GLASS) {
                return BlockRenderLayer.TRANSLUCENT;
            }

            return BlockRenderLayer.SOLID;
        }

        public boolean isFullCube(IBlockState state) {
            if (this == EnumBlockType.GLASS) {
                return false;
            }

            return true;
        }

        @Override
        public String getName() {
            return this.name;
        }

        public int getBlockId() {
            return this.blockId;
        }

        public Material getMaterial() {
            return this.materialIn;
        }

        public String getUnlocalizedName()
        {
            return this.name;
        }

        public String toString() {
            return this.name;
        }

        public String getHarvestTool() {
            return this.harvestTool;
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