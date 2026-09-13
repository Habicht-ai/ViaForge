package com.viaversion.viaforge.mixin.impl.mobs;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Entity.class)
public interface MobSizeAccess {
    @Invoker("setSize") void viaForge$size(float width, float height);
}
