package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.NavigationDecorator;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
	private double radius;
	private double minDist, maxDist;

	@Override
	public boolean parseParameters(Object params) {
		if (params == null || !(params instanceof NavigationDecorator))
			return false;
		this.nparams = (NavigationDecorator) params;
		return true;
	}

	@Override
	public void buildOnWorld(MissionInit missionInit, World world) throws DecoratorException {
		if (nparams.getRandomPlacementProperties().getOrigin().getX() != null)
			originX = nparams.getRandomPlacementProperties().getOrigin().getX().doubleValue();
		else
			originX = world.getSpawnPoint().getX();

		if (nparams.getRandomPlacementProperties().getOrigin().getY() != null)
			originY = nparams.getRandomPlacementProperties().getOrigin().getY().doubleValue();
		else
			originY = world.getSpawnPoint().getY();

		if (nparams.getRandomPlacementProperties().getOrigin().getZ() != null)
			originZ = nparams.getRandomPlacementProperties().getOrigin().getZ().doubleValue();
		else
			originZ = world.getSpawnPoint().getZ();

		radius = nparams.getRandomPlacementProperties().getRadius().doubleValue();
		minDist = nparams.getMinRandomizedDistance().doubleValue();
		maxDist = nparams.getMaxRandomizedDistance().doubleValue();

		double placementX = 0, placementY = 0, placementZ = 0;

		if (nparams.getRandomPlacementProperties().getPlacement() == "surface") {
			placementX = ((Math.random() - 0.5) * 2 * radius) + originX;
			placementZ = (Math.random() > 0.5 ? -1 : 1) * Math.sqrt((radius * radius) - (placementX * placementX))
					+ originZ;
			BlockPos pos = world.getHeight(new BlockPos(placementX, 0, placementZ));
			placementY = pos.getY();
		} else if (nparams.getRandomPlacementProperties().getPlacement() == "circle") {
			placementX = ((Math.random() - 0.5) * 2 * radius) + originX;
			placementY = originY;
			placementZ = (Math.random() > 0.5 ? -1 : 1) * Math.sqrt((radius * radius) - (placementX * placementX))
					+ originZ;
		} else {
			placementX = ((Math.random() - 0.5) * 2 * radius) + originX;
			placementY = (Math.random() - 0.5) * 2 * Math.sqrt((radius * radius) - (placementX * placementX)) + originY;
			placementZ = (Math.random() > 0.5 ? -1 : 1)
					* Math.sqrt((radius * radius) - (placementX * placementX) - (placementY * placementY)) + originZ;
		}

		IBlockState state = MinecraftTypeHelper
				.ParseBlockType(nparams.getRandomPlacementProperties().getBlock().value());
		world.setBlockState(new BlockPos(placementX, placementY, placementZ), state);

		// Set compass location to the block
		double xDel = 0, yDel = 0, zDel = 0;
		if (nparams.isRandomizeCompassLocation()) {
			double dist = 0;
			do {
				xDel = (Math.random() - 0.5) * 2 * maxDist;
				yDel = (Math.random() - 0.5) * 2 * maxDist;
				zDel = (Math.random() - 0.5) * 2 * maxDist;
				dist = Math.sqrt(xDel * xDel + yDel * yDel + zDel * zDel);
			} while (dist <= maxDist && dist >= minDist);
		}

		// Set compass logic
		// Since compasses point to world spawn, point at world spawn
		world.setSpawnPoint(new BlockPos(placementX + xDel, placementY + yDel, placementZ + zDel));
		Minecraft.getMinecraft().player.setSpawnPoint(new BlockPos(originX, originY, originZ), true);
	}

	@Override
	public boolean getExtraAgentHandlersAndData(List<Object> handlers, Map<String, String> data) {
		return false;
	}

	@Override
	public void update(World world) {
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
