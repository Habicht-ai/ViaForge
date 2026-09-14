package com.viaversion.viaforge.boats;

import com.viaversion.viaforge.platform.ViaForgeProtocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.*;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;

/** Private native packets enter Via at the 1.9 boundary, retaining compression and encryption. */
public final class BoatPackets {
    public static final String CHANNEL="VF|boat";
    public static C17PacketCustomPayload movement(ServerBoat boat) {
        PacketBuffer data=new PacketBuffer(Unpooled.buffer()); data.writeByte(0);
        data.writeDouble(boat.posX).writeDouble(boat.posY).writeDouble(boat.posZ).writeFloat(boat.rotationYaw).writeFloat(boat.rotationPitch);
        return new C17PacketCustomPayload(CHANNEL,data);
    }
    public static C17PacketCustomPayload rowing(boolean left,boolean right) {
        PacketBuffer data=new PacketBuffer(Unpooled.buffer()); data.writeByte(1).writeBoolean(left).writeBoolean(right); return new C17PacketCustomPayload(CHANNEL,data);
    }
    public static C17PacketCustomPayload input(float strafe,float forward,boolean jump,boolean sneak) {
        PacketBuffer data=new PacketBuffer(Unpooled.buffer()); data.writeByte(2).writeFloat(strafe).writeFloat(forward).writeByte((jump?1:0)|(sneak?2:0)); return new C17PacketCustomPayload(CHANNEL,data);
    }
    public static void move(ServerBoat boat) { if(boat.controlled()) Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(movement(boat)); }
    public static void paddles(ServerBoat boat) { if(boat.controlled()) Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(rowing(boat.paddle(0),boat.paddle(1))); }
    public static void register(ViaForgeProtocol protocol) {
        protocol.registerServerbound(ServerboundPackets1_8.CUSTOM_PAYLOAD, wrapper -> {
            String channel=wrapper.passthrough(Types.STRING);
            if(CHANNEL.equals(channel)||com.viaversion.viaforge.hands.HandPackets.CHANNEL.equals(channel))
                com.viaversion.viaforge.common.compatibility.ExtensionPackets.translate(channel,wrapper);
        });
    }
    private BoatPackets() { }
}
