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

import java.util.ArrayList;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

import org.apache.commons.lang3.ArrayUtils;


/** Class which maintains a set of additional keybindings for functionality we wish to add into the programme, but which isn't (currently) exposed to the agent.
 */
public class KeyManager
{
    private ArrayList<InternalKey> additionalKeys;

    /** Create a new KeyManager class which will be responsible for maintaining our own extra internal keys.
     * @param settings the original Minecraft GameSettings which we are modifying
     * @param additionalKeys an array of extra "internal" keys which we want to hook in to Minecraft.
     */
    public KeyManager(GameSettings settings, ArrayList<InternalKey> additionalKeys)
    {
        this.additionalKeys = additionalKeys;
        if (additionalKeys != null)
        {
            fixAdditionalKeyBindings(settings);
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    /** Call this to finalise any additional key bindings we want to create in the mod.
     * @param settings Minecraft's original GameSettings object which we are appending to.
     */
    private void fixAdditionalKeyBindings(GameSettings settings)
    {
        if (this.additionalKeys == null)
        {
            return; // No extra keybindings to add.
        }
    
        // The keybindings are stored in GameSettings as a java built-in array.
        // There is no way to append to such arrays, so instead we create a new
        // array of the correct
        // length, copy across the current keybindings, add our own ones, and
        // set the new array back
        // into the GameSettings:
        KeyBinding[] bindings = (KeyBinding[]) ArrayUtils.addAll(settings.keyBindings, this.additionalKeys.toArray());
        settings.keyBindings = bindings;
    }
    
    /** Tick event called on the Client.<br>
     * Used to simulate pressing and releasing of our additional keys.<br>
     * This is about as close as we can (easily) get in the call stack to the point when Minecraft does the equivalent code for its own keys.
     * @param ev ClientTickEvent for this tick.
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev)
    {
        if (ev != null && ev.phase == Phase.START)
        {
            for (InternalKey binding : this.additionalKeys)
            {
                if (binding.isKeyDown())
                {
                    binding.onKeyDown();
                }
                if (binding.isPressed())
                {
                    binding.onPressed();
                }
            }
        }
    }
}
