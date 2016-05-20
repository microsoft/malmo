package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Simple IObservationProducer class that returns a list of the full inventory, including the armour.
 */
public class ObservationFromFullInventoryImplementation extends HandlerBase implements IObservationProducer
{
    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
    	EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        int nSlots = player.inventory.getSizeInventory();
        for (int i = 0; i < nSlots; i++)
        {
            ItemStack is = player.inventory.getStackInSlot(i);
            if (is != null)
            {
                json.addProperty("InventorySlot_" + i + "_size", is.stackSize);
                json.addProperty("InventorySlot_" + i + "_item", is.getItem().getUnlocalizedName());
            }
        }
        System.out.println(json.toString());
    }
    
	@Override
	public void prepare(MissionInit missionInit) {}

	@Override
	public void cleanup() {}
}