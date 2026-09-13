package com.viaversion.viaforge.mixin.impl.boats;

import com.viaversion.viaforge.boats.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class MixinBoatWorld {
    @Unique private Entity viaForge$boatPassenger;
    @Redirect(method="updateEntities",at=@At(value="FIELD",target="Lnet/minecraft/entity/Entity;ridingEntity:Lnet/minecraft/entity/Entity;",opcode=180,ordinal=0))
    private Entity independentSeatTick(Entity rider) {
        // Bypass 1.8's single-passenger consistency check. Both seats are ticked
        // after their boat below, so the camera cannot lag behind by one tick.
        return rider.ridingEntity instanceof ServerBoat ? null:rider.ridingEntity;
    }
    @Inject(method="updateEntityWithOptionalForce",at=@At("HEAD"),cancellable=true)
    private void skipIndependentPassenger(Entity entity,boolean force,CallbackInfo ci) {
        if(entity.ridingEntity instanceof ServerBoat && entity!=viaForge$boatPassenger) ci.cancel();
    }
    @Redirect(method="updateEntityWithOptionalForce",at=@At(value="FIELD",target="Lnet/minecraft/entity/Entity;riddenByEntity:Lnet/minecraft/entity/Entity;",opcode=180,ordinal=0))
    private Entity tickBothSeats(Entity mount) {
        if(!(mount instanceof ServerBoat)) return mount.riddenByEntity;
        for(Entity passenger:new java.util.ArrayList<>(((ServerBoat)mount).passengers())) {
            if(passenger.isDead || passenger.ridingEntity!=mount) { ((ServerBoat)mount).detach(passenger); continue; }
            Entity previous=viaForge$boatPassenger;
            try { viaForge$boatPassenger=passenger; ((World)(Object)this).updateEntity(passenger); }
            finally { viaForge$boatPassenger=previous; }
        }
        return null;
    }
    @Inject(method="updateEntities",at=@At("HEAD"))
    private void resolveSeats(CallbackInfo ci) { if((Object)this instanceof WorldClient) ServerBoats.resolve((WorldClient)(Object)this); }
}
