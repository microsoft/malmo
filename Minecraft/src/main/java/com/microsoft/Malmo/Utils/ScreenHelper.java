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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.microsoft.Malmo.MalmoMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ScreenHelper
{
    private static class IngameGUIHook extends GuiIngameForge
    {
        public IngameGUIHook(Minecraft mc)
        {
            super(mc);
        }

        public void displayTitle(String title, String subTitle, int timeFadeIn, int displayTime, int timeFadeOut)
        {
            TitleChangeEvent event = new TitleChangeEvent(title, subTitle);
            MinecraftForge.EVENT_BUS.post(event);
            super.displayTitle(title, subTitle, timeFadeIn, displayTime, timeFadeOut);
        }
    }

    public static class TitleChangeEvent extends Event
    {
        public final String title;
        public final String subtitle;

        public TitleChangeEvent(String title, String subtitle)
        {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public static void hookIntoInGameGui()
    {
        Minecraft.getMinecraft().ingameGUI = new IngameGUIHook(Minecraft.getMinecraft());
    }

    public enum TextCategory
    {
        TXT_INFO,
        TXT_SERVER_STATE,
        TXT_CLIENT_STATE,
        TXT_SERVER_WARNING,
        TXT_CLIENT_WARNING,
        TXT_AGENT_MESSAGE
    }

    public enum DebugOutputLevel
    {
        OUTPUT_NONE("No display"),                  // Don't draw anything
        OUTPUT_FRIENDLY("Normal"),                  // Default - draw messages from the agent, etc
        OUTPUT_INFO("Basic info"),                  // Also draw basic info (eg Mission Control Port, etc)
        OUTPUT_WARNINGS("Show warnings"),           // Also draw warnings
        OUTPUT_DEBUG("Show debugging info"),        // Also draw state machine states
        OUTPUT_EVERYTHING("Show all diagnostics");  // And anything else

        private final String displayName;
        DebugOutputLevel(String displayName) { this.displayName = displayName; }
        public final String getDisplayName() { return this.displayName; }
    }

    public class TextFragment
    {
        public TextCategory category;
        public String text;
        public long expirationTime;
        public String handle = null;

        public TextFragment(String text, TextCategory category, Integer displayTimeMs)
        {
            this.text = (text != null) ? text : "";
            this.category = category;
            this.expirationTime = (displayTimeMs != null) ? System.currentTimeMillis() + displayTimeMs : -1;
        }
    }

    public class TextCategoryAttributes
    {
        public int xOrg;
        public int yOrg;
        public int colour;
        public boolean list;
        public boolean flashing;
        public DebugOutputLevel displayLevel;

        public TextCategoryAttributes(int xorg, int yorg, int colour, boolean list, boolean flashing, DebugOutputLevel displayLevel)
        {
            this.xOrg = xorg;
            this.yOrg = yorg;
            this.colour = colour;
            this.list = list;
            this.flashing = flashing;
            this.displayLevel = displayLevel;
        }
    }

    protected Map<TextCategory, ArrayList<TextFragment>> fragments = new HashMap<TextCategory, ArrayList<TextFragment>>();
    protected Map<TextCategory, TextCategoryAttributes> attributes = new HashMap<TextCategory, TextCategoryAttributes>();
    protected static DebugOutputLevel outputLevel = DebugOutputLevel.OUTPUT_FRIENDLY;

    public ScreenHelper()
    {
        MinecraftForge.EVENT_BUS.register(this);

        this.attributes.put(TextCategory.TXT_INFO, new TextCategoryAttributes(800, 850, 0x4488ff, true, false, DebugOutputLevel.OUTPUT_INFO));
        this.attributes.put(TextCategory.TXT_SERVER_STATE, new TextCategoryAttributes(4, 4, 0xffaaff, false, false, DebugOutputLevel.OUTPUT_DEBUG));
        this.attributes.put(TextCategory.TXT_CLIENT_STATE, new TextCategoryAttributes(4, 34, 0xaaffff, false, false, DebugOutputLevel.OUTPUT_DEBUG));
        this.attributes.put(TextCategory.TXT_CLIENT_WARNING, new TextCategoryAttributes(4, 64, 0xff0000, true, true, DebugOutputLevel.OUTPUT_WARNINGS));
        this.attributes.put(TextCategory.TXT_SERVER_WARNING, new TextCategoryAttributes(500, 64, 0xff0000, true, true, DebugOutputLevel.OUTPUT_WARNINGS));
        this.attributes.put(TextCategory.TXT_AGENT_MESSAGE, new TextCategoryAttributes(4, 800, 0x8888ff, true, false, DebugOutputLevel.OUTPUT_FRIENDLY));
    }

    public static void setOutputLevel(DebugOutputLevel dol)
    {
        ScreenHelper.outputLevel = dol;
    }

    public void addFragment(String text, TextCategory category, Integer displayTimeMs)
    {
        TextFragment fragment = new TextFragment(text, category, displayTimeMs);
        addFragment(fragment);
    }

    public void addFragment(String text, TextCategory category, String handle)
    {
        TextFragment fragment = new TextFragment(text, category, null);
        fragment.handle = handle;
        addFragment(fragment);
    }

    public void clearFragment(String handle)
    {
        purgeExpiredFragments(handle);
    }

    protected void addFragment(TextFragment fragment)
    {
        synchronized (this.fragments)
        {
            if (fragment == null)
                return;
            TextCategoryAttributes atts = this.attributes.get(fragment.category);
            if (atts == null)
                return; // Error!

            ArrayList<TextFragment> frags = this.fragments.get(fragment.category);
            if (frags == null)
            {
                frags = new ArrayList<TextFragment>();
                this.fragments.put(fragment.category, frags);
            }
            if (!atts.list)
                frags.clear();
            boolean replaced = false;
            if (fragment.handle != null)
            {
                // Look for a fragment with this handle, and replace it if found:
                for (int i = 0; i < frags.size() && !replaced; i++)
                {
                    if (frags.get(i).handle != null && frags.get(i).handle.equals(fragment.handle))
                    {
                        frags.set(i, fragment);
                        replaced = true;
                    }
                }
            }
            if (!replaced)
                frags.add(fragment);
        }
    }

    private void purgeExpiredFragments(String handle)
    {
        synchronized (this.fragments)
        {
            long timenow = System.currentTimeMillis();
            Iterator<Entry<TextCategory, ArrayList<TextFragment>>> itCat = this.fragments.entrySet().iterator();
            while (itCat.hasNext())
            {
                Map.Entry<TextCategory, ArrayList<TextFragment>> pair = (Map.Entry<TextCategory, ArrayList<TextFragment>>) itCat.next();
                if (pair.getValue() != null)
                {
                    Iterator<TextFragment> itFrag = pair.getValue().iterator();
                    while (itFrag.hasNext())
                    {
                        TextFragment frag = itFrag.next();
                        if ((frag.expirationTime != -1 && frag.expirationTime < timenow) || (handle != null && frag.handle != null && frag.handle.equals(handle)))
                            itFrag.remove();
                    }
                }
            }
        }
    }

    private boolean shouldDisplay(DebugOutputLevel dol)
    {
        return (dol.ordinal() <= ScreenHelper.outputLevel.ordinal());
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev)
    {
        purgeExpiredFragments(null);
        if (Minecraft.getMinecraft().currentScreen != null && !(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu))
            return;
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo)    // Don't obscure MC debug info with our debug info
            return;

        ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();
        float rx = (float) width / 1000f;
        float ry = (float) height / 1000f;

        synchronized(this.fragments)
        {
            for (TextCategory cat : TextCategory.values())
            {
                TextCategoryAttributes atts = this.attributes.get(cat);
                if (atts != null && (!atts.flashing || ((System.currentTimeMillis() / 500) % 3 != 0)) && shouldDisplay(atts.displayLevel))
                {
                    int x = Math.round(rx * (float) atts.xOrg);
                    int y = Math.round(ry * (float) atts.yOrg);
                    ArrayList<TextFragment> frags = this.fragments.get(cat);
                    if (frags != null && !frags.isEmpty())
                    {
                        for (TextFragment frag : frags)
                        {
                            drawText(frag.text, x, y, atts.colour);
                            y += 10;
                        }
                    }
                }
            }
        }
    }

    public static void drawText(String s, int x, int y, int colour)
    {
        FontRenderer frendo = Minecraft.getMinecraft().fontRendererObj;
        frendo.drawStringWithShadow(s, x, y, colour);
    }

    public static void update(Configuration config)
    {
        String[] values = new String[DebugOutputLevel.values().length];
        for (DebugOutputLevel level : DebugOutputLevel.values())
            values[level.ordinal()] = level.getDisplayName();
        String debugOutputLevel = config.getString("debugDisplayLevel", MalmoMod.DIAGNOSTIC_CONFIGS, ScreenHelper.outputLevel.getDisplayName(), "Set the level of debugging information to be displayed on the Minecraft screen.", values);
        for (DebugOutputLevel level : DebugOutputLevel.values())
            if (level.getDisplayName().equals(debugOutputLevel))
                ScreenHelper.outputLevel = level;
    }
}
