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

package com.microsoft.Malmo.Client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IVideoProducer;
import com.microsoft.Malmo.Schemas.ClientAgentConnection;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.TCPSocketHelper;

/**
 * Register this class on the MinecraftForge.EVENT_BUS to intercept video
 * frames.
 * <p>
 * We use this to send video frames over sockets.
 */
public class VideoHook {
    /**
     * If the sockets are not yet open we delay before retrying. Value is in
     * nanoseconds.
     */
    private static final long RETRY_GAP_NS = 5000000000L;

    /**
     * The time in nanoseconds after which we should try sending again.
     */
    private long retry_time_ns = 0;

    /**
     * Calling stop() if we're not running is a no-op.
     */
    private boolean isRunning = false;

    /**
     * MissionInit object for passing to the IVideoProducer.
     */
    private MissionInit missionInit;

    /**
     * Object that will provide the actual video frame on demand.
     */
    private IVideoProducer videoProducer;

    /**
     * Public count of consecutive TCP failures - used to terminate a mission if nothing is listening
     */
    public int failedTCPSendCount = 0;

    /**
     * Object which maintains our connection to the agent.
     */
    private TCPSocketHelper.SocketChannelHelper connection = null;
    
    private int renderWidth;
    
    private int renderHeight;

    ByteBuffer buffer = null;
    ByteBuffer headerbuffer = null;
    final int POS_HEADER_SIZE = 20; // 20 bytes for the five floats governing x,y,z,yaw and pitch.
    /**
     * Resize the rendering and start sending video over TCP.
     */
    public void start(MissionInit missionInit, IVideoProducer videoProducer)
    {
        if (videoProducer == null)
        {
            return; // Don't start up if there is nothing to provide the video.
        }

        videoProducer.prepare(missionInit);
        this.missionInit = missionInit;
        this.videoProducer = videoProducer;
        this.buffer = BufferUtils.createByteBuffer(this.videoProducer.getRequiredBufferSize());
        this.headerbuffer = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        this.renderWidth = videoProducer.getWidth(missionInit);
        this.renderHeight = videoProducer.getHeight(missionInit);
        resizeIfNeeded();
        Display.setResizable(false); // prevent the user from resizing using the window borders

        ClientAgentConnection cac = missionInit.getClientAgentConnection();
        if (cac == null)
            return;	// Don't start up if we don't have any connection details.

        String agentIPAddress = cac.getAgentIPAddress();
        int agentPort = cac.getAgentVideoPort();

        this.connection = new TCPSocketHelper.SocketChannelHelper(agentIPAddress, agentPort);
        this.failedTCPSendCount = 0;

        try
        {
            MinecraftForge.EVENT_BUS.register(this);
            FMLCommonHandler.instance().bus().register(this); 
        }
        catch(Exception e)
        {
            System.out.println("Failed to register video hook: " + e);
        }
        this.isRunning = true;
    }
    
    /**
     * Resizes the window and the Minecraft rendering if necessary. Set renderWidth and renderHeight first.
     */
    private void resizeIfNeeded()
    {
        // resize the window if we need to
        int oldRenderWidth = Display.getWidth(); 
        int oldRenderHeight = Display.getHeight();
        if( this.renderWidth == oldRenderWidth && this.renderHeight == oldRenderHeight )
            return;
        
        try {
            Display.setDisplayMode(new DisplayMode(this.renderWidth, this.renderHeight));
            System.out.println("Resized the window");
        } catch (LWJGLException e) {
            System.out.println("Failed to resize the window!");
            e.printStackTrace();
        }
        forceResize(this.renderWidth, this.renderHeight);
    }

    /**
     * Stop sending video.
     */
    public void stop()
    {
        if( !this.isRunning )
        {
            return;
        }
        if (this.videoProducer != null)
            this.videoProducer.cleanup();

        // stop sending video frames
        try
        {
            MinecraftForge.EVENT_BUS.unregister(this);
            FMLCommonHandler.instance().bus().unregister(this); 
        }
        catch(Exception e)
        {
            System.out.println("Failed to unregister video hook: " + e);
        }
        // Close our TCP socket:
        this.connection.close();
        this.isRunning = false;

        // allow the user to resize the window again
        Display.setResizable(true);
    }
    
    /**
     * Called before and after the rendering of the world.
     * 
     * @param event
     *            Contains information about the event.
     */
    @SubscribeEvent
    public void onRender(RenderTickEvent event)
    {
        if( event.phase == Phase.START )
        {
            // this is here in case the user has resized the window during a mission
            resizeIfNeeded();
        }
    }
    
    /**
     * Called when the world has been rendered but not yet the GUI or player hand.
     * 
     * @param event
     *            Contains information about the event (not used).
     */
    @SubscribeEvent
    public void postRender(RenderWorldLastEvent event)
    {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        float x = (float) (player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks);
        float y = (float) (player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks);
        float z = (float) (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks);
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * event.partialTicks;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * event.partialTicks;

        long time_before_ns = System.nanoTime();

        if (time_before_ns < retry_time_ns)
            return;

        boolean success = false;

        try
        {
            int size = this.videoProducer.getRequiredBufferSize();
            // Get buffer ready for writing to:
            this.buffer.clear();
            this.headerbuffer.clear();
            // Write the pos data:
            this.headerbuffer.putFloat(x);
            this.headerbuffer.putFloat(y);
            this.headerbuffer.putFloat(z);
            this.headerbuffer.putFloat(yaw);
            this.headerbuffer.putFloat(pitch);
            // Write the frame data:
            this.videoProducer.getFrame(this.missionInit, this.buffer);
            // The buffer gets flipped by getFrame(), but we need to flip our header buffer ourselves:
            this.headerbuffer.flip();
            ByteBuffer[] buffers = {this.headerbuffer, this.buffer};

            long time_after_render_ns = System.nanoTime();
            success = this.connection.sendTCPBytes(buffers, size + POS_HEADER_SIZE);
            long time_after_ns = System.nanoTime();
            float ms_send = (time_after_ns - time_after_render_ns) / 1000000.0f;
            float ms_render = (time_after_render_ns - time_before_ns) / 1000000.0f;
            if (success)
                this.failedTCPSendCount = 0;    // Reset count of failed sends.
            //            System.out.format("Total: %.2fms; collecting took %.2fms; sending %d bytes took %.2fms\n", ms_send + ms_render, ms_render, size, ms_send);
            //            System.out.println("Collect: " + ms_render + "; Send: " + ms_send);
        }
        catch (Exception e)
        {
            System.out.format(e.getMessage());
        }
        
        if (!success)
        {
            System.out.format("Failed to send frame - will retry in %d seconds\n", RETRY_GAP_NS / 1000000000L);
            retry_time_ns = time_before_ns + RETRY_GAP_NS;
            this.failedTCPSendCount++;
        }
    }

    /** Force Minecraft to resize its GUI
     * @param width new width of window
     * @param height new height of window
     */
    private void forceResize(int width, int height)
    {
        // Are we in the dev environment or deployed?
        boolean devEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
        // We need to know, because the method name will either be obfuscated or not.
        String resizeMethodName = devEnv ? "resize" : "func_71370_a";

        Class[] cArgs = new Class[2];
        cArgs[0] = int.class;
        cArgs[1] = int.class;
        Method resize;
        try
        {
            resize = Minecraft.class.getDeclaredMethod(resizeMethodName, cArgs);
            resize.setAccessible(true);
            resize.invoke(Minecraft.getMinecraft(), width, height);
        }
        catch (NoSuchMethodException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SecurityException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
