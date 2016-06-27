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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.client.config.GuiButtonExt;

import org.lwjgl.input.Keyboard;

import com.google.common.base.Predicate;
import com.microsoft.Malmo.MalmoModGuiOptions.MalmoModGuiScreen;
import com.microsoft.Malmo.Utils.AddressHelper;
import com.microsoft.Malmo.Utils.AuthenticationHelper;

public class MalmoAuthenticationScreen extends GuiScreen
{
    private MalmoModGuiScreen parentScreen;
    private List<AuthenticationHelper.LoginDetails> logins;
    private int maxNameWidth = 0;
    private GuiTextField usernameField;
    private GuiTextField passwordField;
    private GuiTextField portField;
    private List<GuiTextField> fields = new ArrayList<GuiTextField>();

    private enum ScreenMode
    {
        SCREEN_LIST,
        SCREEN_ADDUSER,
        SCREEN_EDITPASSWORD,
        SCREEN_EDITPORT,
        SCREEN_EDITUSERNAME
    }
    private ScreenMode mode = ScreenMode.SCREEN_LIST;
    private final int NO_USER_SELECTED = -1;
    private int selectedUser = NO_USER_SELECTED;

    private Predicate<Object> portNumbersOnly = new Predicate<Object>()
    {
        public boolean tryParseValidPortNumber(String str)
        {
            try
            {
                Integer.parseInt(str);
            }
            catch (Exception e)
            {
                return false;
            }
            return true;
        }
        public boolean apply(Object obj)
        {
            if (obj instanceof String)
                return this.tryParseValidPortNumber((String)obj);
            return false;
        }
    };

    public MalmoAuthenticationScreen(MalmoModGuiScreen parentScreen)
    {
        this.parentScreen = parentScreen;
        this.logins = AuthenticationHelper.getCopyOfLoginDetails();
    }

