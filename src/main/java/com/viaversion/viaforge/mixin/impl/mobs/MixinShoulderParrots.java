package com.viaversion.viaforge.mixin.impl.mobs;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RenderPlayer.class)
public abstract class MixinShoulderParrots {
    @Inject(method="<init>(Lnet/minecraft/client/renderer/entity/RenderManager;Z)V",at=@At("RETURN"))
    private void parrots(CallbackInfo ci) { ((RenderPlayer)(Object)this).addLayer(new com.viaversion.viaforge.mobs.ShoulderParrots()); }
}
