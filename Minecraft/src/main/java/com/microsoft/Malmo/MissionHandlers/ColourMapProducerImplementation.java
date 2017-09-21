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

import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glReadPixels;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.common.MinecraftForge;

import com.microsoft.Malmo.MissionHandlerInterfaces.IVideoProducer;
import com.microsoft.Malmo.Schemas.ColourMapProducer;
import com.microsoft.Malmo.Schemas.EntityTypes;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.MobWithColour;
import com.microsoft.Malmo.Utils.TextureHelper;

public class ColourMapProducerImplementation extends HandlerBase implements IVideoProducer
{
    private ColourMapProducer cmParams;
    private Framebuffer fbo;
    private Map<String, Integer> mobColours = new HashMap<String, Integer>();
    private Map<String, Integer> miscColours = new HashMap<String, Integer>();

    @Override
    public boolean parseParameters(Object params)
    {
        MinecraftForge.EVENT_BUS.register(this);

        if (params == null || !(params instanceof ColourMapProducer))
            return false;
        this.cmParams = (ColourMapProducer)params;
        for (MobWithColour mob : this.cmParams.getColourSpec())
        {
            byte[] col = mob.getColour();
            int c = (col[2] & 0xff) + ((col[1] & 0xff) << 8) + ((col[0] & 0xff) << 16);
            for (EntityTypes ent : mob.getType())
            {
                String mobName = ent.value();
                this.mobColours.put(mobName, c);
            }
        }
        miscColours.put("textures/environment/sun.png", 0xffff00);
        miscColours.put("textures/environment/moon_phases.png", 0xffffff);
        return true;
    }

    @Override
    public VideoType getVideoType()
    {
        return VideoType.COLOUR_MAP;
    }

    @Override
    public int getWidth()
    {
        return this.cmParams.getWidth();
    }

    @Override
    public int getHeight()
    {
        return this.cmParams.getHeight();
    }

    public int getRequiredBufferSize()
    {
        return this.getWidth() * this.getHeight() * 3;
    }

    @Override
    public void getFrame(MissionInit missionInit, ByteBuffer buffer)
    {
        // All the complicated work is done inside TextureHelper - by this point, all we need do
        // is grab the contents of the Minecraft framebuffer.
        final int width = getWidth();
        final int height = getHeight();

        // Render the Minecraft frame into our own FBO, at the desired size:
        this.fbo.bindFramebuffer(true);
        Minecraft.getMinecraft().getFramebuffer().framebufferRenderExt(width, height, true);
        glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, buffer);
        this.fbo.unbindFramebuffer();
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        this.fbo = new Framebuffer(this.getWidth(), this.getHeight(), true);
        TextureHelper.setIsProducingColourMap(true);
        TextureHelper.setMobColours(this.mobColours);
        TextureHelper.setMiscTextureColours(this.miscColours);
        TextureHelper.setSkyRenderer(new TextureHelper.BlankSkyRenderer(this.cmParams.getSkyColour()));
    }

    @Override
    public void cleanup()
    {
        TextureHelper.setIsProducingColourMap(false);
        this.fbo.deleteFramebuffer(); // Must do this or we leak resources.
    }
}
