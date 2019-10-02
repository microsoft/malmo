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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.NavigationDecorator;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;
import com.microsoft.Malmo.Utils.PositionHelper;
import com.microsoft.Malmo.Utils.SeedHelper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Creates a decorator that sets a random block and then points all compasses
 * towards the block.
 *
 * @author Cayden Codel, Carnegie Mellon University
 *
 */
public class NavigationDecoratorImplementation extends HandlerBase implements IWorldDecorator {

	private NavigationDecorator nparams;

	private double originX, originY, originZ;
	private double placementX, placementY, placementZ;
	private double radius;
	private double minDist, maxDist;
	private double minRad, maxRad;

	@Override
	public boolean parseParameters(Object params) {
		if (params == null || !(params instanceof NavigationDecorator))
			return false;
		this.nparams = (NavigationDecorator) params;
		return true;
	}


	@Override
	public void buildOnWorld(MissionInit missionInit, World world) throws DecoratorException {
		if (nparams.getRandomPlacementProperties().getOrigin() != null)
			originX = nparams.getRandomPlacementProperties().getOrigin().getX().doubleValue();
		else
			originX = world.getSpawnPoint().getX();
		if (nparams.getRandomPlacementProperties().getOrigin() != null)
			originY = nparams.getRandomPlacementProperties().getOrigin().getY().doubleValue();
		else
			originY = world.getSpawnPoint().getY();
		if (nparams.getRandomPlacementProperties().getOrigin() != null)
			originZ = nparams.getRandomPlacementProperties().getOrigin().getZ().doubleValue();
		else
			originZ = world.getSpawnPoint().getZ();

		maxRad = nparams.getRandomPlacementProperties().getMaxRandomizedRadius().doubleValue();
		minRad = nparams.getRandomPlacementProperties().getMinRandomizedRadius().doubleValue();
		radius = (int) (SeedHelper.getRandom().nextDouble() * (maxRad - minRad) + minRad);

		minDist = nparams.getMinRandomizedDistance().doubleValue();
		maxDist = nparams.getMaxRandomizedDistance().doubleValue();
		placementX = 0;
		placementY = 0;
		placementZ = 0;
		if (nparams.getRandomPlacementProperties().getPlacement().equals("surface")) {
			placementX = ((SeedHelper.getRandom().nextDouble() - 0.5) * 2 * radius);
			placementZ = (SeedHelper.getRandom().nextDouble() > 0.5 ? -1 : 1) * Math.sqrt((radius * radius) - (placementX * placementX));
			// Change center to origin now
			placementX += originX;
			placementZ += originZ;
			placementY = PositionHelper.getTopSolidOrLiquidBlock(world, new BlockPos(placementX, 0, placementZ)).getY() - 1;
		} else if (nparams.getRandomPlacementProperties().getPlacement().equals("fixed_surface")) {
			placementX = ((0.42 - 0.5) * 2 * radius);
			placementZ = (0.24 > 0.5 ? -1 : 1) * Math.sqrt((radius * radius) - (placementX * placementX));
			// Change center to origin now
			placementX += originX;
			placementZ += originZ;
			placementY = PositionHelper.getTopSolidOrLiquidBlock(world, new BlockPos(placementX, 0, placementZ)).getY() - 1;
		} else if (nparams.getRandomPlacementProperties().getPlacement().equals("circle")) {
			placementX = ((SeedHelper.getRandom().nextDouble() - 0.5) * 2 * radius);
			placementY = originY;
			placementZ = (SeedHelper.getRandom().nextDouble() > 0.5 ? -1 : 1) * Math.sqrt((radius * radius) - (placementX * placementX));
			// Change center to origin now
			placementX += originX;
			placementZ += originZ;
		} else {
			placementX = ((SeedHelper.getRandom().nextDouble() - 0.5) * 2 * radius);
			placementY = (SeedHelper.getRandom().nextDouble() - 0.5) * 2 * Math.sqrt((radius * radius) - (placementX * placementX));
			placementZ = (SeedHelper.getRandom().nextDouble() > 0.5 ? -1 : 1)
					* Math.sqrt((radius * radius) - (placementX * placementX) - (placementY * placementY));
			// Change center to origin now
			placementX += originX;
			placementY += originY;
			placementZ += originZ;
		}
		IBlockState state = MinecraftTypeHelper
				.ParseBlockType(nparams.getRandomPlacementProperties().getBlock().value());
		world.setBlockState(new BlockPos(placementX, placementY, placementZ), state);
		// Set compass location to the block
		double xDel = 0, zDel = 0;
		if (nparams.isRandomizeCompassLocation()) {
			double dist = 0;
			do {
				xDel = (SeedHelper.getRandom().nextDouble() - 0.5) * 2 * maxDist;
				zDel = (SeedHelper.getRandom().nextDouble() - 0.5) * 2 * maxDist;
				dist = Math.sqrt(xDel * xDel + zDel * zDel);
			} while (dist <= maxDist && dist >= minDist);
		}
		placementX += xDel;
		placementZ += zDel;
	}

	@Override
	public boolean getExtraAgentHandlersAndData(List<Object> handlers, Map<String, String> data) {
		return false;
	}

	@Override
	public void update(World world) {
		if (Minecraft.getMinecraft().player != null) {
			BlockPos spawn = Minecraft.getMinecraft().player.world.getSpawnPoint();
			if (spawn.getX() != (int) placementX && spawn.getY() != (int) placementY
					&& spawn.getZ() != (int) placementZ)
				Minecraft.getMinecraft().player.world.setSpawnPoint(new BlockPos(placementX, placementY, placementZ));
		}
	}

	@Override
	public void prepare(MissionInit missionInit) {
	}

	@Override
	public void cleanup() {
	}

	@Override
	public boolean targetedUpdate(String nextAgentName) {
		return false;
	}

	@Override
	public void getTurnParticipants(ArrayList<String> participants, ArrayList<Integer> participantSlots) {
	}
}