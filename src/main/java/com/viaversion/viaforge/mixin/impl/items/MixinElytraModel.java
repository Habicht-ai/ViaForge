package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerElytraVisuals;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public abstract class MixinElytraModel {
    @Unique private float viaForge$flightLimbScale;
    @Unique private boolean viaForge$flightHead;
    @Inject(method = "setRotationAngles", at = @At("HEAD"))
    private void flightState(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity, CallbackInfo ci) {
        if (com.viaversion.viaforge.hands.FirstPersonArm.active()) {
            viaForge$flightLimbScale = 1;
            viaForge$flightHead = false;
            return;
        }
        viaForge$flightLimbScale = ServerElytraVisuals.limbAmount(entity, 1);
        viaForge$flightHead = ServerElytraVisuals.ticks(entity) > 4;
        if(com.viaversion.viaforge.items.ServerElytraFlight.flying(entity)||com.viaversion.viaforge.items.ServerElytraFlight.crawling(entity))
            ((ModelBiped)(Object)this).isSneak=false;
        else if(com.viaversion.viaforge.items.ServerElytraFlight.crouching(entity))((ModelBiped)(Object)this).isSneak=true;
    }
    // Mixin 0.7 cannot capture method arguments in ModifyVariable. Modify once,
    // at the first load after the HEAD callback, preserving the remaining hand/attack poses.
    @ModifyVariable(method = "setRotationAngles", at = @At(value = "LOAD", ordinal = 0), argsOnly = true, ordinal = 1, require = 1)
    private float flightLimbs(float original) {
        return original * viaForge$flightLimbScale;
    }
    @ModifyVariable(method = "setRotationAngles", at = @At(value = "LOAD", ordinal = 0), argsOnly = true, ordinal = 4, require = 1)
    private float flightHead(float original) {
        return viaForge$flightHead ? -45 : original;
    }
    @Inject(method="setRotationAngles",at=@At("RETURN"))
    private void crawling(float limb,float amount,float age,float yaw,float pitch,float scale,Entity entity,CallbackInfo ci) {
        if (com.viaversion.viaforge.hands.FirstPersonArm.active()) return;
        com.viaversion.viaforge.items.ServerCrawlingModel.apply((ModelBiped)(Object)this,entity,limb,age);
    }
}
