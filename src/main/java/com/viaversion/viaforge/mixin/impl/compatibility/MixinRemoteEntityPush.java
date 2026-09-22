package com.viaversion.viaforge.mixin.impl.compatibility;
import com.viaversion.viaforge.compatibility.ClientEntityPush;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityOtherPlayerMP.class)
public abstract class MixinRemoteEntityPush {
    @Inject(method="onLivingUpdate",at=@At("TAIL"))
    private void afterInterpolation(CallbackInfo ci){ClientEntityPush.tick((EntityOtherPlayerMP)(Object)this);}
}
