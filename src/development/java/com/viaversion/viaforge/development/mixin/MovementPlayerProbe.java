package com.viaversion.viaforge.development.mixin;
import com.viaversion.viaforge.development.MovementTrace;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=EntityPlayerSP.class,priority=1100)
public class MovementPlayerProbe {
    @Inject(method="onLivingUpdate",at=@At("HEAD"),remap=false)
    private void begin(CallbackInfo ci){MovementTrace.sample(this,"LIVING_BEGIN");}
    @Inject(method="onLivingUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V"),remap=false)
    private void before(CallbackInfo ci){MovementTrace.sample(this,"INPUT_BEFORE");}
    @Inject(method="onLivingUpdate",at=@At(value="INVOKE",target="Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V",shift=At.Shift.AFTER),remap=false)
    private void after(CallbackInfo ci){MovementTrace.sample(this,"INPUT_AFTER");}
    @Inject(method="onLivingUpdate",at=@At("RETURN"),remap=false)
    private void end(CallbackInfo ci){MovementTrace.sample(this,"POSE_END");}
    @Inject(method="updateEntityActionState",at=@At("RETURN"),remap=false)
    private void travel(CallbackInfo ci){MovementTrace.sample(this,"TRAVEL_INPUT");}
    @Inject(method="setSprinting",at=@At("HEAD"),remap=false)
    private void sprintBefore(boolean value,CallbackInfo ci){MovementTrace.sample(this,"SPRINT_BEFORE_"+value);}
    @Inject(method="setSprinting",at=@At("RETURN"),remap=false)
    private void sprintAfter(boolean value,CallbackInfo ci){MovementTrace.sample(this,"SPRINT_AFTER_"+value);}
    @Inject(method="onUpdateWalkingPlayer",at=@At("HEAD"),remap=false)
    private void packets(CallbackInfo ci){MovementTrace.sample(this,"PACKETS_BEGIN");}
}
