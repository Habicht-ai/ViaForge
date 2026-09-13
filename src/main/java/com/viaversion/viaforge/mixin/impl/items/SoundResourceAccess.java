package com.viaversion.viaforge.mixin.impl.items;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundList;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SoundHandler.class)
public interface SoundResourceAccess {
    @Invoker("loadSoundResource") void viaForge$loadSound(ResourceLocation location, SoundList sounds);
}
