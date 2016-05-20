package com.microsoft.Malmo;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.NameFormat;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SetAgentNameMessage implements IMessage
{
	private String agentName;

	public SetAgentNameMessage()
	{
	}

	public SetAgentNameMessage(String name)
	{
		this.agentName = name;
	}
	
	@Override
	public void fromBytes(ByteBuf buf)
	{
		 this.agentName = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		ByteBufUtils.writeUTF8String(buf, this.agentName);
	}
	
	public static class Handler implements IMessageHandler<SetAgentNameMessage, IMessage>
	{
		public Handler()
		{
		}
		@Override
		public IMessage onMessage(final SetAgentNameMessage message, final MessageContext ctx)
		{
			IThreadListener mainThread = (WorldServer)ctx.getServerHandler().playerEntity.worldObj; // or Minecraft.getMinecraft() on the client
			mainThread.addScheduledTask(new Runnable() {
				@Override
				public void run() {
					EntityPlayerMP player = ctx.getServerHandler().playerEntity;
					SetAgentNameActor actor = new SetAgentNameActor(player, message.agentName);
					actor.go();
				}
			});
			return null; // no response in this case
		}
		
		@SubscribeEvent
		protected void OnGetDisplayName(NameFormat ev)
		{
			ev.displayname = ev.entityPlayer.getCustomNameTag();
		}
    }
	
	/** Tiny helper class that does the actual name setting.<br>
	 * Made a standalone object to make it reusable from other places.
	 */
	public static class SetAgentNameActor
	{
		EntityPlayerMP player;
		String name;
		
		public SetAgentNameActor(EntityPlayerMP player, String agentName)
		{
			this.player = player;
			this.name = agentName;
			MinecraftForge.EVENT_BUS.register(this);
		}

		@SubscribeEvent
		protected void OnGetDisplayName(NameFormat ev)
		{
			ev.displayname = ev.entityPlayer.getCustomNameTag();
		}
		
		public void go()
		{
			player.setCustomNameTag(this.name);
			player.setAlwaysRenderNameTag(true);
			player.refreshDisplayName();	// Force the custom name to be used in the display name.
			MinecraftForge.EVENT_BUS.unregister(this);
		}
	}
}
