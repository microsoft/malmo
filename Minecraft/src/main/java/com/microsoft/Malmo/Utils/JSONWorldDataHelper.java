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

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.stats.StatisticsManagerServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Helper class for building the "World data" to be passed from Minecraft back to the agent.<br>
 * This class contains helper methods to build up a JSON tree of useful information, such as health, XP, food levels, distance travelled, etc.etc.<br>
 * It can also build up a grid of the block types around the player or somewhere else in the world.
 * Call this on the Server side only.
 */
public class JSONWorldDataHelper
{
    /**
     * Simple class to hold the dimensions of the environment
     * that we want to return in the World Data.<br>
     * Min and max define an inclusive range, where the player's feet are situated at (0,0,0) if absoluteCoords=false.
     */
    static public class GridDimensions {
        public int xMin;
        public int xMax;
        public int yMin;
        public int yMax;
        public int zMin;
        public int zMax;
        public boolean absoluteCoords;
        
        /**
         * Default constructor asks for an environment just big enough to contain
         * the player and one block all around him.
         */
        public GridDimensions() {
            this.xMin = -1; this.xMax = 1;
            this.zMin = -1; this.zMax = 1;
            this.yMin = -1; this.yMax = 2;
            this.absoluteCoords = false;
        }
        
        /**
         * Convenient constructor - effectively specifies the margin around the player<br>
         * Passing (1,1,1) will have the same effect as the default constructor.
         * @param xMargin number of blocks to the left and right of the player
         * @param yMargin number of blocks above and below player
         * @param zMargin number of blocks in front of and behind player
         */
        public GridDimensions(int xMargin, int yMargin, int zMargin) {
            this.xMin = -xMargin; this.xMax = xMargin;
            this.yMin = -yMargin; this.yMax = yMargin + 1;  // +1 because the player is two blocks tall.
            this.zMin = -zMargin; this.zMax = zMargin;
            this.absoluteCoords = false;
        }
        
        /**
         * Convenient constructor for the case where all that is required is the flat patch of ground<br>
         * around the player's feet.
         * @param xMargin number of blocks around the player in the x-axis
         * @param zMargin number of blocks around the player in the z-axis
         */
        public GridDimensions(int xMargin, int zMargin) {
            this.xMin = -xMargin; this.xMax = xMargin;
            this.yMin = -1; this.yMax = -1;  // Flat patch of ground at the player's feet.
            this.zMin = -zMargin; this.zMax = zMargin;
            this.absoluteCoords = false;
        }
    };
    
    /** Builds the basic achievement world data to be used as observation signals by the listener.
     * @param json a JSON object into which the achievement stats will be added.
     */
    public static void buildAchievementStats(JsonObject json, EntityPlayerSP player)
    {
        if(Minecraft.getMinecraft().isIntegratedServerRunning()){

            StatisticsManagerServer sfw = Minecraft.getMinecraft().getIntegratedServer().getPlayerList().getPlayerStatsFile(player);

            json.addProperty("DistanceTravelled", 
                sfw.readStat((StatBase)StatList.WALK_ONE_CM) 
                + sfw.readStat((StatBase)StatList.SWIM_ONE_CM)
                + sfw.readStat((StatBase)StatList.DIVE_ONE_CM) 
                + sfw.readStat((StatBase)StatList.FALL_ONE_CM)
                ); // TODO: there are many other ways of moving!
            json.addProperty("TimeAlive", sfw.readStat((StatBase)StatList.TIME_SINCE_DEATH));
            json.addProperty("MobsKilled", sfw.readStat((StatBase)StatList.MOB_KILLS));
            json.addProperty("PlayersKilled", sfw.readStat((StatBase)StatList.PLAYER_KILLS));
            json.addProperty("DamageTaken", sfw.readStat((StatBase)StatList.DAMAGE_TAKEN));
            json.addProperty("DamageDealt", sfw.readStat((StatBase)StatList.DAMAGE_DEALT));
        }

        

        /* Other potential reinforcement signals that may be worth researching:
        json.addProperty("BlocksDestroyed", sfw.readStat((StatBase)StatList.objectBreakStats) - but objectBreakStats is an array of 32000 StatBase objects - indexed by block type.);
        json.addProperty("Blocked", ev.player.isMovementBlocked()) - but isMovementBlocker() is a protected method (can get round this with reflection)
        */
    }
    
    /** Builds the basic life world data to be used as observation signals by the listener.
     * @param json a JSON object into which the life stats will be added.
     */
    public static void buildLifeStats(JsonObject json, EntityPlayerSP player)
    {
        json.addProperty("Life", player.getHealth());
        json.addProperty("Score", player.getScore());    // Might always be the same as XP?
        json.addProperty("Food", player.getFoodStats().getFoodLevel());
        json.addProperty("XP", player.experienceTotal);
        json.addProperty("IsAlive", !player.isDead);
        json.addProperty("Air", player.getAir());
        json.addProperty("Name", player.getName());
    }
    
    /** Builds the player position data to be used as observation signals by the listener.
     * @param json a JSON object into which the positional information will be added.
     */
    public static void buildPositionStats(JsonObject json, EntityPlayerSP player)
    {
        json.addProperty("XPos",  player.posX);
        json.addProperty("YPos",  player.posY);
        json.addProperty("ZPos", player.posZ);
        json.addProperty("Pitch",  player.rotationPitch);
        json.addProperty("Yaw", player.rotationYaw);
    }

    public static void buildEnvironmentStats(JsonObject json, EntityPlayerSP player)
    {
        json.addProperty("WorldTime", player.world.getWorldTime());  // Current time in ticks
        json.addProperty("TotalTime", player.world.getTotalWorldTime());  // Total time world has been running
    }
    /**
     * Build a signal for the cubic block grid centred on the player.<br>
     * Default is 3x3x4. (One cube all around the player.)<br>
     * Blocks are returned as a 1D array, in order
     * along the x, then z, then y axes.<br>
     * Data will be returned in an array called "Cells"
     * @param json a JSON object into which the info for the object under the mouse will be added.
     * @param environmentDimensions object which specifies the required dimensions of the grid to be returned.
     * @param jsonName name to use for identifying the returned JSON array.
     */
    public static void buildGridData(JsonObject json, GridDimensions environmentDimensions, EntityPlayerMP player, String jsonName)
    {
        if (player == null || json == null)
            return;

        JsonArray arr = new JsonArray();
        BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
        for (int y = environmentDimensions.yMin; y <= environmentDimensions.yMax; y++)
        {
            for (int z = environmentDimensions.zMin; z <= environmentDimensions.zMax; z++)
            {
                for (int x = environmentDimensions.xMin; x <= environmentDimensions.xMax; x++)
                {
                    BlockPos p;
                    if( environmentDimensions.absoluteCoords )
                        p = new BlockPos(x, y, z);
                    else
                        p = pos.add(x, y, z);
                    String name = "";
                    IBlockState state = player.world.getBlockState(p);
                    Object blockName = Block.REGISTRY.getNameForObject(state.getBlock());
                    if (blockName instanceof ResourceLocation)
                    {
                        name = ((ResourceLocation)blockName).getResourcePath();
                    }
                    JsonElement element = new JsonPrimitive(name);
                    arr.add(element);
                }
            }
        }
        json.add(jsonName, arr);
    }
}
