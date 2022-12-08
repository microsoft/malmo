package net.silentchaos512.tutorial.block;

import net.silentchaos512.tutorial.Tutorial;
import net.silentchaos512.tutorial.lib.Names;


public class BlockGhostPlanks extends BlockTutorial {
    
    public BlockGhostPlanks() {
        super();
    }

    @Override
    public String getUnlocalizedName() {
        return "tile." + Tutorial.RESOURCE_PREFIX + Names.GHOST_PLANKS; // tile.tutorial:tutorial_block
    }

}
