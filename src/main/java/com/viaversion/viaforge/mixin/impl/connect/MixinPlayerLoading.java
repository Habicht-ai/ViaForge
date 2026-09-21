package com.viaversion.viaforge.mixin.impl.connect;
import com.viaversion.viaforge.compatibility.ServerSession;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Do not simulate the placeholder spawn while the actual world/first teleport is loading. */
@Mixin(EntityPlayerSP.class)
public abstract class MixinPlayerLoading {
    @ModifyConstant(method="onUpdateWalkingPlayer",constant=@Constant(intValue=20),require=1)
    private int positionReminder(int nativeValue) {
        return ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.EARLY_POSITION_REMINDER)?19:nativeValue;
    }
    @Inject(method="onUpdateWalkingPlayer",at=@At("HEAD"))
    private void inputs(CallbackInfo ci) {
        com.viaversion.viaforge.compatibility.NativeClientTicks.send((EntityPlayerSP)(Object)this,false);
    }
    @Inject(method="onUpdate",at=@At("HEAD"),cancellable=true)
    private void loading(CallbackInfo ci) {
        EntityPlayerSP player=(EntityPlayerSP)(Object)this;
        if(ServerSession.awaitingWorld(player.sendQueue)) ci.cancel();
    }
}
