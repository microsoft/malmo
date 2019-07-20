// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

package com.microsoft.Malmo.MissionHandlers;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldGenerator;
import com.microsoft.Malmo.Schemas.BiomeGenerator;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.MapFileHelper;
import com.microsoft.Malmo.Utils.SeedHelper;

/**
 * Generates a survival world of only the biome specified.
 * 
 * @author Cayden Codel, Carnegie Mellon University
 *
 */
public class BiomeGeneratorImplementation extends HandlerBase implements IWorldGenerator {
	BiomeGenerator bparams;

	// Register the event with the Forge Bus
	public BiomeGeneratorImplementation() {
	}

	/**
	 * Keeps a constant biome given an id number
	 * 
	 * @author Cayden Codel, Carnegie Mellon University Some contributions from
	 *         teamrtg with the LonelyBiome mod
	 *
	 */
	private class GenLayerConstant extends GenLayer {
		private final int value;

		public GenLayerConstant(int value) {
			super(0L);
			this.value = value;
		}

		@Override
		public int[] getInts(int par1, int par2, int par3, int par4) {
			int[] aint2 = IntCache.getIntCache(par3 * par4);

			for (int i = 0; i < aint2.length; i++) {
				aint2[i] = value;
			}

			return aint2;
		}
	}

	// Make sure that the biome is one type with an event on biome gen
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onBiomeGenInit(WorldTypeEvent.InitBiomeGens event) {
		// Negative one is flag value for normal world gen
		if (bparams.getBiome() == -1)
			return;
		GenLayer[] replacement = new GenLayer[2];
		replacement[0] = new GenLayerConstant(bparams.getBiome());
		replacement[1] = replacement[0];
		event.setNewBiomeGens(replacement);
	}

	@Override
	public boolean parseParameters(Object params) {
		if (params == null || !(params instanceof BiomeGenerator))
			return false;
		this.bparams = (BiomeGenerator) params;
		MinecraftForge.TERRAIN_GEN_BUS.register(this);
		return true;
	}

	public static long getWorldSeedFromString() {
		// This seed logic mirrors the Minecraft code in
		// GuiCreateWorld.actionPerformed:
		long seed = (SeedHelper.getRandom()).nextLong();
		return seed;
	}

	@Override
	public boolean createWorld(MissionInit missionInit) {
		long seed = getWorldSeedFromString();
		WorldType.WORLD_TYPES[0].onGUICreateWorldPress();
		WorldSettings worldsettings = new WorldSettings(seed, GameType.SURVIVAL, true, false, WorldType.WORLD_TYPES[0]);
		worldsettings.enableCommands();
		// Create a filename for this map - we use the time stamp to make sure
		// it is different from other worlds, otherwise no new world
		// will be created, it will simply load the old one.
		return MapFileHelper.createAndLaunchWorld(worldsettings, this.bparams.isDestroyAfterUse());
	}

	@Override
	public boolean shouldCreateWorld(MissionInit missionInit, World world) {
		if (this.bparams != null && this.bparams.isForceReset())
			return true;

		if (Minecraft.getMinecraft().world == null || world == null)
			return true; // Definitely need to create a world if there isn't one
		                 // in existence!

		String genOptions = world.getWorldInfo().getGeneratorOptions();
		if (genOptions != null && !genOptions.isEmpty())
			return true; // Biome generator has no generator options.

		return false;
	}

	@Override
	public String getErrorDetails() {
		return ""; // Don't currently have any error exit points.
	}

}
