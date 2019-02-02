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

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyBindingMap;

import com.microsoft.Malmo.Schemas.MissionInit;

/** KeyBinding subclass which opens up the Minecraft keyhandling to external agents for a particular key.<br>
 * If this class is set to override control, it will prevent the KeyBinding baseclass methods from being called,
 * and will instead provide its own interpretation of the state of the keyboard. This allows it to respond to command
 * messages sent externally.<br>
 * Note that one instance of this class overrides one key.<br>
 * Note also that the attack key and use key - although bound to the mouse rather than the keyboard, by default,
 * are implemented in Minecraft using this same KeyBinding object, so this mechanism allows us to control them too.
 */
public class CommandForKey extends CommandBase
{
    public static final String DOWN_COMMAND_STRING = "1";
    public static final String UP_COMMAND_STRING = "0";

    public interface KeyEventListener
    {
        public void onKeyChange(String commandString, boolean pressed);
    }

    private class KeyHook extends KeyBinding
    {
        /**
         * Tracks whether or not this object is overriding the default Minecraft
         * keyboard handling.
         */
        private boolean isOverridingPresses = false;
        private boolean isDown = false;
        private boolean justPressed = false;
        private String commandString = null;
        private boolean keyDownEventSent = false;
        private boolean lastPressedState = false;
        private boolean lastKeydownState = false;
        private KeyEventListener observer = null;

        /** Create a KeyBinding object for the specified key, keycode and category.<br>
         * @param description see Minecraft KeyBinding class
         * @param keyCode see Minecraft KeyBinding class
         * @param category see Minecraft KeyBinding class
         */
        public KeyHook(String description, int keyCode, String category)
        {
            super(description, keyCode, category);
        }

        /** Set our "pressed" state to true and "down" state to true.<br>
         * This provides a means to set the state externally, without anyone actually having to press a key on the keyboard.
         */
        public void press()
        {
            this.isDown = true;
            this.justPressed = true;
        }

        /** Set our "down" state to false.<br>
         * This provides a means to set the state externally, without anyone actually having to press a key on the keyboard.
         */
        public void release()
        {
            this.isDown = false;
        }

        /**
         * Return true if this key is "down"<br>
         * ie the controlling code has issued a key-down command and hasn't yet
         * followed it with a key-up. If this object is not currently set to
         * override, the default Minecraft keyboard handling will be used.
         * 
         * @return true if the key is in its "down" state.
         */
        @Override
        public boolean isKeyDown()
        {
            boolean bReturn = this.isOverridingPresses ? this.isDown : super.isKeyDown();
            if (this.observer != null && !this.isOverridingPresses)
            {
                if (bReturn && !this.keyDownEventSent)
                {
                    this.observer.onKeyChange(this.getCommandString(), true);
                    this.keyDownEventSent = true;
                }
                else if (!bReturn && this.keyDownEventSent)
                {
                    this.observer.onKeyChange(this.getCommandString(), false);
                    this.keyDownEventSent = false;
                }
            }
            return bReturn;
        }
    
        /**
         * Return true if this key is "pressed"<br>
         * This is used for one-shot responses in Minecraft - ie isPressed()
         * will only return true once, even if isKeyDown is still returning
         * true. If this object is not currently set to override, the default
         * Minecraft keyboard handling will be used.
         * 
         * @return true if the key has been pressed since the last time this was
         *         called.
         */
        @Override
        public boolean isPressed()
        {
            boolean bReturn = this.isOverridingPresses ? this.justPressed : super.isPressed();
            this.justPressed = false; // This appears to be how the KeyBinding
                                      // is expected to work.
            if (this.observer != null && !this.isOverridingPresses)
            {
                if (bReturn)
                {
                    // Always send an event if pressed is true.
                    this.observer.onKeyChange(this.getCommandString(), true);
                    this.keyDownEventSent = true;
                }
            }
            return bReturn;
        }
    
        /**
         * Construct a command string from our internal key description.<br>
         * This is the command that we expect to be given from outside in order
         * to control our state.<br>
         * For example, the second hotbar key ("2" on the keyboard, by default)
         * will have a description of "key.hotbar.2", which will result in a
         * command string of "hotbar.2".<br>
         * To "press" and "release" this key, the agent needs to send
         * "hotbar.2 1" followed by "hotbar.2 0".
         * 
         * @return the command string, parsed from the key's description.
         */
        private String getCommandString()
        {
            if (this.commandString == null)
            {
                this.commandString = getKeyDescription();
                int splitpoint = this.commandString.indexOf("."); // Descriptions
                                                                  // are
                                                                  // "key.whatever"
                                                                  // - remove
                                                                  // the "key."
                                                                  // part.
                if (splitpoint != -1 && splitpoint != this.commandString.length())
                {
                    this.commandString = this.commandString.substring(splitpoint + 1);
                }
            }
            return this.commandString;
        }

        /**
         * Attempt to handle this command string, if relevant.
         * 
         * @param command
         *            the command to handle. eg "attack 1" means
         *            "press the attack key".
         * @return true if the command was relevant and was successfully
         *         handled; false otherwise.
         */
        public boolean execute(String verb, String parameter)
        {
            if (verb != null && verb.equalsIgnoreCase(getCommandString()))
            {
                if (parameter != null && parameter.equalsIgnoreCase(DOWN_COMMAND_STRING))
                {
                    press();
                }
                else if (parameter != null && parameter.equalsIgnoreCase(UP_COMMAND_STRING))
                {
                    release();
                }
                else
                {
                    return false;
                }
                return true;
            }
            return false;
        }

