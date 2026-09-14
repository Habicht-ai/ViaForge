package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.*;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class MixinHandInput {
    @Shadow private int rightClickDelayTimer;
    @Inject(method="rightClickMouse",at=@At("HEAD"),cancellable=true)
    private void bothHands(CallbackInfo ci){if(Offhand.active()){rightClickDelayTimer=4;HandActions.rightClick();ci.cancel();}}
    @Inject(method="clickMouse",at=@At("HEAD"))
    private void attackHand(CallbackInfo ci){if(Offhand.active())Offhand.swingHand=0;}
}
