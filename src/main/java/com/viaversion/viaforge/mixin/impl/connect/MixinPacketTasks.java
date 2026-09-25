package com.viaversion.viaforge.mixin.impl.connect;

import com.viaversion.viaforge.compatibility.ClientPacketTasks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.*;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.util.IThreadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketThreadUtil.class)
public abstract class MixinPacketTasks {
    @Inject(method="checkThreadAndEnqueue", at=@At("HEAD"))
    private static <T extends INetHandler> void modernQueue(Packet<T> packet, T listener,
            IThreadListener executor, CallbackInfo ci) {
        if (!(executor instanceof Minecraft) || !(listener instanceof NetHandlerPlayClient)
                || executor.isCallingFromMinecraftThread()) return;
        NetworkManager connection = ((NetHandlerPlayClient) listener).getNetworkManager();
        if (!ClientPacketTasks.enabled(connection)) return;
        ClientPacketTasks.enqueue(connection, () -> {
            if (connection.isChannelOpen()) packet.processPacket(listener);
            else if (packet instanceof S3FPacketCustomPayload) {
                // Retained payloads own their buffer until the main-thread handler.
                PacketBuffer buffer = ((S3FPacketCustomPayload) packet).getBufferData();
                if (buffer.refCnt() > 0) buffer.release();
            }
        });
        throw ThreadQuickExitException.INSTANCE;
    }
}
