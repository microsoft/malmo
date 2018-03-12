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
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.util.JsonException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.common.registry.EntityEntry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.microsoft.Malmo.MalmoMod;

//Helper methods, classes etc which allow us to subvert the Minecraft render pipeline to produce
//a colourmap image in addition to the normal Minecraft image.
public class TextureHelper
{
    // Extend EntityRenderer to give us a relatively clean way to render multiple passes.
    public static class MalmoEntityRenderer extends EntityRenderer
    {
        public MalmoEntityRenderer(Minecraft mcIn, IResourceManager resourceManagerIn)
        {
            super(mcIn, resourceManagerIn);
        }

        @Override
        public void renderWorld(float partialTicks, long finishTimeNano)
        {
            if (isProducingColourMap)
            {
                // Creating a colourmap requires a completely separate pass through the render pipeline
                colourmapFrame = true;
                // Set the sky renderer to produce a solid block of colour:
                Minecraft.getMinecraft().world.provider.setSkyRenderer(blankSkyRenderer);
                // Render the world:
                super.renderWorld(partialTicks, finishTimeNano);
                // Now reset the sky renderer to default:
                Minecraft.getMinecraft().world.provider.setSkyRenderer(null);
                colourmapFrame = false;
                // And get the render pipeline ready to go again:
                OpenGlHelper.glUseProgram(0);
                GlStateManager.clear(16640);
                Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
                GlStateManager.enableTexture2D();
            }
            // Normal render:
            super.renderWorld(partialTicks, finishTimeNano);
        }
    }

    // Extended the RenderManager so that we can keep track of the entity currently being rendered.
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

    // Sky renderer for use with colourmap video output - fills sky with solid colour.
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
            // Adapted from the End sky renderer - just fill with solid colour.
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
    private static IRenderHandler blankSkyRenderer;

    private static boolean isProducingColourMap = false;
    public static boolean colourmapFrame = false;

    public static void hookIntoRenderPipeline()
    {
        // Subvert the render manager. This MUST be called at the right time (FMLInitializationEvent).
        // 1: Replace the MC entity renderer with our own:
        Minecraft.getMinecraft().entityRenderer = new MalmoEntityRenderer(Minecraft.getMinecraft(), Minecraft.getMinecraft().getResourceManager());
        // 2: Create a new RenderManager:
        RenderManager newRenderManager = new TextureHelper.MalmoRenderManager(Minecraft.getMinecraft().renderEngine, Minecraft.getMinecraft().getRenderItem());
        // 3: Use reflection to:
        //      a) replace Minecraft's RenderManager with our new RenderManager
        //      b) point Minecraft's RenderGlobal object to the new RenderManager

        // Are we in the dev environment or deployed?
        boolean devEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
        // We need to know, because the names will either be obfuscated or not.
        String mcRenderManagerName = devEnv ? "renderManager" : "field_175616_W";
        String globalRenderManagerName = devEnv ? "renderManager" : "field_175010_j";
        // NOTE: obfuscated name may need updating if Forge changes - search in
        // ~\.gradle\caches\minecraft\de\oceanlabs\mcp\mcp_snapshot\20161220\1.11.2\srgs\mcp-srg.srg
        Field renderMan;
        Field globalRenderMan;
        try
        {
            renderMan = Minecraft.class.getDeclaredField(mcRenderManagerName);
            renderMan.setAccessible(true);
            renderMan.set(Minecraft.getMinecraft(), newRenderManager);

            globalRenderMan = RenderGlobal.class.getDeclaredField(globalRenderManagerName);
            globalRenderMan.setAccessible(true);
            globalRenderMan.set(Minecraft.getMinecraft().renderGlobal, newRenderManager);
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
    }

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
        // The Minecraft render code is selecting a texture.
        // If we are producing a colour map, this is our opportunity to activate our special fragment shader,
        // which will ignore the texture and use a solid colour - either the colour we pass in, or a colour based
        // on the texture coords (if using the block texture atlas).
        if (isProducingColourMap && colourmapFrame)
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
                            if (ent == null)    // Happens if, for example, currentEntity is of type EntityOtherPlayerMP.
                                ent = currentEntity.getClass().getName();
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
                    int entColUniformLocR = OpenGlHelper.glGetUniformLocation(shaderID, "entityColourR");
                    int entColUniformLocG = OpenGlHelper.glGetUniformLocation(shaderID, "entityColourG");
                    int entColUniformLocB = OpenGlHelper.glGetUniformLocation(shaderID, "entityColourB");
                    if (entColUniformLocR != -1 && entColUniformLocG != -1 && entColUniformLocB != -1)
                    {
                        OpenGlHelper.glUniform1i(entColUniformLocR, col != -1 ? (col >> 16) & 0xff : -1);
                        OpenGlHelper.glUniform1i(entColUniformLocG, col != -1 ? (col >> 8) & 0xff : -1);
                        OpenGlHelper.glUniform1i(entColUniformLocB, col != -1 ? col & 0xff : -1);
                    }
                }
                else
                    OpenGlHelper.glUseProgram(0);
            }
        }
        // Finally, pass call on to OpenGL:
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
            e.printStackTrace();
        }
        return prog;
    }

    public static void setSkyRenderer(IRenderHandler skyRenderer)
    {
        blankSkyRenderer = skyRenderer;
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
