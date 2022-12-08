package net.silentchaos512.tutorial.block;

import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.lib.Names;


public class BlockGhostLog extends BlockTutorial {
    
    public BlockGhostLog() {
        super();
    }

    @Override
    public String getUnlocalizedName() {
        return "tile." + Tutorial.RESOURCE_PREFIX + Names.GHOST_LOG; // tile.tutorial:tutorial_block
    }

}
