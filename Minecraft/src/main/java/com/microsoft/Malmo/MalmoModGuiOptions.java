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

package com.microsoft.Malmo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import com.microsoft.Malmo.Utils.AddressHelper;

public class MalmoModGuiOptions implements IModGuiFactory
{
    public static class MalmoModGuiScreen extends GuiConfig
    {
        public MalmoModGuiScreen(GuiScreen parentScreen)
        {
            super(parentScreen, getConfigElements(), MalmoMod.MODID, MalmoMod.SOCKET_CONFIGS, false, false, "Malmo Platform Settings", MalmoMod.instance.getModPermanentConfigFile().getConfigFile().getParent());
        }

        static private List<IConfigElement> getConfigElements()
        {
            ConfigCategory cat = MalmoMod.instance.getModSessionConfigFile().getCategory(MalmoMod.SOCKET_CONFIGS);
            List<IConfigElement> list = new ArrayList<IConfigElement>();
            for (Property prop : cat.getOrderedValues())
            {
                list.add(new ConfigElement(prop));
            }
            ConfigCategory catDiags = MalmoMod.instance.getModPermanentConfigFile().getCategory(MalmoMod.DIAGNOSTIC_CONFIGS);
            for (Property prop : catDiags.getOrderedValues())
            {
                list.add(new ConfigElement(prop));
            }
            return list;
        }

        @Override
        public void initGui()
        {
            // You can add buttons and initialize fields here
            super.initGui();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks)
        {
            super.drawScreen(mouseX, mouseY, partialTicks);
            if (AddressHelper.getMissionControlPort() != -1)
                this.drawCenteredString(this.fontRendererObj, "Mission Control Port: " + AddressHelper.getMissionControlPort(), this.width / 2, this.height / 2, 0x44ff44);
            else
            {
                this.drawCenteredString(this.fontRendererObj, "NO MISSION CONTROL SOCKET - is there a port collision?", this.width / 2, this.height / 2, 0xff0000);
                this.drawCenteredString(this.fontRendererObj, "Set the portOverride to 0 to let the system allocate a free port dynamically.", this.width / 2, this.height / 2 + this.fontRendererObj.FONT_HEIGHT, 0xffffff);
            }
        }

        @Override
        protected void actionPerformed(GuiButton button)
        {
            // You can process any additional buttons you may have added here
            super.actionPerformed(button);
        }

        public GuiButtonExt getDoneButton()
        {
            for (Object butobj : this.buttonList)
            {
                if (butobj instanceof GuiButtonExt)
                {
                    GuiButtonExt button = (GuiButtonExt)butobj;
                    if (button.id == 2000)
                        return button;
                }
            }
            return null;
        }

        @Override
        public void onGuiClosed()
        {
            super.onGuiClosed();
            // Save any changes to our configuration:
            MalmoMod.instance.getModPermanentConfigFile().save();
            MalmoMod.instance.getModSessionConfigFile().save();
        }
    }

    @Override
    public void initialize(Minecraft minecraftInstance)
    {
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass()
    {
        return MalmoModGuiScreen.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories()
    {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element)
    {
        return null;
    }
}
