package com.microsoft.Malmo.Utils;

import java.io.File;
import java.io.IOException;

import net.minecraftforge.fml.client.FMLClientHandler;

import org.apache.commons.io.FileUtils;

/**
 * Helper methods for manipulating maps, files etc.
 */
public class MapFileHelper
{
    /** Attempt to copy the specified file into the Minecraft saves folder.
     * @param mapFile full path to the map file required
     * @param overwriteOldFiles if false, will rename copy to avoid overwriting any other saved games
     * @return if successful, a File object representing the new copy, which can be fed to Minecraft to load - otherwise null.
     */
    static public File copyMapFiles(File mapFile, boolean overwriteOldFiles)
    {
        System.out.println("Current directory: "+System.getProperty("user.dir"));
        // Look for the basemap file.
        // If it exists, copy it into the Minecraft saves folder,
        // and attempt to load it.
        File savesDir = FMLClientHandler.instance().getSavesDir();
        File dst = null;
        if (mapFile != null && mapFile.exists())
        {
            String name = mapFile.getName();
            dst = new File(savesDir, name);
            int version = 0;
            // Avoid name collisions, if we are not allowed to overwrite old files:
            while (dst.exists() && !overwriteOldFiles)
            {
                dst = new File(savesDir, name + "_" + version);
                version++;
            }
            try
            {
                if (dst.exists() && overwriteOldFiles)
                {
                    // Safest to empty out the destination directory, since copyDirectory will just
                    // copy across the source files, merging them in to the destination. This could result in
                    // odd behaviour as two Minecraft saved worlds get merged.
                    FileUtils.deleteDirectory(dst);
                }
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
}