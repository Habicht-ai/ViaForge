package com.viaversion.viaforge.common.blocks;

import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_8;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_1;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import io.netty.buffer.ByteBuf;
import java.util.function.IntUnaryOperator;
import com.viaversion.nbt.tag.CompoundTag;
import java.util.ArrayList;
import java.util.List;

/** Reads untouched 1.9-1.12.2 data and restores supported states in translated 1.8 packets. */
public final class LegacyBlockPackets {
    private final BlockVersionProfile profile;
    private final LegacyBlockWorld world = new LegacyBlockWorld();
    private final ClientboundPacketType[] packetTypes;
    private int dimension;
    private int fallingEntity = -1, fallingState = -1;
    private final List<CompoundTag> editorChunkData = new ArrayList<>();

    public LegacyBlockPackets(BlockVersionProfile profile) {
        this.profile = profile;
        packetTypes = profile.protocol() >= 338 ? ClientboundPackets1_12_1.values()
                : profile.protocol() >= 335 ? ClientboundPackets1_12.values()
                : profile.protocol() >= 110 ? ClientboundPackets1_9_3.values() : ClientboundPackets1_9.values();
    }

    public LegacyBlockWorld world() { return world; }

    /** Keep complete editor NBT before Via drops unsupported tile types or command fields. */
    public ByteBuf editorUpdate(ByteBuf original) {
        ByteBuf input = original.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (id < 0 || id >= packetTypes.length || !packetTypes[id].getName().equals("BLOCK_ENTITY_DATA")) return null;
        BlockPosition pos = Types.BLOCK_POSITION1_8.read(input);
        int type = input.readUnsignedByte();
        if (type == 0 || type != editorType(pos)) return null;
        return editorPacket(original, pos, type, Types.NAMED_COMPOUND_TAG.read(input));
    }

