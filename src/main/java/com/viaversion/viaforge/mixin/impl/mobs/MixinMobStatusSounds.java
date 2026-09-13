package com.viaversion.viaforge.mixin.impl.mobs;
import com.viaversion.viaforge.mobs.ServerMobSounds;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(EntityLivingBase.class)
public abstract class MixinMobStatusSounds {
    @org.spongepowered.asm.mixin.Unique private String viaForge$damageSound;
    @Shadow protected abstract String getHurtSound();
    @Shadow protected abstract String getDeathSound();
    @Inject(method="handleStatusUpdate",at=@At("HEAD"),cancellable=true)
    private void damageType(byte status, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        EntityLivingBase entity = (EntityLivingBase)(Object)this;
        com.viaversion.viaforge.mobs.MobState state = com.viaversion.viaforge.mobs.ServerMobs.get(entity);
        if (state == null || status != 33 && (state.protocol < 335 || status != 36 && status != 37)) return;
        viaForge$damageSound = ServerMobSounds.key(status == 33 ? "enchant.thorns.hit" : status == 36 ? "entity.generic.hurt_drown" : "entity.generic.hurt_burn", 5);
        try { entity.handleStatusUpdate((byte)2); } finally { viaForge$damageSound = null; }
        ci.cancel();
    }
    @Redirect(method="handleStatusUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/entity/EntityLivingBase;getHurtSound()Ljava/lang/String;"))
    private String hurt(EntityLivingBase entity) {
        if (viaForge$damageSound != null) return viaForge$damageSound;
        String sound = ServerMobSounds.entity(entity,"hurt"); return sound == null ? getHurtSound() : sound;
    }
    @Redirect(method="handleStatusUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/entity/EntityLivingBase;getDeathSound()Ljava/lang/String;"))
    private String death(EntityLivingBase entity) {
        String sound = ServerMobSounds.entity(entity,"death"); return sound == null ? getDeathSound() : sound;
    }
}
