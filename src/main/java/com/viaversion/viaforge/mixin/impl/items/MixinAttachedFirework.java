package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.items.ServerFireworks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityFireworkRocket.class)
public abstract class MixinAttachedFirework {
    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityFireworkRocket;moveEntity(DDD)V"))
    private void followPlayer(EntityFireworkRocket rocket, double x, double y, double z) {
        ServerFireworks.move(rocket, x, y, z);
    }
    @Inject(method = "isInRangeToRenderDist", at = @At("HEAD"), cancellable = true)
    private void hideAttachedRocket(double distance, CallbackInfoReturnable<Boolean> ci) {
        if (ServerFireworks.attached((Entity)(Object)this)) ci.setReturnValue(false);
    }
    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V"))
    private void trail(World world, EnumParticleTypes type, double x, double y, double z, double vx, double vy, double vz, int[] data) {
        world.spawnParticle(type, x, y + (ServerSession.rule(ClientRule.FIREWORK_HAND_TRAIL) ? .3 : 0), z, vx, vy, vz, data);
    }
    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundAtEntity(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
    private void launchSound(World world, Entity entity, String sound, float volume, float pitch) {
        // 1.9+ sends the launch event from the server; replaying 1.8's local copy doubles it.
        if (!ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.ELYTRA)) world.playSoundAtEntity(entity, sound, volume, pitch);
    }
}
