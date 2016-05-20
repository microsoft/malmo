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
import com.microsoft.Malmo.Utils.AuthenticationHelper;

public class MalmoModGuiOptions implements IModGuiFactory
{
    public static class MalmoModGuiScreen extends GuiConfig
    {
        protected GuiButton loginButton;

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

        @SuppressWarnings("unchecked")	// Needed for the buttonList.add call - Minecraft code declares buttonList as unparameterised.
        @Override
        public void initGui()
        {
            // You can add buttons and initialize fields here
            super.initGui();
            this.buttonList.add(this.loginButton = new GuiButton(10, this.width / 8, this.height / 3, 128, 20, "Logins"));
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
            if (AuthenticationHelper.isAuthenticated)
                this.drawCenteredString(this.fontRendererObj, "Authenticated for multi-agent missions using " + Minecraft.getMinecraft().getSession().getUsername(), this.width / 2, this.fontRendererObj.FONT_HEIGHT * 3 + this.height / 2, 0xcccc22);
            else
            {
                this.drawCenteredString(this.fontRendererObj, "Not logged in - multi-agents missions will not be possible.", this.width / 2, this.fontRendererObj.FONT_HEIGHT * 3 + this.height / 2, 0xff5522);
                this.drawCenteredString(this.fontRendererObj, "Check the logins screen for valid usernames/passwords/port mappings.", this.width / 2, this.fontRendererObj.FONT_HEIGHT * 4 + this.height / 2, 0xff5522);
            }
        }

        @Override
        protected void actionPerformed(GuiButton button)
        {
            // You can process any additional buttons you may have added here
            super.actionPerformed(button);
            if (button.enabled && button.id == 10)
            {
                this.mc.displayGuiScreen(new MalmoAuthenticationScreen(this));
            }
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
