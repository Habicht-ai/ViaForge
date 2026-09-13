package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerCombatState;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerControllerMP.class)
public abstract class MixinCombatAttack {
    @Inject(method = "attackEntity", at = @At("RETURN"))
    private void attack(EntityPlayer player, Entity target, CallbackInfo ci) { ServerCombatState.attack(); }
}
