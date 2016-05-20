package com.microsoft.Malmo.Utils;

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
}
