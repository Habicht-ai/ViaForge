package com.viaversion.viaforge.mixin.impl.items;
import com.viaversion.viaforge.items.ServerElytraFlight;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityPlayerSP.class)
public abstract class MixinElytraInput {
    @Inject(method="onLivingUpdate", at=@At(value="INVOKE", target="Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V"))
    private void beforeInput(CallbackInfo ci) { ServerElytraFlight.beforeInput((EntityPlayerSP)(Object)this); }
    @Inject(method="onLivingUpdate", at=@At(value="INVOKE", target="Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V", shift=At.Shift.AFTER))
    private void poseInput(CallbackInfo ci) { ServerElytraFlight.slowInput((EntityPlayerSP)(Object)this); }
    @Inject(method="onLivingUpdate", at=@At(value="INVOKE", target="Lnet/minecraft/client/entity/EntityPlayerSP;isRidingHorse()Z"))
    private void flightInput(CallbackInfo ci) { ServerElytraFlight.input((EntityPlayerSP)(Object)this); }
    @Inject(method="onLivingUpdate", at=@At("RETURN"))
    private void afterMovement(CallbackInfo ci) { ServerElytraFlight.updatePose((EntityPlayerSP)(Object)this); }
    @Inject(method="pushOutOfBlocks", at=@At("HEAD"), cancellable=true)
    private void actualHeadspace(double x, double y, double z, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        EntityPlayerSP player=(EntityPlayerSP)(Object)this;
        // The 1.8 probe rounds height up to whole blocks from feet+0.5. It
        // otherwise ejects a prone player whose actual box fits under a slab.
        if(ServerElytraFlight.compact(player) && player.worldObj.getCollidingBoundingBoxes(player,
                player.getEntityBoundingBox().contract(1e-7,1e-7,1e-7)).isEmpty()) ci.setReturnValue(false);
    }
}
