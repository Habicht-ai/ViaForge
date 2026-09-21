package com.viaversion.viaforge.mixin.impl.connect;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.task.PlayerPacketsTickTask;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.PlayerStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PlayerPacketsTickTask.class,remap=false)
public abstract class MixinClientTickTask {
    @Inject(method="run(Lcom/viaversion/viaversion/api/connection/UserConnection;Lcom/viaversion/viabackwards/protocol/v1_21_2to1_21/storage/PlayerStorage;)V",at=@At("HEAD"),cancellable=true)
    private void actualClientClock(UserConnection connection,PlayerStorage storage,CallbackInfo ci) {
        if(connection.isClientSide())ci.cancel();
    }
}
