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

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldType;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldGenerator;
import com.microsoft.Malmo.Schemas.FlatWorldGenerator;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.MapFileHelper;

public class FlatWorldGeneratorImplementation extends HandlerBase implements IWorldGenerator
{
	FlatWorldGenerator fwparams;
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof FlatWorldGenerator))
			return false;
		
		this.fwparams = (FlatWorldGenerator)params;
		return true;
	}

    @Override
    public boolean createWorld(MissionInit missionInit)
    {
        long seed = DefaultWorldGeneratorImplementation.getWorldSeedFromString(this.fwparams.getSeed());
        WorldSettings worldsettings = new WorldSettings(seed, GameType.SURVIVAL, false, false, WorldType.FLAT);
        // This call to setWorldName allows us to specify the layers of our world, and also the features that will be created.
        // This website provides a handy way to generate these strings: http://chunkbase.com/apps/superflat-generator
        worldsettings.setGeneratorOptions(this.fwparams.getGeneratorString());
        worldsettings.enableCommands(); // Enables cheat commands.
        // Create a filename for this map - we use the time stamp to make sure it is different from other worlds, otherwise no new world
        // will be created, it will simply load the old one.
        return MapFileHelper.createAndLaunchWorld(worldsettings, this.fwparams.isDestroyAfterUse());
    }

    @Override
    public boolean shouldCreateWorld(MissionInit missionInit, World world)
    {
    	if (this.fwparams != null && this.fwparams.isForceReset())
    	    return true;
    	
        if (Minecraft.getMinecraft().world == null || world == null)
            return true;    // Definitely need to create a world if there isn't one in existence!

        WorldInfo worldInfo =  world.getWorldInfo();
        if (worldInfo == null)
            return true;

        String genOptions = worldInfo.getGeneratorOptions();
        if (genOptions == null)
            return true;

        if (!genOptions.equals(this.fwparams.getGeneratorString()))
            return true;    // Generation doesn't match, so recreate.
        
        return false;
    }
    
    @Override
    public String getErrorDetails()
    {
        return "";  // Currently no error exit points, so never anything to report.
    }
}
