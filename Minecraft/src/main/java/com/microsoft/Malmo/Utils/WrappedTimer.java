package com.microsoft.Malmo.Utils;

import net.minecraft.util.Timer;

public class WrappedTimer extends Timer {
    public static final float DEFAULT_MS_PER_TICK = 1000 / 20;

    protected final Timer wrapped;

    public WrappedTimer(Timer wrapped) {
        super(0);
        this.wrapped = wrapped;
        copy(wrapped, this);
    }

    @Override
    public void updateTimer() {
        copy(this, wrapped);
        wrapped.updateTimer();
        copy(wrapped, this);
    }

    protected void copy(Timer from, Timer to) {
        to.elapsedTicks = from.elapsedTicks;
        to.renderPartialTicks = from.renderPartialTicks;
        to.lastSyncSysClock = from.lastSyncSysClock;
        to.elapsedPartialTicks = from.elapsedPartialTicks;
        //#if MC>=11200
        // to.tickLength = from.tickLength;
        //#else
        to.ticksPerSecond = from.ticksPerSecond;
        to.lastHRTime = from.lastHRTime;
        to.timerSpeed = from.timerSpeed;
        to.lastSyncHRClock = from.lastSyncHRClock;
        //#if MC>=10809
        to.counter = from.counter;
        //#else
        //$$ to.field_74285_i = from.field_74285_i;
        //#endif
        to.timeSyncAdjustment = from.timeSyncAdjustment;
        //#endif
    }
}
