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
    private final java.util.Set<Integer> boats = new java.util.HashSet<>();
    private Integer dimension;
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
            case "LOGIN":
                boats.clear();
                ByteBuf login = input.duplicate(); login.skipBytes(5);
                dimension = profile.hasIntJoinDimension() ? login.readInt() : (int)login.readByte();
                operation = 0; break;
            case "RESPAWN":
                int nextDimension = input.getInt(input.readerIndex());
                if (dimension == null || dimension != nextDimension) boats.clear();
                dimension = nextDimension; operation = 6; break;
            case "ADD_ENTITY":
                ByteBuf probe = input.duplicate(); int entityId = Types.VAR_INT.readPrimitive(probe); probe.skipBytes(16);
                boats.remove(entityId);
                int type = probe.readUnsignedByte();
                if (type == 1) { boats.add(entityId); operation = 20; break; }
                if (type == 67 || type == 93 || (type == 68 || type == 79) && profile.protocol() >= 315) { operation = 16; break; }
                if (type != 3 && type != 73 && type != 60 && type != 91 && type != 51) return null;
                operation = 1; break;
            case "SET_ENTITY_DATA": operation = 2; break;
            case "SET_EQUIPPED_ITEM": operation = 3; break;
            case "REMOVE_ENTITIES":
                ByteBuf removed = input.duplicate(); int count = Types.VAR_INT.readPrimitive(removed);
                for (int i = 0; i < count; i++) boats.remove(Types.VAR_INT.readPrimitive(removed));
                operation = 4; break;
            case "ADD_PLAYER": boats.remove(Types.VAR_INT.readPrimitive(input.duplicate())); operation = 5; break;
            case "UPDATE_ATTRIBUTES": operation = 10; break;
            case "ADD_MOB": boats.remove(Types.VAR_INT.readPrimitive(input.duplicate())); operation = 11; break;
            case "ENTITY_EVENT": operation = 12; break;
            case "SET_PASSENGERS": operation = 13; break;
            case "MOVE_ENTITY_POS": operation = 21; break;
            case "MOVE_ENTITY_POS_ROT": operation = 22; break;
            case "MOVE_ENTITY_ROT": operation = 23; break;
            case "TELEPORT_ENTITY": operation = 24; break;
            case "MOVE_VEHICLE": operation = 25; break;
            case "COOLDOWN": operation = 19; break;
            case "UPDATE_MOB_EFFECT": case "REMOVE_MOB_EFFECT":
                ByteBuf effect = input.duplicate(); Types.VAR_INT.readPrimitive(effect);
                int effectId = effect.readUnsignedByte();
                if (effectId < 24 || effectId > 27) return null;
                operation = packets[id].getName().equals("UPDATE_MOB_EFFECT") ? 17 : 18; break;
            default: return null;
        }
        if (operation >= 21 && operation <= 24 && !boats.contains(Types.VAR_INT.readPrimitive(input.duplicate()))) return null;
        return message(source, input, operation);
    }

    /** These events have a complete native implementation; running Via as well adds replacement effects. */
    public ByteBuf replacement(ByteBuf source) {
        ByteBuf input = source.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (id < 0 || id >= packets.length) return null;
        String name = packets[id].getName();
        if (name.equals("SOUND")) {
            String sound = MobSoundCatalog.name(profile.resourceVersion(),Types.VAR_INT.readPrimitive(input.duplicate()));
            if (sound != null && sound.startsWith("entity.")) return message(source,input,14);
        } else if (name.equals("CUSTOM_SOUND")) {
            String sound = Types.STRING.read(input.duplicate());
            if (sound.startsWith("minecraft:")) sound = sound.substring(10);
            if (sound.startsWith("entity.")) return message(source,input,15);
        }
        if (name.equals("LEVEL_EVENT")) {
            int event = input.getInt(input.readerIndex());
            if (event == 2002 || event == 2007 && profile.protocol() >= 315) return message(source, input, 7);
        } else if (name.equals("ENTITY_EVENT") && profile.protocol() >= 315) {
            if (input.getByte(input.readerIndex() + 4) == 35) return message(source, input, 8);
        } else if (name.equals("LEVEL_PARTICLES")) {
            int particle = input.getInt(input.readerIndex());
            if (particle == 42 || particle == 43 || particle == 45 || (particle == 47 || particle == 48) && profile.protocol() >= 315) return message(source, input, 9);
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
