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
	//private double discoveryRadius;
	private double randomizedRadius;

	@Override
	public boolean parseParameters(Object params) {
		if (params == null || !(params instanceof NavigationDecorator))
			return false;
		this.nparams = (NavigationDecorator) params;
		return true;
	}

	@Override
	public void buildOnWorld(MissionInit missionInit, World world) throws DecoratorException {
		originX = nparams.getOrigin().getXCoordinate().doubleValue();
		originY = nparams.getOrigin().getYCoordinate().doubleValue();
		originZ = nparams.getOrigin().getZCoordinate().doubleValue();
		radius = nparams.getRadius().doubleValue();
		//discoveryRadius = nparams.getDiscoveryRadius().doubleValue();
		randomizedRadius = nparams.getRandomizedCompassRadius().doubleValue();

		double placementX = 0, placementY = 0, placementZ = 0;

		if (nparams.getPlacement() == "surface") {
			placementX = ((Math.random() - 0.5) * 2 * radius) + originX;
			placementZ = (Math.random() > 0.5 ? -1 : 1) * Math.sqrt((radius * radius) - (placementX * placementX))
					+ originZ;
			BlockPos pos = world.getHeight(new BlockPos(placementX, 0, placementZ));
			placementY = pos.getY();
		} else if (nparams.getPlacement() == "circle") {
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

		IBlockState state = MinecraftTypeHelper.ParseBlockType(nparams.getBlock().value());
		world.setBlockState(new BlockPos(placementX, placementY, placementZ), state);

		// Set compass location to the block
		double xDel = 0, yDel = 0, zDel = 0;
		if (nparams.isRandomizeCompassLocation()) {
			// Set some error
			xDel = (Math.random() - 0.5) * 2 * randomizedRadius;
			yDel = (Math.random() - 0.5) * 2 * Math.sqrt((randomizedRadius * randomizedRadius) - (xDel * xDel));
			zDel = (Math.random() > 0.5 ? -1 : 1)
					* Math.sqrt((randomizedRadius * randomizedRadius) - (xDel * xDel) - (yDel * yDel));
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
