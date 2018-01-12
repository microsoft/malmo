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

package com.microsoft.Malmo.Server;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.TimeHelper;

public class MalmoModServer
{
    private ServerStateMachine stateMachine;
    private TimeHelper.TickRateMonitor serverTickMonitor = new TimeHelper.TickRateMonitor();

    /**
     * Called when creating a dedicated server
     */
    public void init(FMLInitializationEvent event)
    {
        initBusses();
        this.stateMachine = new ServerStateMachine(ServerState.WAITING_FOR_MOD_READY);
    }

    /**
     * Called when creating an integrated server
     */
    public void init(MissionInit minit)
    {
        initBusses();
        this.stateMachine = new ServerStateMachine(ServerState.WAITING_FOR_MOD_READY, minit);
    }

    /**
     * Provides a direct way for the owner of the integrated server to send a new mission init message.<br>
     * Don't call this unless the server has been initialised first.
     */
    public void sendMissionInitDirectToServer(MissionInit minit)
    {
        this.stateMachine.setMissionInit(minit);
    }

    private void initBusses()
    {
        // Register for various events:
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent ev)
    {
        if (ev.side == Side.SERVER && ev.phase == Phase.START)
        {
            this.serverTickMonitor.beat();
        }
    }

    public float getServerTickRate()
    {
        return this.serverTickMonitor.getEventsPerSecond();
    }
}
