package com.viaversion.viaforge.development.mixin;

import com.viaversion.viaforge.development.MovementTrace;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Passive packet ordering observation; never consumes, delays or substitutes packets. */
@Mixin(value=NetHandlerPlayClient.class,priority=500)
public abstract class MovementPacketProbe {
    @Inject(method="handleEntityMetadata",at=@At("HEAD"))
    private void metadataBegin(S1CPacketEntityMetadata packet,CallbackInfo ci) {
        observeMetadata(packet,"LEGACY_METADATA_BEGIN");
    }
    @Inject(method="handleEntityMetadata",at=@At("RETURN"))
    private void metadataEnd(S1CPacketEntityMetadata packet,CallbackInfo ci) {
        observeMetadata(packet,"LEGACY_METADATA_END");
    }
    private void observeMetadata(S1CPacketEntityMetadata packet,String phase) {
        MovementTrace.metadata(packet,phase);
    }
    @Inject(method="handleCustomPayload",at=@At("HEAD"))
    private void received(S3FPacketCustomPayload packet,CallbackInfo ci) {
        MovementTrace.packet("CUSTOM_RECEIVE",packet.getChannelName(),packet.getBufferData());
    }
    @Inject(method="addToSendQueue",at=@At("HEAD"))
    private void sent(Packet packet,CallbackInfo ci) {
        if(packet instanceof C17PacketCustomPayload) {
            C17PacketCustomPayload custom=(C17PacketCustomPayload)packet;
            MovementTrace.packet("CUSTOM_SEND",custom.getChannelName(),custom.getBufferData());
        }
    }
}
