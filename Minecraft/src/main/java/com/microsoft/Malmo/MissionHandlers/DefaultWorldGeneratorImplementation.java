package com.microsoft.Malmo.MissionHandlers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.WorldSettings.GameType;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldGenerator;
import com.microsoft.Malmo.Schemas.DefaultWorldGenerator;
import com.microsoft.Malmo.Schemas.MissionInit;

public class DefaultWorldGeneratorImplementation extends HandlerBase implements IWorldGenerator
{
	WorldSettings.GameType gameType;
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof DefaultWorldGenerator))
			return false;
		
		DefaultWorldGenerator dwparams = (DefaultWorldGenerator)params;
		this.gameType = GameType.SURVIVAL;
		return true;
	}
	
    @Override
    public boolean createWorld(MissionInit missionInit)
    {
        create(this.gameType);
        return true;
    }
    
    static public void create(WorldSettings.GameType gametype)
    {
        long i = (new Random()).nextLong();
        WorldType.worldTypes[0].onGUICreateWorldPress();
        WorldSettings worldsettings = new WorldSettings(i, gametype, true, false, WorldType.worldTypes[0]);
        worldsettings.setWorldName("");
        worldsettings.enableCommands();
        // Create a filename for this map - we use the time stamp to make sure it is different from other worlds, otherwise no new world
        // will be created, it will simply load the old one.
        String s = SimpleDateFormat.getDateTimeInstance().format(new Date()).replace(":", "_");
        Minecraft.getMinecraft().launchIntegratedServer(s, s, worldsettings);
    }

    @Override
    public boolean shouldCreateWorld(MissionInit missionInit)
    {
        // TODO - allow a parameter to specify whether the world should *always* be recreated.
    	World world = null;
    	MinecraftServer server = MinecraftServer.getServer();
    	if (server.worldServers != null && server.worldServers.length != 0)
    		world = server.getEntityWorld();

    	if (Minecraft.getMinecraft().theWorld == null || world == null)
            return true;    // Definitely need to create a world if there isn't one in existence!

        String genOptions = world.getWorldInfo().getGeneratorOptions();
        if (genOptions != null && !genOptions.isEmpty())
        	return true;	// Default world has no generator options.

        return false;
    }
}