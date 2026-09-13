package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftBlockSession {
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void resetBlockResources(WorldClient world, String message, CallbackInfo ci) {
        if (world == null) ServerBlockSession.unload();
    }
}
