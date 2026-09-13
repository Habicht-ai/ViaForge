package com.viaversion.viaforge.mixin.impl.mobs;
import com.viaversion.viaforge.mobs.ServerMobs;
import net.minecraft.entity.EntityLiving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityLiving.class)
public abstract class MixinMobAmbientSound {
    @Inject(method="playLivingSound",at=@At("HEAD"),cancellable=true)
    private void serverAmbient(CallbackInfo ci) {
        if (ServerMobs.get((EntityLiving)(Object)this) != null) ci.cancel();
    }
}