        public void setObserver(KeyEventListener observer)
        {
            this.observer = observer;
        }
    }

    private KeyHook keyHook = null;
    private KeyBinding originalBinding = null;
    private int originalBindingIndex;
    private String keyDescription;
    
    /** Helper function to create a KeyHook object for a given KeyBinding object.
     * @param key the Minecraft KeyBinding object we are wrapping
     * @return an ExternalAIKey object to replace the original Minecraft KeyBinding object
     */
    private KeyHook create(KeyBinding key)
    {
        if (key != null && key instanceof KeyHook)
        {
            return (KeyHook)key; // Don't create a KeyHook to replace this KeyBinding, since that has already been done at some point.
            // (Minecraft keeps a pointer to every KeyBinding that gets created, and they never get destroyed - so we don't want to create
            // any more than necessary.)
        }
        return new KeyHook(key.getKeyDescription(), key.getKeyCode(), key.getKeyCategory());
    }

    /** Create an ICommandHandler interface for the specified key.
     * @param key the description of the key we want to provide commands to.
     */
    public CommandForKey(String key)
    {
        this.keyDescription = key;
    }

    /** Is this object currently overriding the default Minecraft KeyBinding object?
     * @return true if this object is overriding the default keyboard handling.
     */
    @Override
    public boolean isOverriding()
    {
        return (this.keyHook != null) ? this.keyHook.isOverridingPresses : false;
    }

    /** Switch this object "on" or "off".
     * @param b true if this object is to start overriding the normal Minecraft handling.
     */
    @Override
    public void setOverriding(boolean b)
    {
        if (this.keyHook != null)
        {
            this.keyHook.isDown = false;
            this.keyHook.justPressed = false;
            this.keyHook.isOverridingPresses = b;
        }
    }

    public void setKeyEventObserver(KeyEventListener observer)
    {
        this.keyHook.setObserver(observer);
    }

    @Override
    public void install(MissionInit missionInit)
    {
        // Attempt to find the keybinding that matches the description we were given,
        // and replace it with our own KeyHook object:
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        boolean createdHook = false;
        // GameSettings contains both a field for each KeyBinding (eg keyBindAttack), and an array of KeyBindings with a pointer to
        // each field. We want to make sure we replace both pointers, otherwise Minecraft will end up using our object for some things, and
        // the original object for others.
        // So we need to use reflection to replace the field:
        Field[] fields = GameSettings.class.getFields();
        for (int i = 0; i < fields.length; i++)
        {
            Field f = fields[i];
            if (f.getType() == KeyBinding.class)
            {
                KeyBinding kb;
                try
                {
                    kb = (KeyBinding)(f.get(settings));
                    if (kb != null && kb.getKeyDescription().equals(this.keyDescription))
                    {
                        this.originalBinding = kb;
                        this.keyHook = create(this.originalBinding);
                        createdHook = true;
                        f.set(settings, this.keyHook);
                    }
                }
                catch (IllegalArgumentException e)
                {
                    e.printStackTrace();
                }
                catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                }
            }
        }
        // And then we replace the pointer in the array:
        for (int i = 0; i < settings.keyBindings.length; i++)
        {
            if (settings.keyBindings[i].getKeyDescription().equals(this.keyDescription))
            {
                this.originalBindingIndex = i;
                if (!createdHook)
                {
                    this.originalBinding = settings.keyBindings[i];
                    this.keyHook = create(this.originalBinding);
                    createdHook = true;
                }
                settings.keyBindings[i] = this.keyHook;
            }
        }
        // And possibly in the hotbar array too:
        for (int i = 0; i < settings.keyBindsHotbar.length; i++)
        {
            if (settings.keyBindsHotbar[i].getKeyDescription().equals(this.keyDescription))
            {
                this.originalBindingIndex = i;
                if (!createdHook)
                {
                    this.originalBinding = settings.keyBindsHotbar[i];
                    this.keyHook = create(this.originalBinding);
                    createdHook = true;
                }
                settings.keyBindsHotbar[i] = this.keyHook;
            }
        }
        // Newer versions of MC have changed the way they map from key value to KeyBinding, so we
        // *also* need to fiddle with the static KeyBinding HASH map:_
        Field[] kbfields = KeyBinding.class.getDeclaredFields();
        for (Field f : kbfields)
        {
            if (f.getType() == KeyBindingMap.class)
            {
                net.minecraftforge.client.settings.KeyBindingMap kbp;
                try
                {
                    f.setAccessible(true);
                    kbp = (KeyBindingMap) (f.get(null));
                    // Our new keybinding should already have been added;
                    // just need to remove the original one.
                    while (kbp.lookupAll(this.keyHook.getKeyCode()).size() > 1)
                        kbp.removeKey(this.originalBinding);
                    return;
                }
                catch (IllegalArgumentException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IllegalAccessException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void deinstall(MissionInit missionInit)
    {
        // Do nothing - it's not a simple thing to deinstall ourselves, as Minecraft will keep pointers to us internally,
        // and will end up confused. It's safer simply to stay hooked in. As long as overriding is turned off, the game
        // will behave normally anyway.
    }

    @Override
    public boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        // Our keyhook does all the work:
        return (this.keyHook != null) ? this.keyHook.execute(verb, parameter) : false;
    }
    
    /** Return the KeyBinding object we are using.<br>
     * Mainly provided for the use of the unit tests.
     * @return our internal KeyBinding object.
     */
    public KeyBinding getKeyBinding()
    {
        return this.keyHook;
    }
}