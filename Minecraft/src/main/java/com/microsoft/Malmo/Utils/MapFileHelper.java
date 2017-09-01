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

package com.microsoft.Malmo.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.AnvilConverterException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldSummary;
import net.minecraftforge.fml.client.FMLClientHandler;

import org.apache.commons.io.FileUtils;

/**
 * Helper methods for manipulating maps, files etc.
 */
public class MapFileHelper
{
    static final String tempMark = "TEMP_";

    /** Attempt to copy the specified file into the Minecraft saves folder.
     * @param mapFile full path to the map file required
     * @param overwriteOldFiles if false, will rename copy to avoid overwriting any other saved games
     * @return if successful, a File object representing the new copy, which can be fed to Minecraft to load - otherwise null.
     */
    static public File copyMapFiles(File mapFile, boolean isTemporary)
    {
        System.out.println("Current directory: "+System.getProperty("user.dir"));
        // Look for the basemap file.
        // If it exists, copy it into the Minecraft saves folder,
        // and attempt to load it.
        File savesDir = FMLClientHandler.instance().getSavesDir();
        File dst = null;
        if (mapFile != null && mapFile.exists())
        {
            dst = new File(savesDir, getNewSaveFileLocation(isTemporary));

            try
            {
                FileUtils.copyDirectory(mapFile, dst);
            }
            catch (IOException e)
            {
                System.out.println("Failed to load file: " + mapFile.getPath());
                return null;
            }
        }
        
        return dst;
    }

    /** Get a filename to use for creating a new Minecraft save map.<br>
     * Ensure no duplicates.
     * @param isTemporary mark the filename such that the file management code knows to delete this later
     * @return a unique filename (relative to the saves folder)
     */
    public static String getNewSaveFileLocation(boolean isTemporary) {
        File dst;
        File savesDir = FMLClientHandler.instance().getSavesDir();
        do {
            // We used to create filenames based on the current date/time, but this can cause problems when
            // multiple clients might be writing to the same save location. Instead, use a GUID:
            String s = UUID.randomUUID().toString();

            // Add our port number, to help with file management:
            s = AddressHelper.getMissionControlPort() + "_" + s;

            // If this is a temp file, mark it as such:
            if (isTemporary) {
                s = tempMark + s;
            }

            dst = new File(savesDir, s);
        } while (dst.exists());

        return dst.getName();
    }
    /**
     * Creates and launches a unique world according to the settings. 
     * @param worldsettings the world's settings
     * @param isTemporary if true, the world will be deleted whenever newer worlds are created
     * @return
     */
    public static boolean createAndLaunchWorld(WorldSettings worldsettings, boolean isTemporary)
    {
        String s = getNewSaveFileLocation(isTemporary);
        Minecraft.getMinecraft().launchIntegratedServer(s, s, worldsettings);
        cleanupTemporaryWorlds(s);
        return true;
    }

    /**
     * Attempts to delete all Minecraft Worlds with "TEMP_" in front of the name
     * @param currentWorld excludes this world from deletion, can be null
     */
    public static void cleanupTemporaryWorlds(String currentWorld){
        List<WorldSummary> saveList;
        ISaveFormat isaveformat = Minecraft.getMinecraft().getSaveLoader();
        isaveformat.flushCache();

        try{
            saveList = isaveformat.getSaveList();
        } catch (AnvilConverterException e){
            e.printStackTrace();
            return;
        }

        String searchString = tempMark + AddressHelper.getMissionControlPort() + "_";

        for (WorldSummary s: saveList){
            String folderName = s.getFileName();
            if (folderName.startsWith(searchString) && !folderName.equals(currentWorld)){
                isaveformat.deleteWorldDirectory(folderName);
            }
        }
    }
}