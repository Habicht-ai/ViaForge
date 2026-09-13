package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerCombatState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinCombatMiss {
    @Inject(method = "clickMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;swingItem()V"))
    private void miss(CallbackInfo ci) {
        MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) ServerCombatState.attack();
    }
}
