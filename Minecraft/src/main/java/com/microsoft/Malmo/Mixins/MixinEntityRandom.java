

package com.microsoft.Malmo.Mixins;


import java.util.Random;

import com.microsoft.Malmo.Utils.SeedHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;


@Mixin(Entity.class)
public abstract class MixinEntityRandom  {
    // /* Overrides methods within the MinecraftServer class.
    //  */

    @Shadow protected Random rand;

    @Redirect(method = "<init>*", at = @At(value = "FIELD", target = "net/minecraft/entity/Entity.rand:F", ordinal = 1))
    private Random onSetRand(Entity e) {
        rand = SeedHelper.getRandom("entity");
        return rand;
    }
  
}
