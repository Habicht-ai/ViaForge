package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerTotemSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundHandler.class)
public abstract class MixinTotemSoundReload {
    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void reloadActivationSound(IResourceManager resources, CallbackInfo ci) {
        if (net.minecraft.client.Minecraft.getMinecraft().getSoundHandler() != null) ServerTotemSound.reload();
        if (net.minecraft.client.Minecraft.getMinecraft().getSoundHandler() != null) com.viaversion.viaforge.mobs.ServerMobSounds.reload();
    }
}
