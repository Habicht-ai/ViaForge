package com.viaversion.viaforge.mixin.impl.mobs;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter.BlockItemPacketRewriter1_11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The native llama screen uses server slots, so Via's donkey slot padding is unnecessary. */
@Mixin(value = BlockItemPacketRewriter1_11.class, remap = false)
public abstract class MixinLlamaSlots {
    @Inject(method = "isLlama", at = @At("HEAD"), cancellable = true)
    private void nativeSlots(UserConnection user, CallbackInfoReturnable<Boolean> ci) {
        if (user.isClientSide() && com.viaversion.viaforge.common.compatibility.CompatibilityRegistry.forUser(user).has(com.viaversion.viaforge.common.compatibility.ClientFeature.MOBS)) ci.setReturnValue(false);
    }
}
