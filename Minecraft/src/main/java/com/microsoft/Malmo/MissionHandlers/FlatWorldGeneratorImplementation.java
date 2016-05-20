package com.microsoft.Malmo.MissionHandlers;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldType;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldGenerator;
import com.microsoft.Malmo.Schemas.FlatWorldGenerator;
import com.microsoft.Malmo.Schemas.MissionInit;

public class FlatWorldGeneratorImplementation extends HandlerBase implements IWorldGenerator
{
	WorldSettings.GameType gameType;
	String genString;
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof FlatWorldGenerator))
			return false;
		
		FlatWorldGenerator fwparams = (FlatWorldGenerator)params;
		this.gameType = GameType.SURVIVAL;
		this.genString = fwparams.getGeneratorString();
		return true;
	}

    @Override
    public boolean createWorld(MissionInit missionInit)
    {
        create(this.genString, this.gameType);
        return true;
    }
    
    static public void create(String genString, WorldSettings.GameType gametype)
    {
        WorldSettings worldsettings = new WorldSettings(0, gametype, false, false, WorldType.FLAT);
        // This call to setWorldName allows us to specify the layers of our world, and also the features that will be created.
        // TODO: this should be a parameter in the Mission XML.
        // This website provides a handy way to generate these strings: http://chunkbase.com/apps/superflat-generator
        worldsettings.setWorldName(genString);
        worldsettings.enableCommands(); // Enables cheat commands.
        // Create a filename for this map - we use the time stamp to make sure it is different from other worlds, otherwise no new world
        // will be created, it will simply load the old one.
        String s = SimpleDateFormat.getDateTimeInstance().format(new Date()).replace(":", "_");
        Minecraft.getMinecraft().launchIntegratedServer(s, s, worldsettings);
    }

    @Override
    public boolean shouldCreateWorld(MissionInit missionInit)
    {
    	World world = null;
    	MinecraftServer server = MinecraftServer.getServer();
    	if (server.worldServers != null && server.worldServers.length != 0)
    		world = server.getEntityWorld();
    	
        // TODO - allow a parameter to specify whether the world should *always* be recreated.
        if (Minecraft.getMinecraft().theWorld == null && world == null)
            return true;    // Definitely need to create a world if there isn't one in existence!
        
        if (!world.getWorldInfo().getGameType().equals(this.gameType))
            return true;    // Game types don't match, so recreate.

        String genOptions = world.getWorldInfo().getGeneratorOptions();
        if (!genOptions.equals(this.genString))
            return true;    // Generation doesn't match, so recreate.
        
        return false;
    }
}
