package net.silentchaos512.tutorial.init;

import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.item.ItemTutorial;
import net.silentchaos512.tutorial.item.tool.ItemTutorialPickaxe;
import net.silentchaos512.tutorial.lib.Names;

/**
 * This is where we handle Item registration. Typically, you should also store references to your items for quick
 * access. Note there are two initialization methods here, init (common, both client and server) and initClient
 * (client-side only).
 */
public class ModItems {

  /**
   * The ToolMaterial our tools will use. ToolMaterials are good for setting the properties of basic tools and weapons,
   * but should never be used anywhere but the constructors for said items! For example, Silent's Gems doesn't even USE
   * the material passed to its tools! Things like harvest level, mining speed, durability, etc. are calculated from
   * NBT. Use appropriate getter methods (e.g. getHarvestLevel, getStrVsBlock) if you need the values elsewhere.
   */
  public static ToolMaterial toolMaterial = EnumHelper
      .addToolMaterial(Tutorial.RESOURCE_PREFIX + "tut_mat", 4, 2048, 10.0f, 4.0f, 16);

  /*
   * Item references for easy access.
   */

  public static ItemTutorial tutorialItem;

  public static ItemTutorialPickaxe tutorialPickaxe;

  /**
   * The common initializer. Registers items, but not models. The game will crash if we try to do anything with models
   * on the server, as those classes don't even exist on that side!
   * 
   * This should be called during preInit.
   */
  public static void init() {

    // Initialize the item and store the reference. Can be done on the line the variable is declared, do whatever makes
    // the most sense to you.
    tutorialItem = new ItemTutorial();
    // Setting the registry name is how Forge tells items apart.
    tutorialItem.setRegistryName(new ResourceLocation(Tutorial.MOD_ID, Names.TUTORIAL_ITEM));
    // Finally, register the item! Must be done AFTER setting the registry name.
    GameRegistry.register(tutorialItem);

    tutorialPickaxe = register(new ItemTutorialPickaxe(), Names.PICKAXE);
  }

  /**
   * Here we should call the addRecipes method for all items that have one. Should be called during the init phase.
   */
  public static void initRecipes() {

    tutorialItem.addRecipes();
  }

  /**
   * The client-side initializer. Here we handle model registration. Note the @SideOnly annotation. This causes the
   * method to exist only on the client-side, preventing servers from crashing.
   * 
   * This should be called during init, calling during preInit will crash.
   */
  @SideOnly(Side.CLIENT)
  public static void initClient(ItemModelMesher mesher) {

    // Create a MRL for the model. Note we have two parameters and the second must be "inventory". We can reuse this
    // variable for each item of course, since most Java variables just contain a reference to an object.
    ModelResourceLocation model = new ModelResourceLocation(
        Tutorial.RESOURCE_PREFIX + Names.TUTORIAL_ITEM, "inventory");
    // Here we list all models the item can possibly have. An array will work, or just list each MRL for the final
    // parameters.
    ModelLoader.registerItemVariants(tutorialItem, model);
    // Finally, we associate each MRL (model) with different metadata values for the item. This must be called for each
    // variant! And even if the variants don't depend on metadata, I believe each variant must be registered to a unique
    // meta... In this case, there are no other variants, so we just pass in a metadata of zero.
    mesher.register(tutorialItem, 0, model);

    registerModel(mesher, tutorialPickaxe, Names.PICKAXE);
  }

  /**
   * Helper method to make item registration more compact.
   * 
   * @param item
   *          The new item to register.
   * @param name
   *          The name for the item (prepends the resource prefix automatically).
   * @return item
   * @since Episode 7
   */
  protected static <T extends Item> T register(T item, String name) {

    item.setRegistryName(new ResourceLocation(Tutorial.MOD_ID, name));
    GameRegistry.register(item);
    item.setCreativeTab(Tutorial.tabTutorial);
    item.setUnlocalizedName(Tutorial.RESOURCE_PREFIX + name);
    return item;
  }

  /**
   * Helper method to make model registration more compact.
   * 
   * @param mesher
   *          The ItemModelMesher passed into initClient.
   * @param item
   *          The item you are registering models for.
   * @param name
   *          The item name (same one used in register).
   * @since Episode 7
   */
  @SideOnly(Side.CLIENT)
  protected static void registerModel(ItemModelMesher mesher, Item item, String name) {

    ModelResourceLocation model = new ModelResourceLocation(Tutorial.RESOURCE_PREFIX + name,
        "inventory");
    ModelLoader.registerItemVariants(item, model);
    mesher.register(item, 0, model);

  }
}
