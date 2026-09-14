package com.viaversion.viaforge.hands;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.*;
import net.minecraft.network.play.client.*;

/** Native hand-aware operations enter Via immediately above its 1.8 adapter. */
public final class HandPackets {
    public static final String CHANNEL="VF|hands";
    private static PacketBuffer buffer(int op){PacketBuffer b=new PacketBuffer(Unpooled.buffer());b.writeByte(op);return b;}
    public static C17PacketCustomPayload action(int action){PacketBuffer b=buffer(0);b.writeByte(action);return new C17PacketCustomPayload(CHANNEL,b);}
    public static C17PacketCustomPayload wrapped(Packet packet,int hand) {
        int op=packet instanceof C08PacketPlayerBlockPlacement?1:packet instanceof C02PacketUseEntity?2:packet instanceof C0APacketAnimation?3:packet instanceof C0EPacketClickWindow?4:packet instanceof C10PacketCreativeInventoryAction?5:6;
        PacketBuffer b=buffer(op);b.writeByte(hand);
        try{packet.writePacketData(b);}catch(java.io.IOException e){b.release();throw new IllegalStateException(e);}
        return new C17PacketCustomPayload(CHANNEL,b);
    }
    public static void send(Packet packet){Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(packet);}
    public static void translate(PacketWrapper wrapper) {
        com.viaversion.viaforge.common.compatibility.ExtensionPackets.translate(CHANNEL,wrapper);
    }
    private HandPackets() { }
}