    // this.buttonList.add generates type safety warnings because it was declared (in GuiScreen) as a raw List, with no parameters.
    // The proper fix is to change GuiScreen to use List<GuiButton>, but we can't, because it's not our code.
    @SuppressWarnings("unchecked")
    public void initGui()
    {
        super.initGui();
        this.buttonList.clear();

        GuiButtonExt doneButton = this.parentScreen.getDoneButton();
        this.buttonList.add(doneButton);
        GuiButton cancelButton = new GuiButton(2002, doneButton.xPosition + doneButton.width + 5, doneButton.yPosition, doneButton.width, doneButton.height, "Cancel");
        this.buttonList.add(cancelButton);

        if (this.mode == ScreenMode.SCREEN_LIST)
        {
            Keyboard.enableRepeatEvents(false);
            this.usernameField = null;
            this.portField = null;
            this.passwordField = null;
            this.fields.clear();

            // Find new max name width.
            // This code will be run each time something changes, so to avoid confusing button movements,
            // we don't reset maxNameWidth to 0; buttons won't shrink, but they will grow if required.
            for (int i = 0; i < this.logins.size(); i++)
                maxNameWidth = Math.max(maxNameWidth, mc.fontRendererObj.getStringWidth(this.logins.get(i).username));
            int maxwidth = maxNameWidth + 8;	// Leave a little room.

            int i = 0;
            for (AuthenticationHelper.LoginDetails ld : this.logins)
            {
                // Button for the username:
                String name =  ld.username;
                if (name.equals(AuthenticationHelper.username))
                    name = EnumChatFormatting.GOLD + name;
                this.buttonList.add(new GuiButton(0 + (i*4), this.width/16, 32 + (i*22), maxwidth, 20, name));
                // Button for the port override:
                String port = ld.hasPortMapping() ? String.valueOf(ld.port) : "---";
                if (ld.hasPortMapping() && (ld.port < AddressHelper.MIN_MISSION_CONTROL_PORT || ld.port > AddressHelper.MAX_FREE_PORT))
                    port = EnumChatFormatting.DARK_RED + port;	// Port number is out of range.
                this.buttonList.add(new GuiButton(1 + (i*4), this.width/16 + maxwidth + 2, 32 + (i*22), 64, 20, port));
                // Button to edit the password:
                String password = ld.hasPassword() ? "password" : EnumChatFormatting.DARK_GRAY + "password";
                this.buttonList.add(new GuiButton(2 + (i*4), this.width/16 + maxwidth + 68, 32 + (i*22), 64, 20, password));
                // Delete button:
                this.buttonList.add(new GuiButton(3 + (i*4), this.width/16 + maxwidth + 134, 32 + (i*22), 16, 20, EnumChatFormatting.RED + "X"));
                i++;
            }
            GuiButton addButton = new GuiButton(2001, cancelButton.xPosition + cancelButton.width + 5, cancelButton.yPosition, cancelButton.width, cancelButton.height, "Add user");
            this.buttonList.add(addButton);
        }
        else
        {
            Keyboard.enableRepeatEvents(true);
            this.fields = new ArrayList<GuiTextField>();

            if (this.mode == ScreenMode.SCREEN_ADDUSER || this.mode == ScreenMode.SCREEN_EDITUSERNAME)
            {
                this.usernameField = new GuiTextField(2, this.fontRendererObj, this.width / 2 - 100, 60, 200, 20);
                this.usernameField.setMaxStringLength(128);
                this.usernameField.setFocused(true);

                if (this.selectedUser != NO_USER_SELECTED)
                {
                    this.usernameField.setText(this.logins.get(this.selectedUser).username);
                }
                this.fields.add(this.usernameField);
            }

            if (this.mode == ScreenMode.SCREEN_ADDUSER || this.mode == ScreenMode.SCREEN_EDITPORT)
            {
                this.portField = new GuiTextField(3, this.fontRendererObj, this.width / 2 - 100, 84, 200, 20);
                this.portField.func_175205_a(portNumbersOnly);	// Ensure only valid port numbers are typed.

                if (this.selectedUser != NO_USER_SELECTED)
                {
                    if (this.logins.get(this.selectedUser).hasPortMapping())
                    {
                        int port = this.logins.get(this.selectedUser).port;
                        this.portField.setText(String.valueOf(port));
                    }
                }
                this.fields.add(this.portField);
            }

            if (this.mode == ScreenMode.SCREEN_ADDUSER || this.mode == ScreenMode.SCREEN_EDITPASSWORD)
            {
                this.passwordField = new GuiTextField(3, this.fontRendererObj, this.width / 2 - 100, 108, 200, 20);
                this.passwordField.setMaxStringLength(128);
                this.fields.add(this.passwordField);
                if (this.selectedUser != NO_USER_SELECTED)
                {
                    if (this.logins.get(this.selectedUser).hasPassword())
                    {
                        this.passwordField.setText(this.logins.get(this.selectedUser).password);
                    }
                }
            }
        }
    }

