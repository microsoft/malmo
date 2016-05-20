package com.microsoft.Malmo.Server;

import com.microsoft.Malmo.Schemas.MissionInit;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class MalmoModServer
{
	private ServerStateMachine stateMachine;
	
	/** Called when creating a dedicated server
	 */
	public void init(FMLInitializationEvent event)
	{
		initBusses();
        this.stateMachine = new ServerStateMachine(ServerState.WAITING_FOR_MOD_READY);
	}

	/** Called when creating an integrated server
	 */
	public void init(MissionInit minit)
	{
		initBusses();
        this.stateMachine = new ServerStateMachine(ServerState.WAITING_FOR_MOD_READY, minit);
	}
	
	/** Provides a direct way for the owner of the integrated server to send a new mission init message.<br>
	 * Don't call this unless the server has been initialised first.*/
	public void sendMissionInitDirectToServer(MissionInit minit)
	{
		this.stateMachine.setMissionInit(minit);
	}

	private void initBusses()
	{
        // Register for various events:
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

}
