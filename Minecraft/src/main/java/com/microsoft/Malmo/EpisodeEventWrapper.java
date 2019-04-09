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

package com.microsoft.Malmo;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.microsoft.Malmo.Utils.TimeHelper.SyncTickEvent;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Class that is responsible for catching all the Forge events we require (server ticks, client ticks, etc)
 * and passing them on to the current episode.<br>
 * Doing it this way saves us having to register/deregister each individual episode, which was causing race-condition vulnerabilities.
 */
public class EpisodeEventWrapper {
    /** The current episode, if there is one. */
    private StateEpisode stateEpisode = null;

    /** Lock to prevent the state episode being changed whilst mid event.<br>
     * This does not prevent multiple events from *reading* the stateEpisode, but
     * it won't allow *writing* to the stateEpisode whilst it is being read.
     */
    ReentrantReadWriteLock stateEpisodeLock = new ReentrantReadWriteLock();

    /** Set our state to a new episode.<br>
     * This waits on the stateEpisodeLock to prevent the episode being changed whilst in use.
     * @param stateEpisode the episode to switch to.
     * @return the previous state episode.
     */
    public StateEpisode setStateEpisode(StateEpisode stateEpisode)
    {
        this.stateEpisodeLock.writeLock().lock();
    	StateEpisode lastEpisode = this.stateEpisode;
        this.stateEpisode = stateEpisode;
        this.stateEpisodeLock.writeLock().unlock();
        return lastEpisode;
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev)
    {
        // Now pass the event on to the active episode, if there is one:
        this.stateEpisodeLock.readLock().lock();
        if (this.stateEpisode != null && this.stateEpisode.isLive())
        {
        	try
        	{
        		this.stateEpisode.onClientTick(ev);
        	}
        	catch (Exception e)
        	{
        		// Do what??
        	}
        }
        this.stateEpisodeLock.readLock().unlock();
    }

    @SubscribeEvent 
    public void onSyncTickEvent(SyncTickEvent ev){
        this.stateEpisodeLock.readLock().lock();

        if(this.stateEpisode != null && this.stateEpisode.isLive())
        {
            this.stateEpisode.onSyncTick(ev);
        }
        this.stateEpisodeLock.readLock().unlock();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent ev)
    {
        // Pass the event on to the active episode, if there is one:
        this.stateEpisodeLock.readLock().lock();
        if (this.stateEpisode != null && this.stateEpisode.isLive())
        {
            this.stateEpisode.onServerTick(ev);
        }
        this.stateEpisodeLock.readLock().unlock();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent ev)
    {
        // Pass the event on to the active episode, if there is one:
        this.stateEpisodeLock.readLock().lock();
        if (this.stateEpisode != null && this.stateEpisode.isLive())
        {
            this.stateEpisode.onPlayerTick(ev);
        }
        this.stateEpisodeLock.readLock().unlock();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev)
    {
        // Pass the event on to the active episode, if there is one:
        this.stateEpisodeLock.readLock().lock();
        if (this.stateEpisode != null && this.stateEpisode.isLive())
        {
            this.stateEpisode.onRenderTick(ev);
        }
        this.stateEpisodeLock.readLock().unlock();
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load cev)
    {
        // Pass the event on to the active episode, if there is one:
        this.stateEpisodeLock.readLock().lock();
        if (this.stateEpisode != null && this.stateEpisode.isLive())
        {
            this.stateEpisode.onChunkLoad(cev);
        }
        this.stateEpisodeLock.readLock().unlock();
    }

    @SubscribeEvent
    public void onPlayerDies(LivingDeathEvent lde)
    {
        // Pass the event on to the active episode, if there is one:
        this.stateEpisodeLock.readLock().lock();
        if (this.stateEpisode != null && this.stateEpisode.isLive())
        {
            this.stateEpisode.onPlayerDies(lde);
        }
        this.stateEpisodeLock.readLock().unlock();
    }
    
    @SubscribeEvent
    public void onConfigChanged(OnConfigChangedEvent ev)
    {
    	if (ev.getModID() == MalmoMod.MODID)	// Check we are responding to the correct Mod's event!
    	{
    		this.stateEpisodeLock.readLock().lock();
    		if (this.stateEpisode != null && this.stateEpisode.isLive())
    		{
    			this.stateEpisode.onConfigChanged(ev);
    		}
    		else
    		{
    			//TODO - should we make sure this config change is acted on?
    		}
    		this.stateEpisodeLock.readLock().unlock();
    	}
    }
    
    @SubscribeEvent
    public void onPlayerJoinedServer(PlayerLoggedInEvent ev)
    {
    	this.stateEpisodeLock.readLock().lock();
    	if (this.stateEpisode != null && this.stateEpisode.isLive())
    	{
    		this.stateEpisode.onPlayerJoinedServer(ev);
    	}
    	this.stateEpisodeLock.readLock().unlock();
    }
}