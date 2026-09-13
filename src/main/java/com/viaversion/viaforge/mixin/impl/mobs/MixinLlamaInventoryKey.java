package com.viaversion.viaforge.mixin.impl.mobs;

import com.viaversion.viaforge.common.blocks.MobKind;
import com.viaversion.viaforge.mobs.ServerMob;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public abstract class MixinLlamaInventoryKey {
    @Inject(method = "isRidingHorse", at = @At("HEAD"), cancellable = true)
    private void ridingLlama(CallbackInfoReturnable<Boolean> ci) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        Entity mount = Minecraft.getMinecraft().thePlayer.ridingEntity;
        if (mount instanceof ServerMob && ((ServerMob)mount).kind() == MobKind.LLAMA) ci.setReturnValue(true);
    }
}
