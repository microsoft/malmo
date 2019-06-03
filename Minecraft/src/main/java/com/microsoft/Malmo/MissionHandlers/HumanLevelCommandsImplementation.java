package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.Malmo.Schemas.HumanLevelCommand;
import com.microsoft.Malmo.Schemas.HumanLevelCommands;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.TimeHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class HumanLevelCommandsImplementation extends CommandGroup {
    float targetYawDelta = 0;
    float targetPitchDelta = 0;
    float targetYawDeltaDelta = 0;
    float targetPitchDeltaDelta = 0;
    TimeHelper.TickRateMonitor clientTickMonitor = new TimeHelper.TickRateMonitor();
    TimeHelper.TickRateMonitor renderTickMonitor = new TimeHelper.TickRateMonitor();

    public HumanLevelCommandsImplementation() {
        super();
        setShareParametersWithChildren(true); // Pass our parameter block on to the following children:
        List<CommandForKey> keys = getKeyOverrides();
        for (CommandForKey k : keys) {
            addCommandHandler(k);
        }
    }

    static public List<CommandForKey> getKeyOverrides() {
        List<CommandForKey> keys = new ArrayList<CommandForKey>();
        keys.add(new CommandForKey("key.forward"));
        keys.add(new CommandForKey("key.left"));
        keys.add(new CommandForKey("key.back"));
        keys.add(new CommandForKey("key.right"));
        keys.add(new CommandForKey("key.jump"));
        keys.add(new CommandForKey("key.sneak"));
        keys.add(new CommandForKey("key.sprint"));
        keys.add(new CommandForKey("key.inventory"));
        keys.add(new CommandForKey("key.swapHands"));
        keys.add(new CommandForKey("key.drop"));
        keys.add(new CommandForKey("key.use"));
        keys.add(new CommandForKey("key.attack"));
        keys.add(new CommandForKey("key.pickItem"));
        for (int i = 1; i <= 9; i++) {
            keys.add(new CommandForKey("key.hotbar." + i));
        }
        return keys;
    }

    @Override
    public boolean parseParameters(Object params) {
        super.parseParameters(params);

        if (params == null || !(params instanceof HumanLevelCommands))
            return false;

        HumanLevelCommands cmparams = (HumanLevelCommands) params;
        setUpAllowAndDenyLists(cmparams.getModifierList());
        return true;
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        if (verb.equalsIgnoreCase(HumanLevelCommand.MOVE_MOUSE.value())) {
            if (parameter != null && parameter.length() != 0) {
                String[] params = parameter.split(" ");
                if (params.length != 2 && params.length != 3) {
                    System.out.println(
                            "Malformed parameter string (" + parameter + ") - expected <x> <y>, or <x> <y> <z>");
                    return false; // Error - incorrect number of parameters.
                }
                Integer x, y, z;
                try {
                    x = Integer.valueOf(params[0]);
                    y = Integer.valueOf(params[1]);
                    z = params.length == 3 ? Integer.valueOf(params[2]) : 0;
                } catch (NumberFormatException e) {
                    System.out.println("Malformed parameter string (" + parameter + ") - " + e.getMessage());
                    return false;
                }
                if (x == null || y == null) {
                    System.out.println("Malformed parameter string (" + parameter + ")");
                    return false; // Error - incorrect parameters.
                }
                if (x != 0 || y != 0) {
                    // Code based on EntityRenderer.updateCameraAndRender:
                    Minecraft mc = Minecraft.getMinecraft();
                    float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
                    float f1 = f * f * f * 8.0F;
                    float f2 = (float) x * f1;
                    float f3 = (float) y * f1;
                    if (mc.gameSettings.invertMouse)
                        f3 = -f3;

                    // Correct any errors from last mouse move:
                    if (this.isOverriding())
                        mc.player.turn(this.targetYawDelta, this.targetPitchDelta);
                    int renderTicksPerClientTick = this.clientTickMonitor.getEventsPerSecond() >= 1 ? (int) Math.ceil(
                            this.renderTickMonitor.getEventsPerSecond() / this.clientTickMonitor.getEventsPerSecond())
                            : 0;
                    renderTicksPerClientTick = Math.max(1, renderTicksPerClientTick);
                    this.targetYawDelta = f2;
                    this.targetPitchDelta = f3;
                    this.targetYawDeltaDelta = f2 / (float) renderTicksPerClientTick;
                    this.targetPitchDeltaDelta = f3 / (float) renderTicksPerClientTick;
                    // System.out.println("Changing over " + renderTicksPerClientTick + " render
                    // ticks.");
                }
                if (z != 0) {
                    // Code based on Minecraft.runTickMouse
                    if (!Minecraft.getMinecraft().player.isSpectator() && this.isOverriding()) {
                        Minecraft.getMinecraft().player.inventory.changeCurrentItem(z);
                    }
                }
                return true;
            }
        }
        return super.onExecute(verb, parameter, missionInit);
    }

    @Override
    public boolean isFixed() {
        return true;
    }

    @Override
    public void install(MissionInit missionInit) {
        super.install(missionInit);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void deinstall(MissionInit missionInit) {
        super.deinstall(missionInit);
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent ev) {
        if (this.isCommandAllowed(HumanLevelCommand.MOVE_MOUSE.value())) {
            if (ev.phase == Phase.START) {
                // Track average client ticks per second:
                this.clientTickMonitor.beat();
            }
        }
    }

    /**
     * Called for each screen redraw - approximately three times as often as the
     * other tick events, under normal conditions.<br>
     * This is where we want to update our yaw/pitch, in order to get smooth panning
     * etc (which is how Minecraft itself does it). The speed of the render ticks is
     * not guaranteed, and can vary from machine to machine, so we try to account
     * for this in the calculations.
     * 
     * @param ev the RenderTickEvent object for this tick
     */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {

        if (this.isCommandAllowed(HumanLevelCommand.MOVE_MOUSE.value())) {
            if (ev.phase == Phase.START && this.isOverriding()) {
                // Track average fps:
                this.renderTickMonitor.beat();
                if (this.isOverriding()) {
                    EntityPlayerSP player = Minecraft.getMinecraft().player;
                    if (player != null) {
                        if (this.targetPitchDelta != 0 || this.targetYawDelta != 0) {
                            player.turn(this.targetYawDeltaDelta, this.targetPitchDeltaDelta);
                            this.targetYawDelta -= this.targetYawDeltaDelta;
                            this.targetPitchDelta -= this.targetPitchDeltaDelta;
                            if (this.targetYawDelta / this.targetYawDeltaDelta < 1.0)
                                this.targetYawDeltaDelta = this.targetYawDelta;
                            if (this.targetPitchDelta / this.targetPitchDeltaDelta < 1.0)
                                this.targetPitchDeltaDelta = this.targetPitchDelta;
                        }
                    }
                }
            }
        }
    }
}
