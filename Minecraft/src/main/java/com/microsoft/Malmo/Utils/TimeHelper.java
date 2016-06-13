package com.microsoft.Malmo.Utils;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.Timer;
import net.minecraft.world.World;

/** Time-based methods and helpers.<br>
 * Time is usually measured in some form of game tick (eg WorldTick etc). In the normal course of operations,
 * these take places 20 times a second - hence a MillisencondsPerWorldTick value of 50.
 * If the game is overclocked, this is no longer true, but generally it still makes sense to deal with a simple multiple of game ticks,
 * so we leave MillisecondsPerWorldTick unchanged.
 */
public class TimeHelper
{
    public final static float MillisecondsPerWorldTick = 50.0f;
    public final static float MillisecondsPerSecond = 1000.0f;
    public static long serverTickLength = 50;
    
    /** Very simple stopwatch-style timer class; times in WorldTicks.
     */
    static public class WorldTimer
    {
        private World world;
        private long startTime = 0;
        private long stopTime = 0;
        
        public WorldTimer(World world)
        {
            this.world = world;
        }
        
        /** Start timing
         */
        public void start()
        {
            this.startTime = this.world.getTotalWorldTime();
            this.stopTime = 0;
        }
        
        /** Stop timing
         */
        public void stop()
        {
            this.stopTime = this.world.getTotalWorldTime();
        }
        
        /** Get the timed duration, converted into what would be milliseconds if no over-clocking has occurred.<br>
         * If stop() has been called, returns the time between calls to stop() and start().
         * If start() has been called but not stop, returns the time since start() was called.<br>
         * It is up to the user to avoid doing things in a stupid order.
         * @return the measured duration
         */
        public float getDurationInMs()
        {
            long duration = (stopTime != 0) ? this.stopTime - this.startTime : this.world.getTotalWorldTime() - this.startTime;
            return duration * MillisecondsPerWorldTick;
        }
    }
    
    static public boolean setMinecraftClientClockSpeed(float ticksPerSecond)
    {
        boolean devEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
        // We need to know, because the member name will either be obfuscated or not.
        String timerMemberName = devEnv ? "timer" : "field_71428_T";
        // NOTE: obfuscated name may need updating if Forge changes - search for "timer" in Malmo\Minecraft\build\tasklogs\retromapSources.log
        Field timer;
        try
        {
            timer = Minecraft.class.getDeclaredField(timerMemberName);
            timer.setAccessible(true);
            timer.set(Minecraft.getMinecraft(), new Timer(ticksPerSecond));
            return true;
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
        return false;
    }
}
