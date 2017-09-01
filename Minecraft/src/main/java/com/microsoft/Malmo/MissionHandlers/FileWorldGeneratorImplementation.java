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

import java.io.File;
import java.util.List;

import net.minecraft.client.AnvilConverterException;
import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldSummary;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldGenerator;
import com.microsoft.Malmo.Schemas.FileWorldGenerator;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.MapFileHelper;

public class FileWorldGeneratorImplementation extends HandlerBase implements IWorldGenerator
{
    String mapFilename;
    FileWorldGenerator fwparams;
    String errorDetails;

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof FileWorldGenerator))
            return false;

        this.fwparams = (FileWorldGenerator) params;
        this.mapFilename = fwparams.getSrc();
        return true;
    }

    @Override
    public boolean createWorld(MissionInit missionInit)
    {
        if (this.mapFilename == null || this.mapFilename.length() == 0)
        {
            this.errorDetails = "No basemap URI provided - check your Mission XML.";
            return false;
        }
        File mapSource = new File(this.mapFilename);
        if (!mapSource.exists())
        {
            this.errorDetails = "Basemap file " + this.mapFilename + " was not found - check your Mission XML and ensure the file exists on the Minecraft client machine.";
            return false;
        }
        if (!mapSource.isDirectory())
        {
            this.errorDetails = "Basemap location " + this.mapFilename + " needs to be a folder. Check the path in your Mission XML.";
            return false;
        }
        File mapCopy = MapFileHelper.copyMapFiles(mapSource, this.fwparams.isDestroyAfterUse());
        if (mapCopy == null)
        {
            this.errorDetails = "Unable to copy " + this.mapFilename + " - is the hard drive full?";
            return false;
        }
        if (!Minecraft.getMinecraft().getSaveLoader().canLoadWorld(mapCopy.getName()))
        {
            this.errorDetails = "Minecraft is unable to load " + this.mapFilename + " - is it a valid saved world?";
            return false;
        }

        ISaveFormat isaveformat = Minecraft.getMinecraft().getSaveLoader();
        List<WorldSummary> worldlist;
        try
        {
            worldlist = isaveformat.getSaveList();
        }
        catch (AnvilConverterException anvilconverterexception)
        {
        	this.errorDetails = "Minecraft couldn't rebuild saved world list.";
            return false;
        }

        WorldSummary newWorld = null;
        for (WorldSummary ws : worldlist)
        {
            if (ws.getFileName().equals(mapCopy.getName()))
                newWorld = ws;
        }
        if (newWorld == null)
        {
            this.errorDetails = "Minecraft could not find the copied world.";
            return false;
        }

        net.minecraftforge.fml.client.FMLClientHandler.instance().tryLoadExistingWorld(null, newWorld);
        IntegratedServer server = Minecraft.getMinecraft().getIntegratedServer();
        String worldName = (server != null) ? server.getWorldName() : null;
        if (worldName == null || !worldName.equals(newWorld.getDisplayName()))
        {
            this.errorDetails = "Minecraft could not load " + this.mapFilename + " - is it a valid saved world?";
            return false;
        }
        MapFileHelper.cleanupTemporaryWorlds(mapCopy.getName());    // Now we are safely running a new file, we can attempt to clean up old ones.
        return true;
    }
    
    @Override
    public boolean shouldCreateWorld(MissionInit missionInit, World world)
    {
        if (this.fwparams != null && this.fwparams.isForceReset())
            return true;

        if (world == null)
            return true;   // There is no world, so we definitely need to create one.

        String name = (world != null) ? world.getWorldInfo().getWorldName() : "";
        // Extract the name from the path (need to cope with backslashes or forward slashes.)
        String mapfile = (this.mapFilename == null) ? "" : this.mapFilename;    // Makes no sense to have an empty filename, but createWorld will deal with it graciously.
        String[] parts = mapfile.split("[\\\\/]");
        if (name.length() > 0 && parts[parts.length - 1].equalsIgnoreCase(name) && Minecraft.getMinecraft().world != null)
            return false;	// We don't check whether the game modes match - it's up to the server state machine to sort that out.

        return true;	// There's no world, or the world is different to the basemap file, so create.
    }
    
    @Override
    public String getErrorDetails()
    {
        return this.errorDetails;
    }
}
