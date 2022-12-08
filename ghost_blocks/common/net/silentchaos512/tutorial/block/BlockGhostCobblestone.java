package net.silentchaos512.tutorial.block;

import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.lib.Names;


public class BlockGhostCobblestone extends BlockTutorial {
    
    public BlockGhostCobblestone() {
        super();
    }

    @Override
    public String getUnlocalizedName() {
        return "tile." + Tutorial.RESOURCE_PREFIX + Names.GHOST_COBBLESTONE; // tile.tutorial:tutorial_block
    }

}
