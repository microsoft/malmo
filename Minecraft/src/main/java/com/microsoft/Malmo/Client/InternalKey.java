package com.microsoft.Malmo.Client;

import net.minecraft.client.settings.KeyBinding;

/** KeyBinding subclass which allows us to create additional keys for our own use.<br>
 * Users should create one of these objects and override the onPressed and onKeyDown methods to carry out whatever they desire.<br>
 * These methods are not called by the vanilla Minecraft code, but by our own KeyManager object. The pressed/otherwise state of the keys
 * is maintained by Minecraft, however, provided they are sneakily inserted into the Minecraft GameSettings by the KeyManager class.
 */
public class InternalKey extends KeyBinding
{
    /** Create a KeyBinding object for the specified key, keycode and category.<br>
     * @param description see Minecraft KeyBinding class
     * @param keyCode see Minecraft KeyBinding class
     * @param category see Minecraft KeyBinding class
     */
    public InternalKey(String description, int keyCode, String category)
    {
        super(description, keyCode, category);
    }
    
    /** Method which can be overriden by extra keybindings.<br>
     * This method is NOT called by the normal Minecraft game code - so don't expect to be able to hook code in to normal game keys via this method.<br>
     * Rather, this method is called directly by the Mod code.
     */
    public void onPressed()
    {
    }
    
    /** Method which can be overriden by extra keybindings.<br>
     * This method is NOT called by the normal Minecraft game code - so don't expect to be able to hook code in to normal game keys via this method.<br>
     * Rather, this method is called directly by the Mod code.
     */
    public void onKeyDown()
    {
    }

};
