package com.viaversion.viaforge.blocks.gui;

import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.BlockPos;

/** Native 1.9 command and 1.10-1.12 structure plugin messages, carried through Via unchanged. */
public final class BlockEditorPackets {
    public static C17PacketCustomPayload command(BlockPos pos, NBTTagCompound data) {
        String command = data.getString("Command");
        if (command.getBytes(StandardCharsets.UTF_8).length > 32500) throw new IllegalArgumentException("command_length");
        String mode = data.getString("mode");
        choice(mode, "REDSTONE", "AUTO", "SEQUENCE");
        PacketBuffer buffer = position(pos);
        buffer.writeString(command);
        buffer.writeBoolean(data.getBoolean("TrackOutput"));
        buffer.writeString(mode);
        buffer.writeBoolean(data.getBoolean("conditional"));
        buffer.writeBoolean(data.getBoolean("auto"));
        return new C17PacketCustomPayload("MC|AutoCmd", buffer);
    }

    public static C17PacketCustomPayload structure(BlockPos pos, int action, NBTTagCompound data) {
        if (action < 1 || action > 4) throw new IllegalArgumentException("action");
        choice(data.getString("mode"), "SAVE", "LOAD", "CORNER", "DATA");
        choice(data.getString("mirror"), "NONE", "LEFT_RIGHT", "FRONT_BACK");
        choice(data.getString("rotation"), "NONE", "CLOCKWISE_90", "CLOCKWISE_180", "COUNTERCLOCKWISE_90");
        if (data.getString("name").length() > 64 || data.getString("metadata").length() > 128) throw new IllegalArgumentException("name");
        for (String axis : new String[]{"X", "Y", "Z"}) {
            range(data.getInteger("pos" + axis), -32, 32);
            range(data.getInteger("size" + axis), 0, 32);
        }
        float integrity = data.getFloat("integrity");
        if (!Float.isFinite(integrity) || integrity < 0 || integrity > 1) throw new IllegalArgumentException("numbers");
        PacketBuffer buffer = position(pos);
        buffer.writeByte(action);
        buffer.writeString(data.getString("mode"));
        buffer.writeString(data.getString("name"));
        for (String key : new String[]{"posX", "posY", "posZ", "sizeX", "sizeY", "sizeZ"}) buffer.writeInt(data.getInteger(key));
        buffer.writeString(data.getString("mirror"));
        buffer.writeString(data.getString("rotation"));
        buffer.writeString(data.getString("metadata"));
        buffer.writeBoolean(data.getBoolean("ignoreEntities"));
        buffer.writeBoolean(data.getBoolean("showair"));
        buffer.writeBoolean(data.getBoolean("showboundingbox"));
        buffer.writeFloat(integrity);
        long seed = data.getLong("seed");
        do { int part = (int) (seed & 127); seed >>>= 7; buffer.writeByte(seed == 0 ? part : part | 128); } while (seed != 0);
        return new C17PacketCustomPayload("MC|Struct", buffer);
    }

    private static PacketBuffer position(BlockPos pos) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeInt(pos.getX()).writeInt(pos.getY()).writeInt(pos.getZ());
        return buffer;
    }
    private static void choice(String value, String... choices) {
        for (String choice : choices) if (value.equals(choice)) return;
        throw new IllegalArgumentException("mode");
    }
    private static void range(int value, int min, int max) { if (value < min || value > max) throw new IllegalArgumentException("numbers"); }
    private BlockEditorPackets() { }
}
