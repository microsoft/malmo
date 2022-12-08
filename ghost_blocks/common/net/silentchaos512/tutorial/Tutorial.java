package net.silentchaos512.tutorial;

import java.util.Random;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.silentchaos512.tutorial.init.ModItems;

/**
 * Our mod class. This serves as the starting point for our mod and all its code.
 *
 */
@Mod(modid = Tutorial.MOD_ID, name = Tutorial.MOD_NAME, version = Tutorial.VERSION, dependencies = Tutorial.DEPENDENCIES)
public class Tutorial {
  // Constants

  /**
   * Our mod ID. This is how Forge and other mods will identify this mod. Forge lowercases this automatically, but I
   * recommend using all lowercase letters and underscores if you are not already using something else! Mod ID's not
   * lining up can cause MANY problems, so be careful!
   */
  public static final String MOD_ID = "tutorial";
  /**
   * The mod name. This is meant to be human-readable. For example, this is what WIT and Waila will display.
   */
  public static final String MOD_NAME = "Silent's Awesome Tutorial Mod";
  /**
   * The version number. We will replace this string with the current build number in our build.gradle file.
   */
  public static final String VERSION = "@VERSION@";
  /**
   * Dependencies. You can list mods that this one requires, should load after, etc.
   */
  public static final String DEPENDENCIES = "required-after:forge@[13.19.1.2188,)";
  /**
   * Resource prefix is used for ModelResourceLocations and some other things. It's just the mod ID followed by a colon.
   */
  public static final String RESOURCE_PREFIX = MOD_ID.toLowerCase() + ":"; // tutorial:

  // Variables
  public static Random random = new Random();

  /**
   * The instance of the mod. I never use this for anything myself, but I believe you need to have this.
   */
  @Instance(MOD_ID)
  public static Tutorial instance;

  /**
   * A sided proxy. Which class is used depends on whether we are on the client- or server-side.
   * Typically, you have a CommonProxy for the server and a ClientProxy that inherits from CommonProxy for the client.
   * We will use are proxies for most (if not all) are initializations because the client-server separation is convenient.
   */
  @SidedProxy(clientSide = "net.silentchaos512.tutorial.ClientProxy", serverSide = "net.silentchaos512.tutorial.CommonProxy")
  public static CommonProxy proxy;

  /*
   * Forge has three initialization phases, typically called pre-init, init, and post-init. All mods will go through
   * pre-init, then all mods go through init, then finally post-init. These stages help with mod interactions.
   * We will be handing over initialization phase control to our proxy.
   * 
   * Note the EventHandler annotation on the following three methods. Also the arguments, which determine which phase
   * each method is called during.
   */

  @EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    proxy.preInit(event);
  }

  @EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }

  @EventHandler
  public void postInit(FMLPostInitializationEvent event) {
    proxy.postInit(event);
  }

  public static CreativeTabs tabTutorial = new CreativeTabs(Tutorial.RESOURCE_PREFIX + "creative_tab") {

    @Override
    public ItemStack getTabIconItem() {
      return new ItemStack(ModItems.tutorialItem);
    }
  };
}
