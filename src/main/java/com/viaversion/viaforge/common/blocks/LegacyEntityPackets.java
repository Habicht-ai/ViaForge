package com.viaversion.viaforge.common.blocks;

import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.*;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import io.netty.buffer.ByteBuf;

/** Carries original visual data through the native packet queue, after Via translation. */
public final class LegacyEntityPackets {
    public static final String CHANNEL = "VF|entity";
    private final BlockVersionProfile profile;
    private final ClientboundPacketType[] packets;
    public LegacyEntityPackets(BlockVersionProfile profile) {
        this.profile = profile;
        packets = profile.protocol() >= 338 ? ClientboundPackets1_12_1.values()
                : profile.protocol() >= 335 ? ClientboundPackets1_12.values()
                : profile.protocol() >= 110 ? ClientboundPackets1_9_3.values() : ClientboundPackets1_9.values();
    }
    public ByteBuf capture(ByteBuf source) {
        ByteBuf input = source.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (id < 0 || id >= packets.length) return null;
        int operation;
        switch (packets[id].getName()) {
            case "LOGIN": operation = 0; break;
            case "RESPAWN": operation = 6; break;
            case "ADD_ENTITY":
                ByteBuf probe = input.duplicate(); Types.VAR_INT.readPrimitive(probe); probe.skipBytes(16);
                int type = probe.readUnsignedByte();
                if (type != 3 && type != 73 && type != 60 && type != 91) return null;
                operation = 1; break;
            case "SET_ENTITY_DATA": operation = 2; break;
            case "SET_EQUIPPED_ITEM": operation = 3; break;
            case "REMOVE_ENTITIES": operation = 4; break;
            case "ADD_PLAYER": operation = 5; break;
            default: return null;
        }
        return message(source, input, operation);
    }

    /** These events have a complete native implementation; running Via as well adds replacement effects. */
    public ByteBuf replacement(ByteBuf source) {
        ByteBuf input = source.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (id < 0 || id >= packets.length) return null;
        String name = packets[id].getName();
        if (name.equals("LEVEL_EVENT")) {
            int event = input.getInt(input.readerIndex());
            if (event == 2002 || event == 2007 && profile.protocol() >= 315) return message(source, input, 7);
        } else if (name.equals("ENTITY_EVENT") && profile.protocol() >= 315) {
            if (input.getByte(input.readerIndex() + 4) == 35) return message(source, input, 8);
        } else if (name.equals("LEVEL_PARTICLES") && profile.protocol() >= 315) {
            if (input.getInt(input.readerIndex()) == 47) return message(source, input, 9);
        }
        return null;
    }

    private ByteBuf message(ByteBuf source, ByteBuf input, int operation) {
        ByteBuf output = source.alloc().buffer();
        try {
            Types.VAR_INT.writePrimitive(output, ClientboundPackets1_8.CUSTOM_PAYLOAD.getId());
            Types.STRING.write(output, CHANNEL);
            output.writeShort(profile.protocol()).writeByte(operation);
            if (operation != 0) output.writeBytes(input);
            return output;
        } catch (Throwable failure) { output.release(); throw failure; }
    }
}
