package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RandomPlacementDecorator;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

public class RandomPlacementDecoratorImplementation extends HandlerBase implements IWorldDecorator {

	private RandomPlacementDecorator rparams;

	private double originX, originY, originZ;
	private double radius;

	@Override
	public boolean parseParameters(Object params) {
		if (params == null || !(params instanceof RandomPlacementDecorator))
			return false;
		this.rparams = (RandomPlacementDecorator) params;
		return true;
	}

	@Override
	public void buildOnWorld(MissionInit missionInit, World world) throws DecoratorException {
		if (rparams.getOrigin().getXCoordinate() != null)
			originX = rparams.getOrigin().getXCoordinate().doubleValue();
		else
			originX = world.getSpawnPoint().getX();
		
		if (rparams.getOrigin().getYCoordinate() != null)
			originY = rparams.getOrigin().getYCoordinate().doubleValue();
		else
			originY = world.getSpawnPoint().getY();
		
		if (rparams.getOrigin().getZCoordinate() != null)
			originZ = rparams.getOrigin().getZCoordinate().doubleValue();
		else
			originZ = world.getSpawnPoint().getZ();
		
		originY = rparams.getOrigin().getYCoordinate().doubleValue();
		originZ = rparams.getOrigin().getZCoordinate().doubleValue();
		radius = rparams.getRadius().doubleValue();

		double placementX = 0, placementY = 0, placementZ = 0;

		if (rparams.getPlacement() == "surface") {
			placementX = ((Math.random() - 0.5) * 2 * radius) + originX;
			placementZ = (Math.random() > 0.5 ? -1 : 1) * Math.sqrt((radius * radius) - (placementX * placementX))
					+ originZ;
			BlockPos pos = world.getHeight(new BlockPos(placementX, 0, placementZ));
			placementY = pos.getY();
		} else if (rparams.getPlacement() == "circle") {
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

		IBlockState state = MinecraftTypeHelper.ParseBlockType(rparams.getBlock().value());
		world.setBlockState(new BlockPos(placementX, placementY, placementZ), state);
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
