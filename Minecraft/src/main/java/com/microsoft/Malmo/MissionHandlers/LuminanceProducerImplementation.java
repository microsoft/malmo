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

import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

import java.nio.ByteBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.microsoft.Malmo.MissionHandlerInterfaces.IVideoProducer;
import com.microsoft.Malmo.Schemas.LuminanceProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.TextureHelper;

public class LuminanceProducerImplementation extends HandlerBase implements IVideoProducer
{
    private LuminanceProducer lumParams;
    private Framebuffer fbo;
    static private int shaderID = -1;

    @Override
    public boolean parseParameters(Object params)
    {
        MinecraftForge.EVENT_BUS.register(this);

        if (params == null || !(params instanceof LuminanceProducer))
            return false;
        this.lumParams = (LuminanceProducer) params;

        if (shaderID == -1)
            shaderID = TextureHelper.createProgram("lum");
        return true;
    }

    @Override
    public VideoType getVideoType()
    {
        return VideoType.LUMINANCE;
    }

    @Override
    public int getWidth()
    {
        return this.lumParams.getWidth();
    }

    @Override
    public int getHeight()
    {
        return this.lumParams.getHeight();
    }

    public int getRequiredBufferSize()
    {
        return this.getWidth() * this.getHeight();
    }

    @Override
    public void getFrame(MissionInit missionInit, ByteBuffer buffer)
    {
        final int width = getWidth();
        final int height = getHeight();

        // Render the Minecraft frame into our own FBO, at the desired size:
        OpenGlHelper.glUseProgram(shaderID);
        this.fbo.bindFramebuffer(true);
        Minecraft.getMinecraft().getFramebuffer().framebufferRenderExt(width, height, true);
        GlStateManager.bindTexture(this.fbo.framebufferTexture);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL_RED, GL_UNSIGNED_BYTE, buffer);
        this.fbo.unbindFramebuffer();
        OpenGlHelper.glUseProgram(0);
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        this.fbo = new Framebuffer(this.getWidth(), this.getHeight(), true);
    }

    @Override
    public void cleanup()
    {
        this.fbo.deleteFramebuffer(); // Must do this or we leak resources.
    }
}
