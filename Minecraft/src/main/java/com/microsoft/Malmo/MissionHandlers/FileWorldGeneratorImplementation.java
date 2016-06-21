package com.microsoft.Malmo.MissionHandlers;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldSettings.GameType;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldGenerator;
import com.microsoft.Malmo.Schemas.FileWorldGenerator;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.MapFileHelper;

public class FileWorldGeneratorImplementation extends HandlerBase implements IWorldGenerator
{
	String mapFilename;
	FileWorldGenerator fwparams;
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof FileWorldGenerator))
			return false;
		
		this.fwparams = (FileWorldGenerator)params;
		this.mapFilename = fwparams.getSrc();
		return true;
	}
	
    @Override
    public boolean createWorld(MissionInit missionInit)
    {
        if (this.mapFilename != null && this.mapFilename.length() > 0)
        {
            File mapSource = new File(this.mapFilename);
            File mapCopy = MapFileHelper.copyMapFiles(mapSource, true);
            if (mapCopy != null && Minecraft.getMinecraft().getSaveLoader().canLoadWorld(mapCopy.getName()))
            {
                net.minecraftforge.fml.client.FMLClientHandler.instance().tryLoadExistingWorld(null, mapCopy.getName(), mapSource.getName());
                return true;
            }
        }
        return false;   // Failed to load, or there was nothing to try to load.
    }
    
    @Override
    public boolean shouldCreateWorld(MissionInit missionInit)
    {
        if (this.fwparams != null && this.fwparams.isForceReset())
            return true;
        
        if (this.mapFilename == null || this.mapFilename.length() == 0)
        	return false;	// No basemap specified, so don't create a world.
    	World world = null;
    	MinecraftServer server = MinecraftServer.getServer();
    	if (server.worldServers != null && server.worldServers.length != 0)
    		world = server.getEntityWorld();

    	String name = (world != null) ? world.getWorldInfo().getWorldName() : "";
        // Extract the name from the path (need to cope with backslashes or forward slashes.)
        String[] parts = this.mapFilename.split("[\\\\/]");
        if (name.length() > 0 && parts[parts.length - 1].equalsIgnoreCase(name) && Minecraft.getMinecraft().theWorld != null)
        	return false;	// We don't check whether the game modes match - it's up to the server state machine to sort that out.

        return true;	// There's no world, or the world is different to the basemap file, so create.
    }
}
