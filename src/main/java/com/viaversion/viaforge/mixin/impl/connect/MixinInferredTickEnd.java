package com.viaversion.viaforge.mixin.impl.connect;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter.EntityPacketRewriter1_21_2;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.PlayerStorage;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** ViaBackwards 5.12 also infers boundaries before movement; our client supplies the actual end. */
@Mixin(value=EntityPacketRewriter1_21_2.class,remap=false)
public abstract class MixinInferredTickEnd {
    @Inject(method="endPreviousTick",at=@At("HEAD"),cancellable=true)
    private void nativeBoundary(PacketWrapper packet,PlayerStorage storage,CallbackInfo ci) {
        if(packet.user().isClientSide())ci.cancel();
    }
}
