package com.microsoft.Malmo.MissionHandlers;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.JSONWorldDataHelper;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

/**
 * Simple IObservationProducer object that pings out a whole bunch of data.<br>
 */
public class ObservationFromEquippedItemImplementation extends HandlerBase implements IObservationProducer
{
	@Override
	public void prepare(MissionInit missionInit) {}

	@Override
	public void cleanup() {}

	@Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        JsonObject equippedItems = new JsonObject();
        ItemStack mainItem = player.getHeldItemMainhand();
        ItemStack offhandItem = player.getHeldItemOffhand();
        
        equippedItems.add("mainhand", getInventoryJson(mainItem));
        equippedItems.add("offhand", getInventoryJson(offhandItem));

        json.add("equipped_items", equippedItems);
    }

    public static JsonObject getInventoryJson(ItemStack itemToAdd){
            JsonObject jobj = new JsonObject();
            if (itemToAdd != null && !itemToAdd.isEmpty())
            {
                DrawItem di = MinecraftTypeHelper.getDrawItemFromItemStack(itemToAdd);
                String name = di.getType();
                if (di.getColour() != null)
                    jobj.addProperty("colour", di.getColour().value());
                if (di.getVariant() != null)
                    jobj.addProperty("variant", di.getVariant().getValue());
                jobj.addProperty("type", name);
                jobj.addProperty("quantity", itemToAdd.getCount());
                if(itemToAdd.isItemStackDamageable()){
                    jobj.addProperty("currentDamage", itemToAdd.getItemDamage());
                    jobj.addProperty("maxDamage", itemToAdd.getMaxDamage());
                } else{
                    jobj.addProperty("currentDamage", -1);
                    jobj.addProperty("maxDamage", -1);
                }
            }
            return jobj;
    }
}