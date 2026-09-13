package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public abstract class MixinShieldArmPose {
    @Inject(method = "setRotationAngles", at = @At("HEAD"))
    private void shieldPose(float a, float b, float c, float d, float e, float f, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof EntityPlayer) || !ServerBlockSession.supportsProtocol(107)) return;
        ModelBiped model = (ModelBiped)(Object)this;
        EntityPlayer player = (EntityPlayer)entity;
        ServerEntityViews.View view = ServerEntityViews.get(entity.getEntityId());
        model.heldItemLeft = 0;
        if (view != null && (view.handState & 3) == 3 && ClientItems.is(view.offhand, Kind.SHIELD)) model.heldItemRight = player.getHeldItem() == null ? 0 : 1;
        for (boolean other : new boolean[]{false, true}) {
            ItemStack stack = other ? view == null ? null : view.offhand : player.getHeldItem();
            if (!ClientItems.is(stack, Kind.SHIELD)) continue;
            int pose = ServerEntityViews.blocking(player, other, stack) ? 3 : 1;
            if (other ^ (view != null && view.leftHanded)) {
                model.heldItemLeft = pose;
                if (!other) model.heldItemRight = 0;
            } else model.heldItemRight = pose;
        }
    }
    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    private void leftShieldYaw(float a, float b, float c, float d, float e, float f, Entity entity, CallbackInfo ci) {
        ModelBiped model = (ModelBiped)(Object)this;
        if (entity instanceof EntityPlayer && ServerBlockSession.supportsProtocol(107) && model.heldItemLeft == 3) model.bipedLeftArm.rotateAngleY += .5235988F;
    }
}
