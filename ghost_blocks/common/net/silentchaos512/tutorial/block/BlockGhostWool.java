package net.silentchaos512.tutorial.block;

import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.lib.Names;


public class BlockGhostWool extends BlockTutorial {
    
    public BlockGhostWool() {
        super();
    }

    @Override
    public String getUnlocalizedName() {
        return "tile." + Tutorial.RESOURCE_PREFIX + Names.GHOST_WOOL; // tile.tutorial:tutorial_block
    }

}
