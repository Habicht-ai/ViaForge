package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerTotemParticle;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(EffectRenderer.class)
public abstract class MixinTotemParticleLayer {
    @Redirect(method = "addEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;getAlpha()F"))
    private float originalDepthLayer(EntityFX particle) {
        // ParticleSimpleAnimated disables depth writes even while fully opaque.
        // This return value only selects the list; the rendered alpha stays intact.
        return particle instanceof ServerTotemParticle || particle instanceof com.viaversion.viaforge.mobs.MobParticles
                && ((com.viaversion.viaforge.mobs.MobParticles)particle).type == 43 ? 0F : particle.getAlpha();
    }
}
