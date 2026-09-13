package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.common.blocks.LegacyBlockPackets;
import com.viaversion.viaversion.api.minecraft.chunks.BaseChunk;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSectionImpl;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_8;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_1;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyBlockPacketsTest {
    @Test
    public void everyIntermediateProtocolPreservesOriginalStatesAndRestoresOnlyAvailableBlocks() throws Exception {
        for (BlockVersionProfile profile : BlockVersionProfile.values()) {
            LegacyBlockPackets packets = new LegacyBlockPackets(profile);
            ByteBuf join = packet(profile, "LOGIN");
            join.writeInt(7).writeByte(0);
            if (profile.hasIntJoinDimension()) join.writeInt(0); else join.writeByte(0);
            assertTrue(packets.capture(join));
            assertEquals(0, join.readerIndex());
            join.release();

            Chunk original = chunk(true);
            original.getSections()[0].palette(PaletteType.BLOCKS).setIdAt(0, 203 << 4 | 7);
            original.getSections()[0].palette(PaletteType.BLOCKS).setIdAt(1, 251 << 4 | 4);
            original.getSections()[0].palette(PaletteType.BLOCKS).setIdAt(2, 1 << 4);
            ByteBuf source = packet(profile, "LEVEL_CHUNK");
            if (profile.hasChunkBlockEntities()) new ChunkType1_9_3(true).write(source, original);
            else new ChunkType1_9_1(true).write(source, original);
            packets.capture(source);
            assertEquals(0, source.readerIndex());
            assertEquals(203 << 4 | 7, packets.world().get(-16, 0, 32));
            source.release();

            Chunk fallback = chunk(true);
            fallback.getSections()[0].palette(PaletteType.BLOCKS).setIdAt(0, 156 << 4 | 7);
            fallback.getSections()[0].palette(PaletteType.BLOCKS).setIdAt(1, 35 << 4 | 4);
            fallback.getSections()[0].palette(PaletteType.BLOCKS).setIdAt(2, 1 << 4);
            ByteBuf translated = Unpooled.buffer();
            Types.VAR_INT.writePrimitive(translated, 0x21);
            new ChunkType1_8(true).write(translated, fallback);
            ByteBuf output = packets.restore(translated, state -> state + 8192);
            assertEquals(0, translated.readerIndex());
            assertEquals(0x21, Types.VAR_INT.readPrimitive(output));
            Chunk restored = new ChunkType1_8(true).read(output);
            assertEquals((203 << 4 | 7) + 8192, restored.getSections()[0].palette(PaletteType.BLOCKS).idAt(0));
            assertEquals(profile.protocol() >= 335 ? (251 << 4 | 4) + 8192 : 35 << 4 | 4,
                    restored.getSections()[0].palette(PaletteType.BLOCKS).idAt(1));
            assertEquals(16, restored.getSections()[0].palette(PaletteType.BLOCKS).idAt(2));
            assertArrayEquals(fallback.getSections()[0].getLight().getBlockLight(), restored.getSections()[0].getLight().getBlockLight());
            assertArrayEquals(fallback.getBiomeData(), restored.getBiomeData());
            output.release();
            translated.release();
        }
        assertNull(BlockVersionProfile.forProtocol(47));
        assertNull(BlockVersionProfile.forProtocol(767));
    }

    @Test
    public void blockUpdatesAirRemovalUnloadAndDimensionChangesDoNotLeaveStaleOriginals() throws Exception {
        BlockVersionProfile profile = BlockVersionProfile.V1_12_2;
        LegacyBlockPackets packets = new LegacyBlockPackets(profile);
        packets.world().replaceChunk(-1, 2, true, new char[16][]);
        ByteBuf multi = packet(profile, "CHUNK_BLOCKS_UPDATE");
        multi.writeInt(-1).writeInt(2);
        Types.VAR_INT.writePrimitive(multi, 2);
        multi.writeShort(0x12ff); // x=1, z=2, y=255
        Types.VAR_INT.writePrimitive(multi, 235 << 4 | 3);
        multi.writeShort(0x2300);
        Types.VAR_INT.writePrimitive(multi, 251 << 4 | 15);
        packets.capture(multi);
        multi.release();
        assertEquals(235 << 4 | 3, packets.world().get(-15, 255, 34));
        assertEquals(251 << 4 | 15, packets.world().get(-14, 0, 35));

        ByteBuf translated = Unpooled.buffer();
        Types.VAR_INT.writePrimitive(translated, 0x22);
        translated.writeInt(-1).writeInt(2);
        Types.VAR_INT.writePrimitive(translated, 1);
        translated.writeShort(0x12ff);
        Types.VAR_INT.writePrimitive(translated, 16);
        ByteBuf output = packets.restore(translated, state -> 6000);
        Types.VAR_INT.readPrimitive(output);
        output.skipBytes(8);
        assertEquals(1, Types.VAR_INT.readPrimitive(output));
        assertEquals(0x12ff, output.readUnsignedShort());
        assertEquals(6000, Types.VAR_INT.readPrimitive(output));
        output.release();
        translated.release();

        ByteBuf single = packet(profile, "BLOCK_UPDATE");
        Types.BLOCK_POSITION1_8.write(single, new com.viaversion.viaversion.api.minecraft.BlockPosition(-15, 255, 34));
        Types.VAR_INT.writePrimitive(single, 0);
        packets.capture(single);
        single.release();
        assertEquals(0, packets.world().get(-15, 255, 34));

        ByteBuf respawn = packet(profile, "RESPAWN");
        respawn.writeInt(0);
        packets.capture(respawn);
        assertEquals(1, packets.world().chunkCount()); // same-dimension respawn retains chunks
        respawn.setInt(respawn.writerIndex() - 4, -1);
        packets.capture(respawn);
        assertEquals(0, packets.world().chunkCount());
        respawn.release();

        packets.world().replaceChunk(-1, 2, true, new char[16][]);
        ByteBuf unload = packet(profile, "FORGET_LEVEL_CHUNK");
        unload.writeInt(-1).writeInt(2);
        packets.capture(unload);
        unload.release();
        assertEquals(0, packets.world().chunkCount());
    }

    @Test
    public void netherChunkDoesNotExpectSkylight() throws Exception {
        LegacyBlockPackets packets = new LegacyBlockPackets(BlockVersionProfile.V1_9);
        ByteBuf join = packet(BlockVersionProfile.V1_9, "LOGIN");
        join.writeInt(1).writeByte(0).writeByte(-1);
        packets.capture(join);
        join.release();
        Chunk chunk = chunk(false);
        chunk.getSections()[0].palette(PaletteType.BLOCKS).setIdAt(0, 201 << 4);
        ByteBuf source = packet(BlockVersionProfile.V1_9, "LEVEL_CHUNK");
        new ChunkType1_9_1(false).write(source, chunk);
        packets.capture(source);
        source.release();
        assertEquals(201 << 4, packets.world().get(-16, 0, 32));
    }

    @Test
    public void explosionOffsetsUseTruncationAtNegativeCoordinates() throws Exception {
        LegacyBlockPackets packets = new LegacyBlockPackets(BlockVersionProfile.V1_12_2);
        packets.world().replaceChunk(-1, 0, true, new char[16][]);
        packets.world().set(-2, 64, 2, 201 << 4);
        packets.world().set(-3, 64, 2, 201 << 4);
        ByteBuf explosion = packet(BlockVersionProfile.V1_12_2, "EXPLODE");
        explosion.writeFloat(-2.75F).writeFloat(64.5F).writeFloat(2.5F).writeFloat(4F).writeInt(1);
        explosion.writeByte(0).writeByte(0).writeByte(0);
        packets.capture(explosion);
        explosion.release();
        assertEquals(0, packets.world().get(-2, 64, 2));
        assertEquals(201 << 4, packets.world().get(-3, 64, 2));
    }

    private static ByteBuf packet(BlockVersionProfile profile, String name) {
        ClientboundPacketType[] types = profile.protocol() >= 338 ? ClientboundPackets1_12_1.values()
                : profile.protocol() >= 335 ? ClientboundPackets1_12.values()
                : profile.protocol() >= 110 ? ClientboundPackets1_9_3.values() : ClientboundPackets1_9.values();
        for (ClientboundPacketType type : types) {
            if (type.getName().equals(name)) {
                ByteBuf buffer = Unpooled.buffer();
                Types.VAR_INT.writePrimitive(buffer, type.getId());
                return buffer;
            }
        }
        throw new AssertionError(name);
    }

    private static Chunk chunk(boolean sky) {
        ChunkSection[] sections = new ChunkSection[16];
        sections[0] = new ChunkSectionImpl(true);
        sections[0].palette(PaletteType.BLOCKS).addId(0);
        sections[0].getLight().setBlockLight(new byte[2048]);
        if (sky) sections[0].getLight().setSkyLight(new byte[2048]);
        return new BaseChunk(-1, 2, true, false, 1, sections, new int[256], new ArrayList<>());
    }
}
