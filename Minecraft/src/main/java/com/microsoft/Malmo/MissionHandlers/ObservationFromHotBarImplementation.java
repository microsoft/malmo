package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Simple IObservationProducer class that returns a list of what is in the "hotbar".
 */
public class ObservationFromHotBarImplementation extends HandlerBase implements IObservationProducer
{
	@Override
	public void prepare(MissionInit missionInit) {}

	@Override
	public void cleanup() {}

	@Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        for (int i = 0; i < 9; i++)
        {
        	EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
            ItemStack is = player.inventory.getStackInSlot(i);
            if (is != null)
            {
                json.addProperty("Hotbar_" + i + "_size", is.stackSize);
                json.addProperty("Hotbar_" + i + "_item", is.getItem().getUnlocalizedName());
            }
        }
        System.out.println(json.toString());
    }
}