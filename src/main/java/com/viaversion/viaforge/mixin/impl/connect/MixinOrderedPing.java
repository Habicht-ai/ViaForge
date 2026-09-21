package com.viaversion.viaforge.mixin.impl.connect;

import com.viaversion.viaforge.common.compatibility.OrderedPing;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinOrderedPing {
    @Inject(method="handleCustomPayload",at=@At("HEAD"),cancellable=true)
    private void acknowledge(S3FPacketCustomPayload packet,CallbackInfo ci) {
        if(!OrderedPing.CHANNEL.equals(packet.getChannelName()))return;
        NetHandlerPlayClient handler=(NetHandlerPlayClient)(Object)this;
        PacketThreadUtil.checkThreadAndEnqueue(packet,handler,Minecraft.getMinecraft());
        try {
            PacketBuffer response=new PacketBuffer(Unpooled.buffer(4));
            response.writeInt(packet.getBufferData().readInt());
            handler.addToSendQueue(new C17PacketCustomPayload(OrderedPing.CHANNEL,response));
        }finally{packet.getBufferData().release();}
        ci.cancel();
    }
}
