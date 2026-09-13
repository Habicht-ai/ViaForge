package com.viaversion.viaforge.boats;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.platform.ViaForgeProtocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.*;
import com.viaversion.viarewind.protocol.v1_9to1_8.Protocol1_9To1_8;
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
            String channel=wrapper.passthrough(Types.STRING); if(!CHANNEL.equals(channel)) return;
            wrapper.cancel();
            if(BlockVersionProfile.forProtocol(wrapper.user().getProtocolInfo().serverProtocolVersion().getVersion())==null) return;
            int operation=wrapper.read(Types.UNSIGNED_BYTE);
            ServerboundPackets1_9 type=operation==0 ? ServerboundPackets1_9.MOVE_VEHICLE:operation==1 ? ServerboundPackets1_9.PADDLE_BOAT:ServerboundPackets1_9.PLAYER_INPUT;
            PacketWrapper modern=PacketWrapper.create(type,wrapper.user());
            if(operation==0) { modern.write(Types.DOUBLE,wrapper.read(Types.DOUBLE)); modern.write(Types.DOUBLE,wrapper.read(Types.DOUBLE)); modern.write(Types.DOUBLE,wrapper.read(Types.DOUBLE)); modern.write(Types.FLOAT,wrapper.read(Types.FLOAT)); modern.write(Types.FLOAT,wrapper.read(Types.FLOAT)); }
            else if(operation==1) { modern.write(Types.BOOLEAN,wrapper.read(Types.BOOLEAN)); modern.write(Types.BOOLEAN,wrapper.read(Types.BOOLEAN)); }
            else if(operation==2) { modern.write(Types.FLOAT,wrapper.read(Types.FLOAT)); modern.write(Types.FLOAT,wrapper.read(Types.FLOAT)); modern.write(Types.UNSIGNED_BYTE,wrapper.read(Types.UNSIGNED_BYTE)); }
            else return;
            modern.sendToServer(Protocol1_9To1_8.class,true);
        });
    }
    private BoatPackets() { }
}
