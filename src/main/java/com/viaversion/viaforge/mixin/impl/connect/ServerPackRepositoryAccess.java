package com.viaversion.viaforge.mixin.impl.connect;

import java.io.File;
import net.minecraft.client.resources.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourcePackRepository.class)
public interface ServerPackRepositoryAccess {
    @Accessor("dirServerResourcepacks") File viaForge$cache();
    @Accessor("resourcePackInstance") void viaForge$serverPack(IResourcePack pack);
}
