package com.microsoft.Malmo.MissionHandlers;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.glReadPixels;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.MathHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.microsoft.Malmo.MissionHandlerInterfaces.IVideoProducer;
import com.microsoft.Malmo.Schemas.DepthProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.VideoProducer;

public class DepthProducerImplementation extends HandlerBase implements IVideoProducer
{
    private DepthProducer videoParams;
    private Framebuffer fbo;

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof DepthProducer))
            return false;
        this.videoParams = (DepthProducer)params;
        return true;
    }

    @Override
    public VideoType getVideoType()
    {
        return VideoType.DEPTH_MAP;
    }

    @Override
    public void getFrame(MissionInit missionInit, ByteBuffer buffer)
    {
        final int width = this.videoParams.getWidth();
        final int height = this.videoParams.getHeight();

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Minecraft.getMinecraft().getFramebuffer().framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.fbo.framebufferObject );
        GL30.glBlitFramebuffer(
                0, 0,
                Minecraft.getMinecraft().getFramebuffer().framebufferWidth,
                Minecraft.getMinecraft().getFramebuffer().framebufferHeight,
                0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST );

        this.fbo.bindFramebuffer(true);
        glReadPixels(0, 0, width, height, GL_DEPTH_COMPONENT, GL_FLOAT, buffer.asFloatBuffer());
        FloatBuffer fds = BufferUtils.createFloatBuffer(16);
        //GL11.glGetFloat(GL11.GL_DEPTH_BIAS, fds);
        GL11.glGetFloat(GL11.GL_DEPTH_RANGE, fds);
        FloatBuffer fluffer = buffer.asFloatBuffer();
        //float ffff = fds.get(0);
        //System.out.println(ffff);
        System.out.println("Near: " + fds.get(0) + " far: " + fds.get(1));
        float zNear = fds.get(0);
        float zFar = fds.get(1);
        zNear = 0.05F;
        zFar = 128.0F * MathHelper.SQRT_2;
        float minval = 1;
        float maxval = 0;
        for (int i = 0; i < width * height; i++)
        {
            float f = fluffer.get(i);
            if (f < minval)
                minval = f;
            if (f > maxval)
                maxval = f;
            f = 2.0f * f - 1.0f;
            float zLinear = 2.0f * zNear * zFar / (zFar + zNear - f * (zFar - zNear));
            fluffer.put(i, zLinear);
        }
        System.out.println("Range: " + minval + " - " + maxval);
        this.fbo.unbindFramebuffer();
    }

    @Override
    public int getWidth()
    {
        return this.videoParams.getWidth();
    }

    @Override
    public int getHeight()
    {
        return this.videoParams.getHeight();
    }

    public int getRequiredBufferSize()
    {
        return this.videoParams.getWidth() * this.videoParams.getHeight() * 4;
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        this.fbo = new Framebuffer(this.videoParams.getWidth(), this.videoParams.getHeight(), true);
        // Set the requested camera position
        // Minecraft.getMinecraft().gameSettings.thirdPersonView = this.videoParams.getViewpoint();
    }

    @Override
    public void cleanup()
    {
        this.fbo.deleteFramebuffer();   // Must do this or we leak resources.
    }
}