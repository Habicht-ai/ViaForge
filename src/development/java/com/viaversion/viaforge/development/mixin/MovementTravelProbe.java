package com.viaversion.viaforge.development.mixin;
import com.viaversion.viaforge.development.MovementTrace;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=EntityPlayer.class,priority=900)
public class MovementTravelProbe {
    @Inject(method="moveEntityWithHeading",at=@At("HEAD"),remap=false)
    private void travel(float side,float forward,CallbackInfo ci){MovementTrace.sample(this,"PHYSICS_BEGIN");}
    @Inject(method="moveEntityWithHeading",at=@At("RETURN"),remap=false)
    private void end(float side,float forward,CallbackInfo ci){MovementTrace.sample(this,"PHYSICS_END");}
}
