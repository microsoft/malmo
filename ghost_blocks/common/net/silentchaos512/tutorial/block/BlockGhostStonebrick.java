package net.silentchaos512.tutorial.block;

import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.lib.Names;


public class BlockGhostStonebrick extends BlockTutorial {
    
    public BlockGhostStonebrick() {
        super();
    }

    @Override
    public String getUnlocalizedName() {
        return "tile." + Tutorial.RESOURCE_PREFIX + Names.GHOST_STONEBRICK; // tile.tutorial:tutorial_block
    }

}
