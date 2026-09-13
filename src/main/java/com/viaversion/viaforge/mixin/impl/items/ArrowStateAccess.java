package com.viaversion.viaforge.mixin.impl.items;

import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityArrow.class)
public interface ArrowStateAccess {
    @Accessor("inGround") boolean viaForge$inGround();
    @Accessor("ticksInGround") int viaForge$ticksInGround();
}
