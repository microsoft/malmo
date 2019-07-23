package com.microsoft.Malmo.Mixins;


import com.microsoft.Malmo.Utils.SeedHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;

@Mixin(WorldProvider.class)
public abstract class MixinWorldProviderSpawn{
    @Shadow protected World world;
    @Shadow public abstract boolean hasNoSky();
    @Shadow private net.minecraft.world.WorldType terrainType;
    

    public BlockPos getRandomizedSpawnPoint()
    {
        BlockPos ret = this.world.getSpawnPoint();

        boolean isAdventure = world.getWorldInfo().getGameType() == GameType.ADVENTURE;
        int spawnFuzz = this.world instanceof WorldServer ? terrainType.getSpawnFuzz((WorldServer)this.world, this.world.getMinecraftServer()) : 1;
        int border = MathHelper.floor(world.getWorldBorder().getClosestDistance(ret.getX(), ret.getZ()));
        if (border < spawnFuzz) spawnFuzz = border;

        if (!hasNoSky() && !isAdventure && spawnFuzz != 0)
        {
            if (spawnFuzz < 2) spawnFuzz = 2;
            int spawnFuzzHalf = spawnFuzz / 2;
            ret = world.getTopSolidOrLiquidBlock(ret.add(SeedHelper.getRandom("playerSpawn").nextInt(spawnFuzzHalf) - spawnFuzz, 0, SeedHelper.getRandom("playerSpawn").nextInt(spawnFuzzHalf) - spawnFuzz));
        }

        return ret;
    }

}