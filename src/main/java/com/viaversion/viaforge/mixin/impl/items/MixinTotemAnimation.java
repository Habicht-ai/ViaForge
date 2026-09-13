package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerTotemAnimation;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinTotemAnimation {
    @Inject(method = "updateRenderer", at = @At("RETURN"))
    private void tickActivation(CallbackInfo ci) { ServerTotemAnimation.tick(); }
    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V"))
    private void renderActivation(float partialTicks, long nanoTime, CallbackInfo ci) { ServerTotemAnimation.render(partialTicks); }
}
