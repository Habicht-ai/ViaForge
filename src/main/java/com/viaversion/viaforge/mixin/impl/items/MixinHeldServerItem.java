package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.*;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerHeldItem.class)
public abstract class MixinHeldServerItem {
    @Shadow @Final private RendererLivingEntity<?> livingEntityRenderer;
    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void held(EntityLivingBase entity, float a, float b, float c, float d, float e, float f, float g, CallbackInfo ci) {
        if (!ServerHeldItemRenderer.imported(entity.getHeldItem()) && !com.viaversion.viaforge.hands.Offhand.active()) return;
        ServerHeldItemRenderer.render((ModelBiped)livingEntityRenderer.getMainModel(), entity, entity.getHeldItem(), false);
        offhand(entity); ci.cancel();
    }
    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V", at = @At("RETURN"))
    private void otherHand(EntityLivingBase entity, float a, float b, float c, float d, float e, float f, float g, CallbackInfo ci) { offhand(entity); }
    private void offhand(EntityLivingBase entity) {
        net.minecraft.item.ItemStack stack=com.viaversion.viaforge.hands.Offhand.of(entity);
        if(stack!=null)ServerHeldItemRenderer.render((ModelBiped)livingEntityRenderer.getMainModel(),entity,stack,true);
    }
}
