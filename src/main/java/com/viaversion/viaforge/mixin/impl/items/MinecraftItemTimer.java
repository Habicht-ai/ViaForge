package com.viaversion.viaforge.mixin.impl.items;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftItemTimer {
    @Accessor("timer") Timer viaForge$itemTimer();
}
