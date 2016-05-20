package com.microsoft.Malmo.Client;

import java.util.ArrayList;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.FMLCommonHandler;
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
            FMLCommonHandler.instance().bus().register(this);
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
