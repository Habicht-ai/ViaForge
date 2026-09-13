package com.viaversion.viaforge.mixin.impl.boats;

import com.viaversion.viaforge.boats.ServerBoat;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinBoatPassengers {
    @Inject(method="setAngles",at=@At("TAIL"))
    private void boatView(float yaw,float pitch,CallbackInfo ci) {
        Entity rider=(Entity)(Object)this;
        if(rider.ridingEntity instanceof ServerBoat) ((ServerBoat)rider.ridingEntity).orientPassenger(rider);
    }
    @Inject(method="updateRidden",at=@At("HEAD"),cancellable=true)
    private void modernSeat(CallbackInfo ci) {
        Entity rider=(Entity)(Object)this;
        if(!(rider.ridingEntity instanceof ServerBoat)) return;
        ServerBoat boat=(ServerBoat)rider.ridingEntity;
        if(boat.isDead) boat.detach(rider);
        else { rider.motionX=rider.motionY=rider.motionZ=0; rider.onUpdate(); if(rider.ridingEntity==boat) boat.positionPassenger(rider,true); }
        ci.cancel();
    }
    @Inject(method="mountEntity",at=@At("HEAD"),cancellable=true)
    private void changeSeat(Entity target,CallbackInfo ci) {
        Entity rider=(Entity)(Object)this;
        if(rider.ridingEntity instanceof ServerBoat) ((ServerBoat)rider.ridingEntity).detach(rider);
        if(target instanceof ServerBoat) { // Actual acceptance and seat order arrive in SET_PASSENGERS.
            ci.cancel();
        }
    }
}
