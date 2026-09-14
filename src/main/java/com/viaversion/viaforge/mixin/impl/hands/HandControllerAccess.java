package com.viaversion.viaforge.mixin.impl.hands;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(PlayerControllerMP.class)
public interface HandControllerAccess {
    @Invoker("syncCurrentPlayItem") void viaForge$syncSlot();
}
