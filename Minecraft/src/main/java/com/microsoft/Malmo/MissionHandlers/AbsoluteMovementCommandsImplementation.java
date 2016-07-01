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

import io.netty.buffer.ByteBuf;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Schemas.AbsoluteMovementCommand;
import com.microsoft.Malmo.Schemas.AbsoluteMovementCommands;
import com.microsoft.Malmo.Schemas.MissionInit;

public class AbsoluteMovementCommandsImplementation extends CommandBase
{
    private boolean isOverriding = false;
    private boolean setX = false;
    private boolean setY = false;
    private boolean setZ = false;
    private boolean setYaw = false;
    private boolean setPitch = false;
    private float x;
    private float y;
    private float z;
    private float rotationYaw;
    private float rotationPitch;

    @Override
    public boolean parseParameters(Object params)
    {
        super.parseParameters(params);
        
    	if (params == null || !(params instanceof AbsoluteMovementCommands))
    		return false;

    	AbsoluteMovementCommands amparams = (AbsoluteMovementCommands)params;
    	setUpAllowAndDenyLists(amparams.getModifierList());
    	return true;
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev)
    {
    	if (ev.phase == Phase.END)
    		sendChanges();
    }

    private void sendChanges()
    {
    	EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
    	if (player == null)
    		return;

    	double x = this.setX ? this.x : 0;
        double y = this.setY ? this.y : 0;
        double z = this.setZ ? this.z : 0;
        float yaw = this.setYaw ? this.rotationYaw : 0;
        float pitch = this.setPitch ? this.rotationPitch : 0;

        // Send any changes requested over the wire to the server:
        if (this.setX || this.setY || this.setZ || this.setYaw || this.setPitch)
        {
	    	MalmoMod.network.sendToServer(new TeleportMessage(x, y, z, yaw, pitch, this.setX, this.setY, this.setZ, this.setYaw, this.setPitch));
            this.setX = this.setY = this.setZ = this.setYaw = this.setPitch = false;
        }
    }
    
    public static class TeleportMessage implements IMessage
    {
        private double x = 0;
        private double y = 0;
        private double z = 0;
        private float yaw = 0;
        private float pitch = 0;
        
        private boolean setX = false;
        private boolean setY = false;
        private boolean setZ = false;
        private boolean setYaw = false;
        private boolean setPitch = false;
        
    	public TeleportMessage()
    	{
    	}
    	
    	public TeleportMessage(double x, double y, double z, float yaw, float pitch, boolean setX, boolean setY, boolean setZ, boolean setYaw, boolean setPitch)
    	{
    		this.x = x;
    		this.y = y;
    		this.z = z;
    		this.yaw = yaw;
    		this.pitch = pitch;

    		this.setX = setX;
    		this.setY = setY;
    		this.setZ = setZ;
    		this.setYaw = setYaw;
    		this.setPitch = setPitch;
    	}
    	
		@Override
		public void fromBytes(ByteBuf buf)
		{
			this.x = buf.readDouble();
			this.y = buf.readDouble();
			this.z = buf.readDouble();
			this.yaw = buf.readFloat();
			this.pitch = buf.readFloat();

			this.setX = buf.readBoolean();
			this.setY = buf.readBoolean();
			this.setZ = buf.readBoolean();
			this.setYaw = buf.readBoolean();
			this.setPitch = buf.readBoolean();
		}

		@Override
		public void toBytes(ByteBuf buf)
		{
			buf.writeDouble(this.x);
			buf.writeDouble(this.y);
			buf.writeDouble(this.z);
			buf.writeFloat(this.yaw);
			buf.writeFloat(this.pitch);
			
			buf.writeBoolean(this.setX);
			buf.writeBoolean(this.setY);
			buf.writeBoolean(this.setZ);
			buf.writeBoolean(this.setYaw);
			buf.writeBoolean(this.setPitch);
		}
    }
    
    public static class TeleportMessageHandler implements IMessageHandler<TeleportMessage, IMessage>
    {
		@Override
		public IMessage onMessage(TeleportMessage message, MessageContext ctx)
		{
	        EnumSet<S08PacketPlayerPosLook.EnumFlags> enumset = EnumSet.noneOf(S08PacketPlayerPosLook.EnumFlags.class);
	        if (!message.setX)
	        	enumset.add(S08PacketPlayerPosLook.EnumFlags.X);
	        if (!message.setY)
	        	enumset.add(S08PacketPlayerPosLook.EnumFlags.Y);
	        if (!message.setZ)
	        	enumset.add(S08PacketPlayerPosLook.EnumFlags.Z);
	        if (!message.setYaw)
	        	enumset.add(S08PacketPlayerPosLook.EnumFlags.Y_ROT);
	        if (!message.setPitch)
	        	enumset.add(S08PacketPlayerPosLook.EnumFlags.X_ROT);

			EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            player.mountEntity((Entity)null);
            player.playerNetServerHandler.setPlayerLocation(message.x, message.y, message.z, message.yaw, message.pitch, enumset);
            player.setRotationYawHead(message.yaw);
            return null;
		}
    }
    
    @Override
    public boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        if (verb == null || verb.length() == 0)
        {
            return false;
        }
        
        // Now parse the command:
        if (verb.equalsIgnoreCase(AbsoluteMovementCommand.TPX.value()))
        {
            this.setX = true;
            this.x = Float.valueOf(parameter);
            return true;
        }
        else if (verb.equalsIgnoreCase(AbsoluteMovementCommand.TPY.value()))
        {
            this.setY = true;
            this.y = Float.valueOf(parameter);
            return true;
        }
        else if (verb.equalsIgnoreCase(AbsoluteMovementCommand.TPZ.value()))
        {
            this.setZ = true;
            this.z = Float.valueOf(parameter);
            return true;
        }
        else if (verb.equalsIgnoreCase(AbsoluteMovementCommand.TP.value()))
        {
            String[] coords = parameter.split(" ");
            if (coords.length != 3)
                return false;
            this.setX = this.setY = this.setZ = true;
            this.x = Float.valueOf(coords[0]);
            this.y = Float.valueOf(coords[1]);
            this.z = Float.valueOf(coords[2]);
            return true;
        }
        else if (verb.equalsIgnoreCase(AbsoluteMovementCommand.SET_YAW.value()))
        {
            this.setYaw = true;
            this.rotationYaw = Float.valueOf(parameter);
            return true;
        }
        else if (verb.equalsIgnoreCase(AbsoluteMovementCommand.SET_PITCH.value()))
        {
        	this.setPitch = true;
            this.rotationPitch = Float.valueOf(parameter);
            return true;
        }
        return false;
    }

    @Override
    public void install(MissionInit missionInit)
    {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void deinstall(MissionInit missionInit)
    {
        FMLCommonHandler.instance().bus().unregister(this);
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public boolean isOverriding()
    {
        return this.isOverriding;
    }

    @Override
    public void setOverriding(boolean b)
    {
        this.isOverriding = b;
    }
}
