package net.silentchaos512.tutorial.block;

import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.lib.Names;


public class BlockGhostDirt extends BlockTutorial {
    
    public BlockGhostDirt() {
        super();
    }

    @Override
    public String getUnlocalizedName() {
        return "tile." + Tutorial.RESOURCE_PREFIX + Names.GHOST_DIRT; // tile.tutorial:tutorial_block
    }

}
