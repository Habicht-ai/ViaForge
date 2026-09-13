package com.viaversion.viaforge.mixin.impl.blocks;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftResourcePacks {
    @Accessor("defaultResourcePacks")
    List<IResourcePack> viaForge$defaultResourcePacks();
}