    public List<ByteBuf> editorChunkUpdates(ByteBuf original) {
        List<ByteBuf> result = new ArrayList<>();
        try {
            for (CompoundTag tag : editorChunkData) {
                BlockPosition pos = new BlockPosition(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                result.add(editorPacket(original, pos, editorType(pos), tag));
            }
            return result;
        } catch (Throwable failure) {
            for (ByteBuf packet : result) packet.release();
            throw failure;
        } finally { editorChunkData.clear(); }
    }

    private int editorType(BlockPosition pos) {
        LegacyBlockCatalog.Definition definition = LegacyBlockCatalog.state(world.get(pos.x(), pos.y(), pos.z()));
        if (definition == null) return 0;
        if (definition.kind == LegacyBlockCatalog.Kind.COMMAND) return 2;
        if (definition.kind == LegacyBlockCatalog.Kind.GATEWAY) return 8;
        return definition.kind == LegacyBlockCatalog.Kind.STRUCTURE && profile.protocol() >= 210 ? 7 : 0;
    }

    private static ByteBuf editorPacket(ByteBuf original, BlockPosition pos, int type, CompoundTag tag) {
        ByteBuf output = original.alloc().buffer();
        try {
            Types.VAR_INT.writePrimitive(output, ClientboundPackets1_8.BLOCK_ENTITY_DATA.getId());
            Types.BLOCK_POSITION1_8.write(output, pos);
            output.writeByte(type);
            Types.NAMED_COMPOUND_TAG.write(output, tag);
            return output;
        } catch (Throwable failure) { output.release(); throw failure; }
    }

    /** Replace 1.12's bed tile update with native block updates, before Via drops it. */
    public List<ByteBuf> bedUpdate(ByteBuf original, IntUnaryOperator localState) {
        if (profile.protocol() < 335) return null;
        ByteBuf input = original.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (id < 0 || id >= packetTypes.length || !packetTypes[id].getName().equals("BLOCK_ENTITY_DATA")) return null;
        BlockPosition pos = Types.BLOCK_POSITION1_8.read(input);
        if (input.readUnsignedByte() != 11) return null;
        CompoundTag tag = Types.NAMED_COMPOUND_TAG.read(input);
        List<BlockPosition> changed = applyBedColor(pos, tag);
        List<ByteBuf> updates = new ArrayList<>();
        for (BlockPosition changedPos : changed) {
            int mapped = map(world.get(changedPos.x(), changedPos.y(), changedPos.z()), localState);
            if (mapped < 0) continue;
            ByteBuf output = original.alloc().buffer();
            Types.VAR_INT.writePrimitive(output, ClientboundPackets1_8.BLOCK_UPDATE.getId());
            Types.BLOCK_POSITION1_8.write(output, changedPos);
            Types.VAR_INT.writePrimitive(output, mapped);
            updates.add(output);
        }
        return updates;
    }

    private List<BlockPosition> applyBedColor(BlockPosition pos, CompoundTag tag) {
        List<BlockPosition> changed = new ArrayList<>();
        if (tag == null || !tag.contains("color")) return changed;
        int color = tag.getInt("color");
        int state = world.get(pos.x(), pos.y(), pos.z());
        if (!world.setBedColor(pos.x(), pos.y(), pos.z(), color)) return changed;
        changed.add(pos);
        // Some servers only send the head's tile data. Color its matching partner
        // as well, including when that partner lies in a neighboring loaded chunk.
        int direction = state & 3, sign = (state & 8) == 0 ? 1 : -1;
        int dx = direction == 1 ? -sign : direction == 3 ? sign : 0;
        int dz = direction == 0 ? sign : direction == 2 ? -sign : 0;
        BlockPosition other = new BlockPosition(pos.x() + dx, pos.y(), pos.z() + dz);
        int partner = world.get(other.x(), other.y(), other.z());
        if (LegacyBlockCatalog.isBedState(partner) && (partner & 11) == ((state & 11) ^ 8)
                && world.setBedColor(other.x(), other.y(), other.z(), color)) changed.add(other);
        return changed;
    }

    /** New block entities need their original block events even if Via would cancel them. */
    public ByteBuf blockEvent(ByteBuf original, IntUnaryOperator localState) {
        ByteBuf input = original.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (id < 0 || id >= packetTypes.length || !packetTypes[id].getName().equals("BLOCK_EVENT")) return null;
        BlockPosition pos = Types.BLOCK_POSITION1_8.read(input);
        int event = input.readUnsignedByte(), value = input.readUnsignedByte();
        int block = Types.VAR_INT.readPrimitive(input);
        int local = map(block << 4, localState);
        if (local < 0) return null;
        ByteBuf output = original.alloc().buffer();
        Types.VAR_INT.writePrimitive(output, ClientboundPackets1_8.BLOCK_EVENT.getId());
        Types.BLOCK_POSITION1_8.write(output, pos);
        output.writeByte(event).writeByte(value);
        Types.VAR_INT.writePrimitive(output, local >> 4); // Native palette: registry block id << 4 | metadata
        return output;
    }

    /** Does not change the supplied buffer's indexes or contents. Returns true for a join. */
    public boolean capture(ByteBuf original) throws Exception {
        fallingEntity = fallingState = -1;
        editorChunkData.clear();
        ByteBuf input = original.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (id < 0 || id >= packetTypes.length) return false;
        switch (packetTypes[id].getName()) {
            case "ADD_ENTITY":
                int entity = Types.VAR_INT.readPrimitive(input);
                input.skipBytes(16); // UUID
                int type = input.readUnsignedByte();
                input.skipBytes(26); // double coordinates, pitch and yaw
                int data = input.readInt();
                if (type == 70) {
                    fallingEntity = entity;
                    fallingState = (data & 4095) << 4 | (data >>> 12 & 15);
                }
                break;
            case "LOGIN":
                input.skipBytes(5); // entity id, game mode
                dimension = profile.hasIntJoinDimension() ? input.readInt() : input.readByte();
                world.clear();
                return true;
            case "RESPAWN":
                int nextDimension = input.readInt();
                if (nextDimension != dimension) world.clear();
                dimension = nextDimension;
                break;
            case "LEVEL_CHUNK":
                Chunk chunk = profile.hasChunkBlockEntities() ? new ChunkType1_9_3(dimension == 0).read(input)
                        : new ChunkType1_9_1(dimension == 0).read(input);
                char[][] sections = new char[16][];
                for (int s = 0; s < sections.length; s++) {
                    ChunkSection section = chunk.getSections()[s];
                    if (section == null) continue;
                    sections[s] = new char[4096];
                    DataPalette palette = section.palette(PaletteType.BLOCKS);
                    for (int i = 0; i < 4096; i++) sections[s][i] = (char) palette.idAt(i);
                }
                world.replaceChunk(chunk.getX(), chunk.getZ(), chunk.isFullChunk(), sections);
                for (CompoundTag tag : chunk.getBlockEntities()) {
                    BlockPosition editorPos = new BlockPosition(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                    if (editorPos.x() >> 4 == chunk.getX() && editorPos.z() >> 4 == chunk.getZ() && editorType(editorPos) != 0) editorChunkData.add(tag);
                }
                if (profile.protocol() >= 335) for (CompoundTag tag : chunk.getBlockEntities()) {
                    if (!"minecraft:bed".equals(tag.getString("id")) && !"Bed".equals(tag.getString("id"))) continue;
                    int x = tag.getInt("x"), y = tag.getInt("y"), z = tag.getInt("z");
                    if (x >> 4 == chunk.getX() && z >> 4 == chunk.getZ()) applyBedColor(new BlockPosition(x, y, z), tag);
                }
                break;
            case "BLOCK_UPDATE":
                BlockPosition pos = Types.BLOCK_POSITION1_8.read(input);
                world.set(pos.x(), pos.y(), pos.z(), Types.VAR_INT.readPrimitive(input));
                break;
            case "CHUNK_BLOCKS_UPDATE":
                int chunkX = input.readInt(), chunkZ = input.readInt();
                int count = Types.VAR_INT.readPrimitive(input);
                checkCount(count, input.readableBytes() / 3);
                for (int i = 0; i < count; i++) {
                    int packed = input.readUnsignedShort();
                    world.set((chunkX << 4) + (packed >> 12), packed & 255,
                            (chunkZ << 4) + ((packed >> 8) & 15), Types.VAR_INT.readPrimitive(input));
                }
                break;
            case "FORGET_LEVEL_CHUNK":
                world.unload(input.readInt(), input.readInt());
                break;
            case "EXPLODE":
                // Legacy explosion offsets use truncation, including at negative coordinates.
                int x = (int) input.readFloat();
                int y = (int) input.readFloat();
                int z = (int) input.readFloat();
                input.skipBytes(4); // strength
                int affected = input.readInt();
                checkCount(affected, input.readableBytes() / 3);
                for (int i = 0; i < affected; i++) world.set(x + input.readByte(), y + input.readByte(), z + input.readByte(), 0);
                break;
            default:
                break;
        }
        return false;
    }

    /** A newly allocated result belongs to the caller; null means keep Via's output. */
    public ByteBuf restore(ByteBuf translated, IntUnaryOperator localState) throws Exception {
        ByteBuf input = translated.duplicate();
        int id = Types.VAR_INT.readPrimitive(input);
        if (id == ClientboundPackets1_8.ADD_ENTITY.getId() && fallingEntity >= 0) {
            int entity = Types.VAR_INT.readPrimitive(input);
            int type = input.readUnsignedByte();
            int mapped = map(fallingState, localState);
            if (entity != fallingEntity || type != 70 || mapped < 0) return null;
            input.skipBytes(14); // native fixed-point coordinates, pitch and yaw
            ByteBuf result = translated.copy();
            result.setInt(input.readerIndex() - translated.readerIndex(), (mapped >> 4) | (mapped & 15) << 12);
            return result;
        }
        if (id != ClientboundPackets1_8.LEVEL_CHUNK.getId()
                && id != ClientboundPackets1_8.BLOCK_UPDATE.getId()
                && id != ClientboundPackets1_8.CHUNK_BLOCKS_UPDATE.getId()) return null;
        ByteBuf output = translated.alloc().buffer(translated.readableBytes());
        try {
            Types.VAR_INT.writePrimitive(output, id);
            if (id == ClientboundPackets1_8.LEVEL_CHUNK.getId()) {
                Chunk chunk = new ChunkType1_8(dimension == 0).read(input);
                // Full chunks with a zero mask are 1.8 unload packets.
                if (!(chunk.isFullChunk() && chunk.getBitmask() == 0)) {
                    for (int s = 0; s < 16; s++) {
                        ChunkSection section = chunk.getSections()[s];
                        if (section == null) continue;
                        DataPalette palette = section.palette(PaletteType.BLOCKS);
                        for (int i = 0; i < 4096; i++) {
                            int original = world.get((chunk.getX() << 4) + (i & 15), (s << 4) + (i >> 8),
                                    (chunk.getZ() << 4) + ((i >> 4) & 15));
                            int mapped = map(original, localState);
                            if (mapped >= 0) palette.setIdAt(i, mapped);
                        }
                    }
                }
                new ChunkType1_8(dimension == 0).write(output, chunk);
            } else if (id == ClientboundPackets1_8.BLOCK_UPDATE.getId()) {
                BlockPosition pos = Types.BLOCK_POSITION1_8.read(input);
                Types.BLOCK_POSITION1_8.write(output, pos);
                int fallback = Types.VAR_INT.readPrimitive(input);
                int mapped = map(world.get(pos.x(), pos.y(), pos.z()), localState);
                Types.VAR_INT.writePrimitive(output, mapped >= 0 ? mapped : fallback);
            } else {
                int x = input.readInt(), z = input.readInt();
                output.writeInt(x).writeInt(z);
                int count = Types.VAR_INT.readPrimitive(input);
                checkCount(count, input.readableBytes() / 3);
                Types.VAR_INT.writePrimitive(output, count);
                for (int i = 0; i < count; i++) {
                    int packed = input.readUnsignedShort();
                    output.writeShort(packed);
                    int fallback = Types.VAR_INT.readPrimitive(input);
                    int mapped = map(world.get((x << 4) + (packed >> 12), packed & 255,
                            (z << 4) + ((packed >> 8) & 15)), localState);
                    Types.VAR_INT.writePrimitive(output, mapped >= 0 ? mapped : fallback);
                }
            }
            output.writeBytes(input);
            return output;
        } catch (Throwable error) {
            output.release();
            throw error;
        }
    }

    private int map(int state, IntUnaryOperator mapper) {
        return state >= 0 && profile.supportsState(state) ? mapper.applyAsInt(state) : -1;
    }

    private static void checkCount(int count, int maximum) {
        if (count < 0 || count > maximum) throw new IllegalArgumentException("Invalid block update count: " + count);
    }
}
