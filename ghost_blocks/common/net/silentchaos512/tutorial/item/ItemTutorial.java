package net.silentchaos512.tutorial.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.lib.Names;

/**
 * A very basic item with no subtypes/variants (episode 3)
 */
public class ItemTutorial extends Item {

  public ItemTutorial() {

    // You can optionally change the max stack size with this method.
    setMaxStackSize(64);
    // If we don't set a creative tab, the block/item won't show up anywhere in the creative menus, but will still
    // appear in JEI.
    setCreativeTab(Tutorial.tabTutorial);
  }

  /**
   * Add recipes related to the item. Episode 5.
   */
  public void addRecipes() {

    /*
     * Shapeless recipes have no specific layout, so we just list each ingredient. If you have more than one of some
     * ingredient, you must list it that many times. In this case, it's 2 bones and 1 birch wood log.
     */
    GameRegistry.addShapelessRecipe(new ItemStack(this), Items.BONE, Items.BONE,
        new ItemStack(Blocks.LOG, 1, 2));
  }

  /**
   * Called when the item is right-clicked. By overriding this method, we can changed the behavior of the item! Note
   * the @Override annotation. You should use these EVERY TIME you override a method. If the method signatures change
   * between Minecraft versions (and that happens fairly often), you will get a compiler error. This makes porting much,
   * much easier! Otherwise, you'll have a hard time figuring out why certain methods suddenly aren't being called.
   */
  @Override
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {

    if (!world.isRemote)
      player.sendMessage(new TextComponentString("You used my item!"));

    return super.onItemRightClick(world, player, hand);
  }

  /**
   * Returns the unlocalized the name for the item. Make sure to add a corresponding line to your localization file! You
   * could probably just call {@link Item#setUnlocalizedName(String)} as well, but I usually just override this.
   */
  @Override
  public String getUnlocalizedName(ItemStack stack) {

    return "item." + Tutorial.RESOURCE_PREFIX + Names.TUTORIAL_ITEM; // item.tutorial:tutorial_item
  }
}
