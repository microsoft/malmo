package net.silentchaos512.tutorial.init;

import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.block.BlockTutorial;
import net.silentchaos512.tutorial.block.BlockGhostDirt;
import net.silentchaos512.tutorial.block.BlockGhostCobblestone;
import net.silentchaos512.tutorial.block.BlockGhostGlass;
import net.silentchaos512.tutorial.block.BlockGhostLog;
import net.silentchaos512.tutorial.block.BlockGhostPlanks;
import net.silentchaos512.tutorial.block.BlockGhostStone;
import net.silentchaos512.tutorial.block.BlockGhostStonebrick;
import net.silentchaos512.tutorial.block.BlockGhostWool;
import net.silentchaos512.tutorial.lib.Names;

/**
 * This class handles Block registration. It's very similar to ModItems. Registering a Block is similar to an Item, but
 * takes a couple extra steps. Again, init is client and server and initClient is client-side only.
 * 
 * Episode 4
 *
 */
public class ModBlocks {

  /*
   * Block references for easy access (for adding recipes and such).
   */
  public static BlockTutorial tutorialBlock;
  public static BlockGhostDirt ghostdirtBlock;
  public static BlockGhostCobblestone ghostcobblestoneBlock;
  public static BlockGhostGlass ghostglassBlock;
  public static BlockGhostLog ghostlogBlock;
  public static BlockGhostPlanks ghostplanksBlock;
  public static BlockGhostStone ghoststoneBlock;
  public static BlockGhostStonebrick ghoststonebrickBlock;
  public static BlockGhostWool ghostwoolBlock;

  /**
   * The common initializer. Registers blocks, but not models. Should be called during preInit.
   */
  public static void init() {
    // Store resource location since we need it twice.
    ResourceLocation location = new ResourceLocation(Tutorial.MOD_ID, Names.TUTORIAL_BLOCK);
    ResourceLocation ghostdirt_location = new ResourceLocation(Tutorial.MOD_ID, Names.GHOST_DIRT);
    ResourceLocation ghostcobblestone_location = new ResourceLocation(Tutorial.MOD_ID, Names.GHOST_COBBLESTONE);
    ResourceLocation ghostglass_location = new ResourceLocation(Tutorial.MOD_ID, Names.GHOST_GLASS);
    ResourceLocation ghostlog_location = new ResourceLocation(Tutorial.MOD_ID, Names.GHOST_LOG);
    ResourceLocation ghostplanks_location = new ResourceLocation(Tutorial.MOD_ID, Names.GHOST_PLANKS);
    ResourceLocation ghoststone_location = new ResourceLocation(Tutorial.MOD_ID, Names.GHOST_STONE);
    ResourceLocation ghoststonebrick_location = new ResourceLocation(Tutorial.MOD_ID, Names.GHOST_STONEBRICK);
    ResourceLocation ghostwool_location = new ResourceLocation(Tutorial.MOD_ID, Names.GHOST_WOOL);
    
    // Initialize the block and store the reference.
    tutorialBlock = new BlockTutorial();
    ghostdirtBlock = new BlockGhostDirt();
    ghostcobblestoneBlock = new BlockGhostCobblestone();
    ghostglassBlock = new BlockGhostGlass();
    ghostlogBlock = new BlockGhostLog();
    ghostplanksBlock = new BlockGhostPlanks();
    ghoststoneBlock = new BlockGhostStone();
    ghoststonebrickBlock = new BlockGhostStonebrick();
    ghostwoolBlock = new BlockGhostWool();
    // Setting the registry name is how Forge tells blocks apart.
    tutorialBlock.setRegistryName(location);
    ghostdirtBlock.setRegistryName(ghostdirt_location);
    ghostcobblestoneBlock.setRegistryName(ghostcobblestone_location);
    ghostglassBlock.setRegistryName(ghostglass_location);
    ghostlogBlock.setRegistryName(ghostlog_location);
    ghostplanksBlock.setRegistryName(ghostplanks_location);
    ghoststoneBlock.setRegistryName(ghoststone_location);
    ghoststonebrickBlock.setRegistryName(ghoststonebrick_location);
    ghostwoolBlock.setRegistryName(ghostwool_location);
    // Register the block. But we also need to...
    GameRegistry.register(tutorialBlock);
    GameRegistry.register(ghostdirtBlock);
    GameRegistry.register(ghostcobblestoneBlock);
    GameRegistry.register(ghostglassBlock);
    GameRegistry.register(ghostlogBlock);
    GameRegistry.register(ghostplanksBlock);
    GameRegistry.register(ghoststoneBlock);
    GameRegistry.register(ghoststonebrickBlock);
    GameRegistry.register(ghostwoolBlock);
    // ...register an ItemBlock for the block as well!. An ItemBlock is an Item that represents a Block. When you mine a
    // block and pick it up, you are actually getting an ItemBlock in your inventory. We are using the basic vanilla
    // ItemBlock, but you could create your own if necessary.
    GameRegistry.register(new ItemBlock(tutorialBlock), location);
    GameRegistry.register(new ItemBlock(ghostdirtBlock), ghostdirt_location);
    GameRegistry.register(new ItemBlock(ghostcobblestoneBlock), ghostcobblestone_location);
    GameRegistry.register(new ItemBlock(ghostglassBlock), ghostglass_location);
    GameRegistry.register(new ItemBlock(ghostlogBlock), ghostlog_location);
    GameRegistry.register(new ItemBlock(ghostplanksBlock), ghostplanks_location);
    GameRegistry.register(new ItemBlock(ghoststoneBlock), ghoststone_location);
    GameRegistry.register(new ItemBlock(ghoststonebrickBlock), ghoststonebrick_location);
    GameRegistry.register(new ItemBlock(ghostwoolBlock), ghostwool_location);
  }

