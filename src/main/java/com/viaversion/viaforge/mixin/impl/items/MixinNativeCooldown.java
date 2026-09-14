package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaversion.api.connection.UserConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.viaversion.viarewind.protocol.v1_9to1_8.storage.CooldownStorage", remap = false)
public abstract class MixinNativeCooldown {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void nativeIndicator(UserConnection connection, CallbackInfo ci) {
        if (connection.isClientSide() && com.viaversion.viaforge.common.compatibility.CompatibilityRegistry.forUser(connection).has(com.viaversion.viaforge.common.compatibility.ClientFeature.COMBAT)) ci.cancel();
    }
}
