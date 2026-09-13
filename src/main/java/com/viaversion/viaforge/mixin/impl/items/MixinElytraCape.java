package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ClientItems;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerCape.class)
public abstract class MixinElytraCape {
    @Inject(method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void wornElytra(AbstractClientPlayer player, float a, float b, float c, float d, float e, float f, float g, CallbackInfo ci) {
        if (ClientItems.is(player.getCurrentArmor(2), Kind.ELYTRA)) ci.cancel();
    }
}
