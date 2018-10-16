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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

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

        AbsoluteMovementCommands amparams = (AbsoluteMovementCommands) params;
        setUpAllowAndDenyLists(amparams.getModifierList());
        return true;
    }

    private void sendChanges()
    {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null)
            return;

        // Send any changes requested over the wire to the server:
        double x = this.setX ? this.x : 0;
        double y = this.setY ? this.y : 0;
        double z = this.setZ ? this.z : 0;
        float yaw = this.setYaw ? this.rotationYaw : 0;
        float pitch = this.setPitch ? this.rotationPitch : 0;

        if (this.setX || this.setY || this.setZ || this.setYaw || this.setPitch)
        {
            MalmoMod.network.sendToServer(new TeleportMessage(x, y, z, yaw, pitch, this.setX, this.setY, this.setZ, this.setYaw, this.setPitch));
            if (this.setYaw || this.setPitch)
            {
                // Send a message that the ContinuousMovementCommands can pick up on:
                Event event = new CommandForWheeledRobotNavigationImplementation.ResetPitchAndYawEvent(this.setYaw, this.rotationYaw, this.setPitch, this.rotationPitch);
                MinecraftForge.EVENT_BUS.post(event);
            }
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
        public IMessage onMessage(final TeleportMessage message, final MessageContext ctx)
        {
            // Don't act here - if we cause chunk loading on this thread (netty) then chunks will get
            // lost from the server.
            IThreadListener mainThread = null;
            if (ctx.side == Side.CLIENT)
                mainThread = Minecraft.getMinecraft();
            else
                mainThread = (WorldServer)ctx.getServerHandler().playerEntity.world;
            mainThread.addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    EnumSet<SPacketPlayerPosLook.EnumFlags> enumset = EnumSet.noneOf(SPacketPlayerPosLook.EnumFlags.class);
                    if (!message.setX)
                        enumset.add(SPacketPlayerPosLook.EnumFlags.X);
                    if (!message.setY)
                        enumset.add(SPacketPlayerPosLook.EnumFlags.Y);
                    if (!message.setZ)
                        enumset.add(SPacketPlayerPosLook.EnumFlags.Z);
                    if (!message.setYaw)
                        enumset.add(SPacketPlayerPosLook.EnumFlags.Y_ROT);
                    if (!message.setPitch)
                        enumset.add(SPacketPlayerPosLook.EnumFlags.X_ROT);

                    EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                    player.dismountRidingEntity();
                    player.connection.setPlayerLocation(message.x, message.y, message.z, message.yaw, message.pitch, enumset);
                    player.setRotationYawHead(message.yaw);
                }
            });
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
            sendChanges();
            return true;
        }
        else if (verb.equalsIgnoreCase(AbsoluteMovementCommand.TPY.value()))
        {
            this.setY = true;
            this.y = Float.valueOf(parameter);
            sendChanges();
            return true;
        }
        else if (verb.equalsIgnoreCase(AbsoluteMovementCommand.TPZ.value()))
        {
            this.setZ = true;
            this.z = Float.valueOf(parameter);
            sendChanges();
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
            sendChanges();
            return true;
        }
        else if (verb.equalsIgnoreCase(AbsoluteMovementCommand.SET_YAW.value()))
        {
            this.setYaw = true;
            this.rotationYaw = Float.valueOf(parameter);
            sendChanges();
            return true;
        }
        else if (verb.equalsIgnoreCase(AbsoluteMovementCommand.SET_PITCH.value()))
        {
            this.setPitch = true;
            this.rotationPitch = Float.valueOf(parameter);
            sendChanges();
            return true;
        }
        return false;
    }

    @Override
    public void install(MissionInit missionInit)
    {
    }

    @Override
    public void deinstall(MissionInit missionInit)
    {
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
