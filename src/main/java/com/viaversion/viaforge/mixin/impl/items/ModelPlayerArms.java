package com.viaversion.viaforge.mixin.impl.items;

import net.minecraft.client.model.ModelPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPlayer.class)
public interface ModelPlayerArms {
    @Accessor("smallArms") boolean viaForge$smallArms();
}
