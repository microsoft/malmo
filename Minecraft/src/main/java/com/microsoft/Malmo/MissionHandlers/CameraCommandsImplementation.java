// ---------------------------------------------------------
// Author: William Guss 2019
// ---------------------------------------------------------

package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.TimeHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class CameraCommandsImplementation extends CommandBase {
    float currentYaw = -10000;
    float currentPitch = -10000;
    boolean overriding = false;

    public CameraCommandsImplementation() {
    }

    @Override
    public void install(MissionInit missionInit) {
        // super.install(missionInit);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void deinstall(MissionInit missionInit) {
        // super.deinstall(missionInit);
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public boolean isOverriding() {
        return overriding;
    }

    @Override
    public void setOverriding(boolean b) {
        this.overriding = b;
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        if (!verb.equals("camera"))
            return false;
        try {
            String[] camParams = parameter.split(" ");

            float pitch = Float.parseFloat(camParams[0]);
            float yaw = Float.parseFloat(camParams[1]);
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player != null) {
                this.currentYaw = player.rotationYaw;
                this.currentPitch = player.rotationPitch;

                player.setPositionAndRotation(player.posX, player.posY, player.posZ, this.currentYaw + yaw, this.currentPitch + pitch);

                this.currentYaw = player.rotationYaw;
                this.currentPitch = player.rotationPitch;
            }
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Malformed parameter string (" + parameter + ") - " + e.getMessage());
            return false;
        }
        return true;
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

        if (ev.phase == Phase.START && this.isOverriding()) {
            // Track average fps:
            if (this.isOverriding()) {
                EntityPlayerSP player = Minecraft.getMinecraft().player;
                if(player != null){
                    if(this.currentYaw == -10000 & this.currentPitch == -10000){
                        

                        this.currentYaw = player.rotationYaw;
                        this.currentPitch = player.rotationPitch;
                    }
                    player.setPositionAndRotation(player.posX, player.posY, player.posZ, this.currentYaw, this.currentPitch);
                }
            }
        }

    }
}
