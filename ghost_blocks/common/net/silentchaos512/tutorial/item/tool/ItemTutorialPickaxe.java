package net.silentchaos512.tutorial.item.tool;

import net.minecraft.item.ItemPickaxe;
import net.silentchaos512.tutorial.init.ModItems;

/**
 * Simple pickaxe (episode 7). The only reason this class needs to exist in this case is because the ItemPickaxe
 * constructor is protected. You can of course override any methods to change the tool to fit your needs. Examples
 * include custom right-click behavior, calculating a harvest level based on NBT (which is why you should NEVER
 * reference toolMaterial for that!), etc.
 */
public class ItemTutorialPickaxe extends ItemPickaxe {

  public ItemTutorialPickaxe() {

    super(ModItems.toolMaterial);
  }
}