    /**
     * Fired when a key is typed (except F11 who toggle full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);

        for (GuiTextField gtf : this.fields)
            gtf.textboxKeyTyped(typedChar, keyCode);

        if (keyCode == 28 || keyCode == 156)
        {
            this.actionPerformed((GuiButton)this.buttonList.get(0));	// "Done" button
        }
        if (keyCode == 15)
        {
            int focus = -1;
            int i = 0;
            for (GuiTextField gtf : this.fields)
            {
                if (gtf.isFocused())
                {
                    focus = i;
                    gtf.setFocused(false);
                }
                i++;
            }
            if (focus != -1)
            {
                this.fields.get((focus + 1) % this.fields.size()).setFocused(true);
            }
        }
    }

    private void setMode(ScreenMode mode)
    {
        this.mode = mode;
        this.setWorldAndResolution(this.mc,  this.width,  this.height);	// Refresh gui
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField gtf : this.fields)
            gtf.mouseClicked(mouseX,  mouseY,  mouseButton);
    }

    public void updateScreen()
    {
        for (GuiTextField gtf : this.fields)
            gtf.updateCursorCounter();
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        super.actionPerformed(button);
        if (button.id == 2000)	// "Done" button
        {
            if (this.mode == ScreenMode.SCREEN_LIST)
            {
                // Save any changes to our configuration:
                AuthenticationHelper.setLoginDetails(this.logins);
                AuthenticationHelper.save();
                AuthenticationHelper.update(MalmoMod.instance.getModPermanentConfigFile());
                this.mc.displayGuiScreen(this.parentScreen);
            }
            else if (this.mode == ScreenMode.SCREEN_ADDUSER)
            {
                Integer port = null;
                try
                {
                    port = Integer.valueOf(this.portField.getText());
                }
                catch (Exception e)
                {
                }
                if (port == null)
                    port = AuthenticationHelper.LoginDetails.nullPortMapping;
                this.logins.add(new AuthenticationHelper.LoginDetails(this.usernameField.getText(), port, this.passwordField.getText()));
                setMode(ScreenMode.SCREEN_LIST);
            }
            else if (this.mode == ScreenMode.SCREEN_EDITPASSWORD && this.selectedUser != NO_USER_SELECTED)
            {
                this.logins.get(selectedUser).password = this.passwordField.getText();
                setMode(ScreenMode.SCREEN_LIST);
            }
            else if (this.mode == ScreenMode.SCREEN_EDITPORT && this.selectedUser != NO_USER_SELECTED)
            {
                Integer port = null;
                try
                {
                    port = Integer.valueOf(this.portField.getText());
                }
                catch (Exception e)
                {
                }
                if (port == null)
                    port = AuthenticationHelper.LoginDetails.nullPortMapping;
                this.logins.get(selectedUser).port = port;
                setMode(ScreenMode.SCREEN_LIST);
            }
            else if (this.mode == ScreenMode.SCREEN_EDITUSERNAME && this.selectedUser != NO_USER_SELECTED)
            {
                this.logins.get(this.selectedUser).username = this.usernameField.getText();
                setMode(ScreenMode.SCREEN_LIST);
            }
        }
        else if (button.id == 2001)	// "Add user" button
        {
            this.selectedUser = NO_USER_SELECTED;
            setMode(ScreenMode.SCREEN_ADDUSER);
        }
        else if (button.id == 2002)	// "Cancel" button
        {
            if (this.mode != ScreenMode.SCREEN_LIST)
                setMode(ScreenMode.SCREEN_LIST);
            else
                this.mc.displayGuiScreen(this.parentScreen);
        }
        else
        {
            this.selectedUser = button.id / 4;
            int action = button.id % 4;
            switch (action)
            {
            case 0:	// Name
                setMode(ScreenMode.SCREEN_EDITUSERNAME);
                break;
            case 1:	// Port mapping
                setMode(ScreenMode.SCREEN_EDITPORT);
                break;
            case 2:	// Password
                setMode(ScreenMode.SCREEN_EDITPASSWORD);
                break;
            case 3:	// Delete
                this.logins.remove(this.selectedUser);
                this.selectedUser = NO_USER_SELECTED;
                this.setWorldAndResolution(this.mc,  this.width,  this.height);	// Refresh gui
                break;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        super.drawScreen(mouseX,  mouseY,  partialTicks);

        this.drawCenteredString(this.fontRendererObj, this.parentScreen.title, this.width / 2, 8, 16777215);
        String title2 = this.parentScreen.titleLine2;

        if (title2 != null)
        {
            int strWidth = mc.fontRendererObj.getStringWidth(title2);
            int elipsisWidth = mc.fontRendererObj.getStringWidth("...");
            if (strWidth > width - 6 && strWidth > elipsisWidth)
                title2 = mc.fontRendererObj.trimStringToWidth(title2, width - 6 - elipsisWidth).trim() + "...";
            this.drawCenteredString(this.fontRendererObj, title2, this.width / 2, 18, 16777215);
        }

        for (GuiTextField gtf : this.fields)
            gtf.drawTextBox();

        if (this.usernameField != null)
            this.drawCenteredString(this.fontRendererObj, "Username:", this.width / 2 - 138, 66, 0xdddddd);
        if (this.portField != null)
            this.drawCenteredString(this.fontRendererObj, "Port:", this.width / 2 - 138, 90, 0xdddddd);
        if (this.passwordField != null)
        {
            this.drawCenteredString(this.fontRendererObj, "Password:", this.width / 2 - 138, 114, 0xdddddd);
            this.drawCenteredString(this.fontRendererObj, "WARNING: passwords are stored in PLAIN TEXT in", this.width/2, this.height - 64, 0xff0000);
            this.drawCenteredString(this.fontRendererObj, MalmoMod.instance.getModPermanentConfigFile().getConfigFile().getAbsolutePath(), this.width/2, this.height - 50, 0xff0000);
        }
    }
}