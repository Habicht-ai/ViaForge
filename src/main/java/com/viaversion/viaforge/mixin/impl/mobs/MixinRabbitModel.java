package com.viaversion.viaforge.mixin.impl.mobs;

import com.viaversion.viaforge.mobs.ServerMobs;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The 1.9 rabbit shrank visually as well as physically; children retain their larger heads. */
@Mixin(ModelRabbit.class)
public abstract class MixinRabbitModel extends ModelBase {
    @Shadow ModelRenderer rabbitLeftFoot, rabbitRightFoot, rabbitLeftThigh, rabbitRightThigh, rabbitBody,
            rabbitLeftArm, rabbitRightArm, rabbitHead, rabbitRightEar, rabbitLeftEar, rabbitTail, rabbitNose;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void targetSize(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale, CallbackInfo ci) {
        if (ServerMobs.get(entity) == null) return;
        setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
        if (isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(.56666666F, .56666666F, .56666666F);
            GlStateManager.translate(0, 22 * scale, 2 * scale);
            viaForge$head(scale); GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(.4F, .4F, .4F); GlStateManager.translate(0, 36 * scale, 0);
            viaForge$body(scale); GlStateManager.popMatrix();
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.scale(.6F, .6F, .6F); GlStateManager.translate(0, 16 * scale, 0);
            viaForge$body(scale); viaForge$head(scale); GlStateManager.popMatrix();
        }
        ci.cancel();
    }
    @Unique private void viaForge$head(float scale) {
        rabbitHead.render(scale); rabbitLeftEar.render(scale); rabbitRightEar.render(scale); rabbitNose.render(scale);
    }
    @Unique private void viaForge$body(float scale) {
        rabbitLeftFoot.render(scale); rabbitRightFoot.render(scale); rabbitLeftThigh.render(scale); rabbitRightThigh.render(scale);
        rabbitBody.render(scale); rabbitLeftArm.render(scale); rabbitRightArm.render(scale); rabbitTail.render(scale);
    }
}
