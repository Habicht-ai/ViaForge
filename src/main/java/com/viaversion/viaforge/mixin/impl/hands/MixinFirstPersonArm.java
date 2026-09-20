package com.viaversion.viaforge.mixin.impl.hands;

import com.viaversion.viaforge.hands.FirstPersonArm;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderPlayer.class)
public abstract class MixinFirstPersonArm {
    @Redirect(method = {"renderRightArm", "renderLeftArm"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelPlayer;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V"), require = 2)
    private void neutralArm(ModelPlayer model, float limb, float amount, float age,
                            float yaw, float pitch, float scale, Entity entity) {
        FirstPersonArm.angles(model, limb, amount, age, yaw, pitch, scale, entity);
    }
}
