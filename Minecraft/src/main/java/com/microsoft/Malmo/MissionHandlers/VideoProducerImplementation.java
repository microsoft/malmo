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

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glReadPixels;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.microsoft.Malmo.MissionHandlerInterfaces.IVideoProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.VideoProducer;

public class VideoProducerImplementation extends HandlerBase implements IVideoProducer
{
    private VideoProducer videoParams;
    private Framebuffer fbo;
    private FloatBuffer depthBuffer;

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof VideoProducer))
            return false;
        this.videoParams = (VideoProducer) params;

        return true;
    }

    @Override
    public VideoType getVideoType()
    {
        return VideoType.VIDEO;
    }

    @Override
    public void getFrame(MissionInit missionInit, ByteBuffer buffer)
    {
        if (!this.videoParams.isWantDepth())
        {
            getRGBFrame(buffer); // Just return the simple RGB, 3bpp image.
            return;
        }

        // Otherwise, do the work of extracting the depth map:
        final int width = this.videoParams.getWidth();
        final int height = this.videoParams.getHeight();

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Minecraft.getMinecraft().getFramebuffer().framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.fbo.framebufferObject);
        GL30.glBlitFramebuffer(0, 0, Minecraft.getMinecraft().getFramebuffer().framebufferWidth, Minecraft.getMinecraft().getFramebuffer().framebufferHeight, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);

        this.fbo.bindFramebuffer(true);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glReadPixels(0, 0, width, height, GL_DEPTH_COMPONENT, GL_FLOAT, this.depthBuffer);
        this.fbo.unbindFramebuffer();

        // Now convert the depth buffer into values from 0-255 and copy it over
        // the alpha channel.
        // We either use the min and max values supplied in order to scale it,
        // or we scale it according
        // to the dynamic content:
        float minval, maxval;

        // The scaling section is optional (since the depthmap is optional) - so
        // if there is no depthScaling object,
        // go with the default of autoscale.
        if (this.videoParams.getDepthScaling() == null || this.videoParams.getDepthScaling().isAutoscale())
        {
            minval = 1;
            maxval = 0;
            for (int i = 0; i < width * height; i++)
            {
                float f = this.depthBuffer.get(i);
                if (f < minval)
                    minval = f;
                if (f > maxval)
                    maxval = f;
            }
        }
        else
        {
            minval = this.videoParams.getDepthScaling().getMin().floatValue();
            maxval = this.videoParams.getDepthScaling().getMax().floatValue();
            if (minval > maxval)
            {
                // You can't trust users.
                float t = minval;
                minval = maxval;
                maxval = t;
            }
        }
        float range = maxval - minval;
        if (range < 0.000001)
            range = 0.000001f; // To avoid divide by zero errors in cases where
                               // there is no depth variance
        float scale = 255 / range;
        for (int i = 0; i < width * height; i++)
        {
            float f = this.depthBuffer.get(i);
            f = (f < minval ? minval : (f > maxval ? maxval : f));
            f -= minval;
            f *= scale;
            buffer.put(i * 4 + 3, (byte) f);
        }
        // Reset depth buffer ready for next read:
        this.depthBuffer.clear();
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
        return this.videoParams.getWidth() * this.videoParams.getHeight() * (this.videoParams.isWantDepth() ? 4 : 3);
    }

    private void getRGBFrame(ByteBuffer buffer)
    {
        final int format = GL_RGB;
        final int width = this.videoParams.getWidth();
        final int height = this.videoParams.getHeight();

        // Render the Minecraft frame into our own FBO, at the desired size:
        this.fbo.bindFramebuffer(true);
        Minecraft.getMinecraft().getFramebuffer().framebufferRenderExt(width, height, true);
        // Now read the pixels out from that:
        // glReadPixels appears to be faster than doing:
        // GlStateManager.bindTexture(this.fbo.framebufferTexture);
        // GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, format, GL_UNSIGNED_BYTE,
        // buffer);
        glReadPixels(0, 0, width, height, format, GL_UNSIGNED_BYTE, buffer);
        this.fbo.unbindFramebuffer();
        GlStateManager.enableDepth();
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        this.fbo = new Framebuffer(this.videoParams.getWidth(), this.videoParams.getHeight(), true);
        // Create a buffer for retrieving the depth map, if requested:
        if (this.videoParams.isWantDepth())
            this.depthBuffer = BufferUtils.createFloatBuffer(this.videoParams.getWidth() * this.videoParams.getHeight());
        // Set the requested camera position
        Minecraft.getMinecraft().gameSettings.thirdPersonView = this.videoParams.getViewpoint();
    }

    @Override
    public void cleanup()
    {
        this.fbo.deleteFramebuffer(); // Must do this or we leak resources.
    }
}
