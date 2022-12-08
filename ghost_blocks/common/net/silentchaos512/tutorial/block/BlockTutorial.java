package net.silentchaos512.tutorial.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.BlockAir;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.lib.Names;
import net.minecraft.util.BlockRenderLayer;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.BlockSlime;

public class BlockTutorial extends BlockSlime {
  public BlockTutorial() {
    super();
    this.translucent = true;
    setCreativeTab(Tutorial.tabTutorial);  
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

  public void addRecipes() {
    GameRegistry.addShapedRecipe(new ItemStack(this), " l ", "lwl", " l ", 'l', Blocks.LADDER, 'w',
        new ItemStack(Blocks.WOOL, 1, OreDictionary.WILDCARD_VALUE));
    GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(this), "dld", "lwl", "dld", 'l',
        Blocks.LADDER, 'w', Blocks.WOOL, 'd', "dyeBlack"));
  }

  @Override
  public String getUnlocalizedName() {
    return "tile." + Tutorial.RESOURCE_PREFIX + Names.TUTORIAL_BLOCK; // tile.tutorial:tutorial_block
  }
}
