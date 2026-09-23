package com.viaversion.viaforge.mixin.impl.compatibility;
import com.viaversion.viaforge.compatibility.ServerSwimming;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(EntityLivingBase.class)
public abstract class MixinSwimmingLiving {
    @ModifyConstant(method="onLivingUpdate",constant=@Constant(floatValue=.98F))
    private float targetInputFriction(float original) {
        EntityLivingBase entity=(EntityLivingBase)(Object)this;
        return entity==net.minecraft.client.Minecraft.getMinecraft().thePlayer&&!entity.isRiding()
            &&com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.SQUARE_SWIM_INPUT)?1F:original;
    }
    @Inject(method="canBreatheUnderwater",at=@At("HEAD"),cancellable=true)
    private void conduitBreathing(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> result) {
        EntityLivingBase entity=(EntityLivingBase)(Object)this;
        if(ServerSwimming.enabled(entity)&&entity.isPotionActive(29))result.setReturnValue(true);
    }
    @Redirect(method="moveEntityWithHeading",at=@At(value="FIELD",target="Lnet/minecraft/entity/EntityLivingBase;jumpMovementFactor:F",opcode=org.objectweb.asm.Opcodes.GETFIELD))
    private float currentAirSprint(EntityLivingBase entity) {
        // Since 1.19.4 Player.getFlyingSpeed reads this tick's sprint state.
        // The cached 1.8 field is one tick late when surfacing and starting sprint.
        if(entity==net.minecraft.client.Minecraft.getMinecraft().thePlayer
                &&com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.CURRENT_AIR_SPRINT)
                &&!((net.minecraft.entity.player.EntityPlayer)entity).capabilities.isFlying)
            return entity.isSprinting()?.025999999F:.02F;
        return entity.jumpMovementFactor;
    }
    @Inject(method="onLivingUpdate",at=@At("RETURN"))
    private void postTravelBubbles(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if(com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.DEFERRED_BLOCK_EFFECTS))ServerSwimming.bubbles((EntityLivingBase)(Object)this);
    }
    @Redirect(method="updateFallState",at=@At(value="INVOKE",target="Lnet/minecraft/entity/EntityLivingBase;handleWaterMovement()Z"))
    private boolean oneFluidSample(EntityLivingBase entity) {
        return ServerSwimming.enabled(entity)?entity.isInWater():entity.handleWaterMovement();
    }
    @Redirect(method="onLivingUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/entity/EntityLivingBase;isInWater()Z"))
    private boolean shallowWaterJump(EntityLivingBase entity) {
        return entity.isInWater()&&!(ServerSwimming.enabled(entity)&&entity.onGround&&ServerSwimming.depth(entity)<=.4D);
    }
}
