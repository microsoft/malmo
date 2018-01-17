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

package com.microsoft.Malmo.Client;

import java.io.IOException;
import java.util.ArrayList;

import org.lwjgl.input.Mouse;

import com.microsoft.Malmo.Utils.CraftingHelper;
import com.microsoft.Malmo.Utils.ScreenHelper.TextCategory;
import com.microsoft.Malmo.Utils.TextureHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class MalmoModClient
{
    public interface MouseEventListener
    {
        public void onXYZChange(int deltaX, int deltaY, int deltaZ);
    }

    public class MouseHook extends MouseHelper
    {
        public boolean isOverriding = true;
        private MouseEventListener observer = null;

		/* (non-Javadoc)
         * @see net.minecraft.util.MouseHelper#mouseXYChange()
         * If we are overriding control, don't allow Minecraft to do any of the usual camera/yaw/pitch stuff that happens when the mouse moves.
         */
        @Override
        public void mouseXYChange()
        {
            if (this.isOverriding)
            {
                this.deltaX = 0;
                this.deltaY = 0;
                if (Mouse.isGrabbed())
                    Mouse.setGrabbed(false);
            }
            else
            {
                super.mouseXYChange();
                if (this.observer != null)
                    this.observer.onXYZChange(this.deltaX, this.deltaY, Mouse.getDWheel());
            }
        }

        @Override
        public void grabMouseCursor()
        {
            if (MalmoModClient.this.inputType != InputType.HUMAN)
            {
                return;
            }
            super.grabMouseCursor();
        }
    
        @Override
        /**
         * Ungrabs the mouse cursor so it can be moved and set it to the center of the screen
         */
        public void ungrabMouseCursor()
        {
            // Vanilla Minecraft calls Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2) at this point...
            // but it's seriously annoying, so we don't.
            Mouse.setGrabbed(false);
        }

        public void requestEvents(MouseEventListener observer)
        {
            this.observer = observer;
        }
    }
    
    // Control overriding:
    enum InputType
    {
        HUMAN, AI
    }

    protected InputType inputType = InputType.HUMAN;
    protected MouseHook mouseHook;
    protected MouseHelper originalMouseHelper;
	private KeyManager keyManager;
	private ClientStateMachine stateMachine;
	private static final String INFO_MOUSE_CONTROL = "mouse_control";

	public void init(FMLInitializationEvent event)
	{
        // Register for various events:
        MinecraftForge.EVENT_BUS.register(this);
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        TextureHelper.hookIntoRenderPipeline();
        setUpExtraKeys(settings);

        this.stateMachine = new ClientStateMachine(ClientState.WAITING_FOR_MOD_READY, this);

        this.originalMouseHelper = Minecraft.getMinecraft().mouseHelper;
        this.mouseHook = new MouseHook();
        this.mouseHook.isOverriding = true;
        Minecraft.getMinecraft().mouseHelper = this.mouseHook;
        setInputType(InputType.AI);
    }

    /** Switch the input type between Human and AI.<br>
     * Will switch on/off the command overrides.
     * @param input type of control (Human/AI)
     */
    public void setInputType(InputType input)
    {
    	if (this.stateMachine.currentMissionBehaviour() != null && this.stateMachine.currentMissionBehaviour().commandHandler != null)
    		this.stateMachine.currentMissionBehaviour().commandHandler.setOverriding(input == InputType.AI);

    	if (this.mouseHook != null)
    		this.mouseHook.isOverriding = (input == InputType.AI);

        // This stops Minecraft from doing the annoying thing of stealing your mouse.
        System.setProperty("fml.noGrab", input == InputType.AI ? "true" : "false");
        inputType = input;
        if (input == InputType.HUMAN)
        {
            Minecraft.getMinecraft().mouseHelper.grabMouseCursor();
        }
        else
        {
            Minecraft.getMinecraft().mouseHelper.ungrabMouseCursor();
        }

		this.stateMachine.getScreenHelper().addFragment("Mouse: " + input, TextCategory.TXT_INFO, INFO_MOUSE_CONTROL);
    }

    
    /** Set up some handy extra keys:
     * @param settings Minecraft's original GameSettings object
     */
    private void setUpExtraKeys(GameSettings settings)
    {
        // Create extra key bindings here and pass them to the KeyManager.
        ArrayList<InternalKey> extraKeys = new ArrayList<InternalKey>();
        // Create a key binding to toggle between player and Malmo control:
        extraKeys.add(new InternalKey("key.toggleMalmo", 28, "key.categories.malmo")	// 28 is the keycode for enter.
        {
            @Override
            public void onPressed()
            {
                InputType it = (inputType != InputType.AI) ? InputType.AI : InputType.HUMAN;
                System.out.println("Toggling control between human and AI - now " + it);
                setInputType(it);
                super.onPressed();
            }
        });

        extraKeys.add(new InternalKey("key.handyTestHook", 22, "key.categories.malmo")
        {
            @Override
            public void onPressed()
            {
                // Use this if you want to test some code with a handy key press
                try
                {
                    CraftingHelper.dumpRecipes("recipe_dump.txt");
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
        this.keyManager = new KeyManager(settings, extraKeys);
    }
    
    /*
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onEvent(GuiOpenEvent event)
    {
        if (event.getGui() instanceof GuiIngameModOptions)
        {
            event.setGui(new MalmoModGuiOptions.MalmoModGuiScreen(null));
        }
    }*/
}
