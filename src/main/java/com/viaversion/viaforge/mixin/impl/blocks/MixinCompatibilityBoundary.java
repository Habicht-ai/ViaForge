package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.common.compatibility.FlattenedProtocolAdapter;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.packet.*;
import com.viaversion.viaversion.exception.CancelException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the actual pass through Via; never replays a protocol on a probe. */
@Mixin(value = AbstractProtocol.class, remap = false)
public abstract class MixinCompatibilityBoundary {
    @Inject(method = "transform", at = @At("HEAD"), cancellable = true)
    private void structure(Direction direction, State state, PacketWrapper packet, CallbackInfo ci) throws Exception {
        if (direction == Direction.SERVERBOUND && state == State.PLAY
                && com.viaversion.viaforge.common.compatibility.FlattenedServerboundPackets.translate(this,packet)) ci.cancel();
    }
    @Inject(method = "transform", at = @At("HEAD"))
    private void preserve(Direction direction, State state, PacketWrapper packet, CallbackInfo ci) throws CancelException {
        if (direction != Direction.CLIENTBOUND || state != State.PLAY) return;
        FlattenedProtocolAdapter adapter = packet.user().get(FlattenedProtocolAdapter.class);
        if (adapter != null && adapter.beforeProtocol(this, packet)) throw CancelException.generate();
    }
}
