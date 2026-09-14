package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerCombatState;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public abstract class MixinCombatTick {
    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void attackTimer(CallbackInfo ci) {
        ServerCombatState.tick((EntityPlayer)(Object)this);
        com.viaversion.viaforge.items.ServerItemCooldowns.tick((EntityPlayer)(Object)this);
        com.viaversion.viaforge.hands.Offhand.tick((EntityPlayer)(Object)this);
    }
}
