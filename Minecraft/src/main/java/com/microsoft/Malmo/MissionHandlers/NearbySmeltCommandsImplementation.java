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

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import net.minecraft.block.BlockFurnace;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.NearbySmeltCommand;
import com.microsoft.Malmo.Schemas.NearbySmeltCommands;
import com.microsoft.Malmo.Utils.CraftingHelper;

/**
 * @author Cayden Codel, Carnegie Mellon University
 * <p>
 * Extends the functionality of the SimpleCraftCommands by requiring a furnace close-by. Only handles smelting, no crafting.
 */
public class NearbySmeltCommandsImplementation extends CommandBase {
    private boolean isOverriding;
    private static ArrayList<BlockPos> furnaces;

    public static class SmeltNearbyMessage implements IMessage {
        String parameters;

        public SmeltNearbyMessage(){}

        public SmeltNearbyMessage(String parameters) {
            this.parameters = parameters;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.parameters = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, this.parameters);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        if (!event.isCanceled() && event.getPlacedBlock().getBlock() instanceof BlockFurnace)
            furnaces.add(event.getPos());
    }

    @SubscribeEvent
    public void onBlockDestroy(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getState().getBlock() instanceof BlockFurnace)
            for (int i = furnaces.size() - 1; i >= 0; i--)
                if (furnaces.get(i).equals(event.getPos()))
                    furnaces.remove(i);
    }

    public static class SmeltNearbyMessageHandler implements IMessageHandler<SmeltNearbyMessage, IMessage> {
        @Override
        public IMessage onMessage(SmeltNearbyMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Vec3d headPos = new Vec3d(player.posX, player.posY + 1.6, player.posZ);

            // Location checking
            boolean closeFurnace = false;
            for (BlockPos furnace : furnaces) {
                Vec3d blockVec = new Vec3d(furnace.getX() + 0.5, furnace.getY() + 0.5, furnace.getZ() + 0.5);

                if (headPos.squareDistanceTo(blockVec) <= 25.0) {
                    // Within a reasonable FOV?
                    // Lots of trig, let's go
                    double fov = Minecraft.getMinecraft().gameSettings.fovSetting;
                    double height = Minecraft.getMinecraft().displayHeight;
                    double width = Minecraft.getMinecraft().displayWidth;
                    Vec3d lookVec = player.getLookVec();
                    Vec3d toBlock = blockVec.subtract(headPos);

                    // Projection of block onto player look vector - if greater than 0, then in front of us
                    double scalarProjection = lookVec.dotProduct(toBlock) / lookVec.lengthVector();
                    if (scalarProjection > 0) {
                        Vec3d yUnit = new Vec3d(0, 1.0, 0);
                        Vec3d lookCross = lookVec.crossProduct(yUnit);
                        Vec3d blockProjectedOntoCross = lookCross.scale(lookCross.dotProduct(toBlock) / lookCross.lengthVector());
                        Vec3d blockProjectedOntoPlayerPlane = toBlock.subtract(blockProjectedOntoCross);
                        double xyDot = lookVec.dotProduct(blockProjectedOntoPlayerPlane);
                        double pitchTheta = Math.acos(xyDot / (lookVec.lengthVector() * blockProjectedOntoPlayerPlane.lengthVector()));

                        Vec3d playerY = lookCross.crossProduct(lookVec);
                        Vec3d blockProjectedOntoPlayerY = playerY.scale(playerY.dotProduct(toBlock) / playerY.lengthVector());
                        Vec3d blockProjectedOntoYawPlane = toBlock.subtract(blockProjectedOntoPlayerY);
                        double xzDot = lookVec.dotProduct(blockProjectedOntoYawPlane);
                        double yawTheta = Math.acos(xzDot / (lookVec.lengthVector() * blockProjectedOntoYawPlane.lengthVector()));

                        if (Math.abs(Math.toDegrees(yawTheta)) <= Math.min(1, width / height) * (fov / 2.0) && Math.abs(Math.toDegrees(pitchTheta)) <= Math.min(1, height / width) * (fov / 2.0))
                            closeFurnace = true;
                    }
                }
            }

            if (closeFurnace) {
                ItemStack input = CraftingHelper.getSmeltingRecipeForRequestedOutput(message.parameters);
                if (input != null)
                    if (CraftingHelper.attemptSmelting(player, input))
                        return null;
            }

            return null;
        }
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        if (verb.equalsIgnoreCase("nearbySmelt") && ! parameter.equalsIgnoreCase("none")) {
            MalmoMod.network.sendToServer(new SmeltNearbyMessage(parameter));
            return true;
        }
        return false;
    }

    @Override
    public boolean parseParameters(Object params) {
        furnaces = new ArrayList<BlockPos>();

        if (!(params instanceof NearbySmeltCommands))
            return false;

        NearbySmeltCommands cParams = (NearbySmeltCommands) params;
        setUpAllowAndDenyLists(cParams.getModifierList());
        return true;
    }

    @Override
    public void install(MissionInit missionInit) {
        CraftingHelper.reset();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void deinstall(MissionInit missionInit) {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public boolean isOverriding() {
        return this.isOverriding;
    }

    @Override
    public void setOverriding(boolean b) {
        this.isOverriding = b;
    }
}