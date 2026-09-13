package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.common.blocks.LegacyEntityPackets;
import com.viaversion.viaforge.items.ServerEntityViews;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinEntityVisualData {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void visualData(S3FPacketCustomPayload packet, CallbackInfo ci) throws Exception {
        if (!LegacyEntityPackets.CHANNEL.equals(packet.getChannelName())) return;
        PacketThreadUtil.checkThreadAndEnqueue(packet, (NetHandlerPlayClient)(Object)this, Minecraft.getMinecraft());
        try { ServerEntityViews.accept(packet.getBufferData()); }
        finally { packet.getBufferData().release(); }
        ci.cancel();
    }
}
