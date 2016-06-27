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
