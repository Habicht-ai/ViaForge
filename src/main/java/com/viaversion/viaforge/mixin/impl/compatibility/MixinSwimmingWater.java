package com.viaversion.viaforge.mixin.impl.compatibility;
import com.viaversion.viaforge.compatibility.ServerSwimming;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.material.Material;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class MixinSwimmingWater {
    @Shadow protected boolean inWater;
    @Shadow protected boolean firstUpdate;
    @Shadow protected abstract void resetHeight();
    @Inject(method="doBlockCollisions",at=@At("RETURN"))
    private void bubbleColumns(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if(!com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.DEFERRED_BLOCK_EFFECTS))ServerSwimming.bubbles((Entity)(Object)this);
    }
    @Inject(method="handleWaterMovement",at=@At("HEAD"),cancellable=true)
    private void originalWater(CallbackInfoReturnable<Boolean> result){
        Entity entity=(Entity)(Object)this;if(!ServerSwimming.enabled(entity))return;
        boolean next=ServerSwimming.water((EntityPlayer)entity);
        if(next){if(!inWater&&!firstUpdate)resetHeight();entity.fallDistance=0;entity.extinguish();}
        inWater=next;result.setReturnValue(next);
    }
    @Inject(method="isInsideOfMaterial",at=@At("HEAD"),cancellable=true)
    private void originalEyes(Material material,CallbackInfoReturnable<Boolean> result){
        Entity entity=(Entity)(Object)this;
        if(material==Material.water&&ServerSwimming.enabled(entity))result.setReturnValue(ServerSwimming.eye(entity));
    }
}