  /**
   * Here we should call the addRecipes method for all blocks that have one. Should be called during the init phase.
   */
  public static void initRecipes() {
    ghostdirtBlock.addRecipes();
    tutorialBlock.addRecipes();
    ghostcobblestoneBlock.addRecipes();
  }

  /**
   * The client-side initializer. Here we handle model registration. Don't forget the @SideOnly annotation! This should
   * be called during init.
   */
  @SideOnly(Side.CLIENT)
  public static void initClient(ItemModelMesher mesher) {

    // We can only register models for Items, so get the item for the block. Maybe you could save a reference to the
    // ItemBlock in init? I haven't tried it, but we likely wouldn't use it anywhere else, so it's not worth doing.
    Item item = Item.getItemFromBlock(tutorialBlock);
    // Item ghostdirtItem = Item.getItemFromBlock(ghostdirtBlock);
    // Item ghostcobblestoneItem = Item.getItemFromBlock(ghostcobblestoneBlock);
    
    // Everything past this point is identical to registering models for items.
    ModelResourceLocation model = new ModelResourceLocation(Tutorial.RESOURCE_PREFIX + Names.TUTORIAL_BLOCK, "inventory");
    // ModelResourceLocation ghostdirtModel = new ModelResourceLocation(Tutorial.RESOURCE_PREFIX + Names.GHOST_DIRT, "inventory");
    // ModelResourceLocation ghostcobblestoneModel = new ModelResourceLocation(Tutorial.RESOURCE_PREFIX + Names.GHOST_COBBLESTONE, "inventory");
    
    ModelLoader.registerItemVariants(item, model);
    // ModelLoader.registerItemVariants(ghostdirtItem, ghosdirtModel);
    // ModelLoader.registerItemVariants(ghostcobblestoneItem, ghostcobblestoneItem);
    mesher.register(item, 0, model);
    // mesher.register(ghostdirtItem, 0, ghostdirtModel);
    // mesher.register(ghostcobblestoneItem, 0, ghostcobblestoneModel);
  }
}
