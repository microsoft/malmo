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

package com.microsoft.Malmo.Utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.JsonException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.microsoft.Malmo.MalmoMod;

public class TextureHelper
{
    public static class MalmoRenderManager extends RenderManager
    {
        public MalmoRenderManager(TextureManager renderEngineIn, RenderItem itemRendererIn)
        {
            super(renderEngineIn, itemRendererIn);
        }
        @Override
        public void renderEntityStatic(Entity entityIn, float partialTicks, boolean p_188388_3_)
        {
            if (isProducingColourMap)
            {
                currentEntity = entityIn;
                super.renderEntityStatic(entityIn, partialTicks, p_188388_3_);
                currentEntity = null;
            }
            else
                super.renderEntityStatic(entityIn, partialTicks, p_188388_3_);
        }
    }

    public static class BlankSkyRenderer extends net.minecraftforge.client.IRenderHandler
    {
        private byte r = 0;
        private byte g = 0;
        private byte b = 0;

        public BlankSkyRenderer(byte[] col)
        {
            this.r = col[0];
            this.g = col[1];
            this.b = col[2];
        }

        public void render(float partialTicks, WorldClient world, Minecraft mc)
        {
            GlStateManager.disableFog();
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            VertexBuffer vertexbuffer = tessellator.getBuffer();

            for (int i = 0; i < 6; ++i)
            {
                GlStateManager.pushMatrix();

                if (i == 1)
                {
                    GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (i == 2)
                {
                    GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (i == 3)
                {
                    GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                }

                if (i == 4)
                {
                    GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                }

                if (i == 5)
                {
                    GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                }

                vertexbuffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
                vertexbuffer.pos(-100.0D, -100.0D, -100.0D).color(this.r, this.g, this.b, 255).endVertex();
                vertexbuffer.pos(-100.0D, -100.0D, 100.0D).color(this.r, this.g, this.b, 255).endVertex();
                vertexbuffer.pos(100.0D, -100.0D, 100.0D).color(this.r, this.g, this.b, 255).endVertex();
                vertexbuffer.pos(100.0D, -100.0D, -100.0D).color(this.r, this.g, this.b, 255).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }

            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
        }
    }

    private static Entity currentEntity = null;
    private static int shaderID = -1;
    private static boolean isInitialised;
    private static Map<String, Integer> idealMobColours = null;
    private static Map<Integer, Integer> texturesToColours = new HashMap<Integer, Integer>();
    private static Map<String, Integer> miscTexturesToColours = new HashMap<String, Integer>();

    private static boolean isProducingColourMap = false;

    public static void init()
    {
        if (!isInitialised)
        {
            shaderID = createProgram("annotate");
            isInitialised = true;
        }
    }

    public static void setIsProducingColourMap(boolean usemap)
    {
        if (usemap && !isInitialised)
            init();
        isProducingColourMap = usemap;
        if (!usemap)
            OpenGlHelper.glUseProgram(0);
    }

    public static void glBindTexture(int target, int texture)
    {
        if (isProducingColourMap)
        {
            if (shaderID != -1)
            {
                // Have we encountered this texture before?
                Integer col = texturesToColours.get(texture);
                if (col == null)
                {
                    // No - are we drawing an entity?
                    if (currentEntity != null)
                    {
                        // Has the user requested a specific mapping?
                        if (idealMobColours != null)
                        {
                            // Yes, in which case use black unless a mapping is found:
                            col = 0;
                            String entName = EntityList.getKey(currentEntity).toString();
                            for (String ent : idealMobColours.keySet())
                            {
                                if (entName.equals(ent))
                                {
                                    col = idealMobColours.get(ent);
                                }
                            }
                        }
                        else
                        {
                            // Provide a default mapping from entity to colour
                            String ent = EntityList.getEntityString(currentEntity);
                            col = (ent.hashCode()) % 0xffffff;
                        }
                        texturesToColours.put(texture, col);
                    }
                    else
                    {
                        // Not drawing an entity. Check the misc mappings:
                        for (String resID : miscTexturesToColours.keySet())
                        {
                            ITextureObject tex = Minecraft.getMinecraft().getTextureManager().getTexture(new ResourceLocation(resID));
                            if (tex != null && tex.getGlTextureId() == texture)
                            {
                                // Match
                                col = miscTexturesToColours.get(resID);
                            }
                        }
                        if (col == null)
                        {
                            // Still no match.
                            // Finally, see if this is the block atlas texture:
                            ITextureObject blockatlas = Minecraft.getMinecraft().getTextureManager().getTexture(new ResourceLocation("textures/atlas/blocks.png"));
                            if (blockatlas != null && blockatlas.getGlTextureId() == texture)
                            {
                                col = -1;
                            }
                        }
                        if (col != null)    // Put this in the map for easy access next time.
                            texturesToColours.put(texture, col);
                    }
                }
                if (col != null)
                {
                    OpenGlHelper.glUseProgram(shaderID);
                    int entColUniformLoc = OpenGlHelper.glGetUniformLocation(shaderID, "entityColour");
                    if (entColUniformLoc != -1)
                        OpenGlHelper.glUniform1i(entColUniformLoc, col);
                }
                else
                    OpenGlHelper.glUseProgram(0);
            }
        }
        GL11.glBindTexture(target, texture);
    }

    public static int loadShader(String filename, int shaderType) throws IOException
    {
        int shaderID = -1;
        InputStream stream = MalmoMod.class.getClassLoader().getResourceAsStream(filename);
        if (stream == null)
        {
            System.out.println("Cannot find shader.");
            return -1;
        }
        try
        {
            byte[] abyte = IOUtils.toByteArray((InputStream) (new BufferedInputStream(stream)));
            ByteBuffer bytebuffer = BufferUtils.createByteBuffer(abyte.length);
            bytebuffer.put(abyte);
            bytebuffer.position(0);
            shaderID = OpenGlHelper.glCreateShader(shaderType);
            OpenGlHelper.glShaderSource(shaderID, bytebuffer);
            OpenGlHelper.glCompileShader(shaderID);

            if (OpenGlHelper.glGetShaderi(shaderID, OpenGlHelper.GL_COMPILE_STATUS) == 0)
            {
                String s = StringUtils.trim(OpenGlHelper.glGetShaderInfoLog(shaderID, 32768));
                JsonException jsonexception = new JsonException("Couldn\'t compile shader program: " + s);
                throw jsonexception;
            }
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }
        return shaderID;
    }

    public static int createProgram(String shader)
    {
        int prog = -1;
        try
        {
            int f_shader = loadShader(shader + ".fsh", OpenGlHelper.GL_FRAGMENT_SHADER);
            int v_shader = loadShader(shader + ".vsh", OpenGlHelper.GL_VERTEX_SHADER);
            prog = OpenGlHelper.glCreateProgram();
            OpenGlHelper.glAttachShader(prog, v_shader);
            OpenGlHelper.glAttachShader(prog, f_shader);
            OpenGlHelper.glLinkProgram(prog);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return prog;
    }

    public static void setMobColours(Map<String, Integer> mobColours)
    {
        if (mobColours == null || mobColours.isEmpty())
            idealMobColours = null;
        else
        {
            idealMobColours = new HashMap<String, Integer>();
            // Convert the names from our XML entity type into recognisable entity names:
            String id = null;
            for (String oldname : mobColours.keySet())
            {
                for (EntityEntry ent : net.minecraftforge.fml.common.registry.ForgeRegistries.ENTITIES)
                {
                   if (ent.getName().equals(oldname))
                   {
                       id = ent.getRegistryName().toString();
                       break;
                   }
                }
                if (id != null)
                {
                    idealMobColours.put(id, mobColours.get(oldname));
                }
            }
        }
        texturesToColours.clear();
    }

    public static void setMiscTextureColours(Map<String, Integer> miscColours)
    {
        if (miscColours == null || miscColours.isEmpty())
            miscTexturesToColours = null;
        else
            miscTexturesToColours = miscColours;
        texturesToColours.clear();
    }
}
