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
package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemCompass;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemCompass;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.NonNullList;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.util.ResourceLocation;


/**
 * Creates observations from a compass in the agent's inventory.
 * 
 * @author Cayden Codel, Carnegie Mellon University
 */
public class ObservationFromCompassImplementation extends HandlerBase implements IObservationProducer {
	boolean compassSet = false;

	@Override
	public void writeObservationsToJSON(JsonObject compassJson, MissionInit missionInit) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null)
			return;

		Minecraft mc = Minecraft.getMinecraft();
		ItemStack compassStack = null;
		boolean hasCompass = false, hasHotbarCompass = false, hasMainHandCompass = false, hasOffHandCompass = false;

		// If player has a compass use that one (there is randomness in compass needle)

		// Offhand compass
		for (ItemStack itemStack : mc.player.inventory.offHandInventory) {
			if (itemStack.getItem() instanceof ItemCompass) {
				compassStack = itemStack;

				hasCompass = true;
				hasOffHandCompass = true;
				break;
			}
		}
		// Main Inventory compass ( overrides offhand compass iff player is holding main hand compass)
		int invSlot = 0;
		for (ItemStack itemStack : mc.player.inventory.mainInventory) {
			if (itemStack.getItem() instanceof ItemCompass) {
				compassStack = compassStack == null ? itemStack : compassStack;
				hasCompass = true;
				if (invSlot < InventoryPlayer.getHotbarSize()) {
					hasHotbarCompass = true;
				}
				if (invSlot == mc.player.inventory.currentItem) {
					hasMainHandCompass = true;
					compassStack = itemStack;
				}
				invSlot += 1;
			}
		}
		if (!hasCompass) {
			compassStack = new ItemStack(new ItemCompass());
		}

		if(!compassSet || !hasCompass){
			compassSet = true;
			compassStack.getItem().addPropertyOverride(
				new ResourceLocation("angle"),
				 new IItemPropertyGetter(){
					@SideOnly(Side.CLIENT)
					double rotation = 0;
					@SideOnly(Side.CLIENT)
					double rota = 0;
					@SideOnly(Side.CLIENT)
					long lastUpdateTick;
					@SideOnly(Side.CLIENT)
					public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
					{
						if (entityIn == null && !stack.isOnItemFrame())
						{
							return 0.0F;
						}
						else
						{
							boolean flag = entityIn != null;
							Entity entity = (Entity)(flag ? entityIn : stack.getItemFrame());
		
							if (worldIn == null)
							{
								worldIn = entity.world;
							}
		
							double d0;
		
							if (worldIn.provider.isSurfaceWorld())
							{
								double d1 = flag ? (double)entity.rotationYaw : this.getFrameRotation((EntityItemFrame)entity);
								d1 = MathHelper.positiveModulo(d1 / 360.0D, 1.0D);
								double d2 = this.getSpawnToAngle(worldIn, entity) / (Math.PI * 2D);
								d0 = 0.5D - (d1 - 0.25D - d2);
							}
							else
							{
								d0 = 0;
							}
							if (flag)
							{
								d0 = this.wobble(worldIn, d0);
							}
		
							return MathHelper.positiveModulo((float)d0, 1.0F);
						}
					}
					@SideOnly(Side.CLIENT)
					private double wobble(World worldIn, double p_185093_2_)
					{
						if (worldIn.getTotalWorldTime() != this.lastUpdateTick)
						{
							this.lastUpdateTick = worldIn.getTotalWorldTime();
							double d0 = p_185093_2_ - this.rotation;
							d0 = MathHelper.positiveModulo(d0 + 0.5D, 1.0D) - 0.5D;
							this.rota += d0 * 0.1D;
							this.rota *= 0.8D;
							this.rotation = MathHelper.positiveModulo(this.rotation + this.rota, 1.0D);
						}
		
						return this.rotation;
					}
					@SideOnly(Side.CLIENT)
					private double getFrameRotation(EntityItemFrame p_185094_1_)
					{
						return (double)MathHelper.clampAngle(180 + p_185094_1_.facingDirection.getHorizontalIndex() * 90);
					}
					
					@SideOnly(Side.CLIENT)
					private double getSpawnToAngle(World p_185092_1_, Entity p_185092_2_)
					{
						BlockPos blockpos = p_185092_1_.getSpawnPoint();
						double angle =  Math.atan2((double)blockpos.getZ() - p_185092_2_.posZ, (double)blockpos.getX() - p_185092_2_.posX);
						return angle;
					
					}
			});
		}


		IItemPropertyGetter angleGetter = compassStack.getItem().getPropertyGetter(new ResourceLocation("angle"));
		float angle = angleGetter.apply(compassStack, mc.world, mc.player);
		angle = ((angle*360 + 180) % 360) - 180;

		compassJson.addProperty("compassAngle", angle); // Current compass angle [-180 - 180]
		compassJson.addProperty("hasCompass", hasCompass); // Player has compass in main inv or offhand
		compassJson.addProperty("hasHotbarCompass", hasHotbarCompass); // Player has compass in HOTBAR
		compassJson.addProperty("hasActiveCompass", hasMainHandCompass || hasOffHandCompass); // Player is holding a
																							  // visible compass
		compassJson.addProperty("hasMainHandCompass", hasMainHandCompass); // Player is holding a compass
		compassJson.addProperty("hasOffHandCompass", hasOffHandCompass); // Player is holding an offhand compass

		BlockPos spawn = mc.player.world.getSpawnPoint(); // Add distance observation in blocks (not vanilla!)
		compassJson.addProperty("distanceToCompassTarget",
				mc.player.getPosition().getDistance(spawn.getX(), spawn.getY(), spawn.getZ()));
	}

	@Override
	public void prepare(MissionInit missionInit) {
		compassSet = false;
	}

	@Override
	public void cleanup() {
		compassSet = false;
	}
}
