package com.microsoft.Malmo.Utils;

import net.minecraft.util.Timer;
import com.microsoft.Malmo.Utils.WrappedTimer;

//#if MC>=10800
import net.minecraftforge.fml.common.eventhandler.Event;
//#else
//$$ import cpw.mods.fml.common.eventhandler.Event;
//#endif


/**
 * Wrapper around the current timer that prevents the timer from advancing by itself.
 */
public class PauseTimer extends WrappedTimer {
    private final Timer state = new Timer(0);

    public PauseTimer(Timer wrapped) {
        super(wrapped);
    }

    @Override
    public void updateTimer() {
        copy(this, state); // Save our current state


        super.updateTimer(); // Update current state
        if(TimeHelper.isPaused()){
            this.renderPartialTicks = state.renderPartialTicks;
        }

        // System.out.println(this.elapsedTicks);
        // FML_BUS.post(new UpdatedEvent());
    }

    public Timer getWrapped() {
        return wrapped;
    }

    public static class UpdatedEvent extends Event {
    }
}
