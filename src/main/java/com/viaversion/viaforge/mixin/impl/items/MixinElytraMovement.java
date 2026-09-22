package com.viaversion.viaforge.mixin.impl.items;
import com.viaversion.viaforge.items.ServerElytraFlight;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(EntityPlayer.class)
public abstract class MixinElytraMovement {
    @Inject(method="moveEntityWithHeading", at=@At("HEAD"), cancellable=true)
    private void glide(float strafe, float forward, CallbackInfo ci) { if (com.viaversion.viaforge.compatibility.ServerSwimming.move((EntityPlayer)(Object)this,strafe,forward)||ServerElytraFlight.move((EntityPlayer)(Object)this)) ci.cancel(); }
    @Inject(method="getEyeHeight", at=@At("HEAD"), cancellable=true)
    private void flightCamera(CallbackInfoReturnable<Float> ci) {
        EntityPlayer player=(EntityPlayer)(Object)this;
        if (ServerElytraFlight.flying(player)||ServerElytraFlight.compact(player)||com.viaversion.viaforge.compatibility.ServerSwimming.pose(player)) ci.setReturnValue(.4F);
        else if(ServerElytraFlight.crouching(player)) ci.setReturnValue(player.height==1.5F?1.27F:1.54F);
    }
}
