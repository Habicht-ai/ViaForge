package com.viaversion.viaforge.blocks;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyBedPacketsTest {
    @Test public void chunkTileColorsAndLaterUpdatesPreserveBothHalvesAndOccupiedState() throws Exception {
        for (BlockVersionProfile profile : new BlockVersionProfile[]{BlockVersionProfile.V1_12, BlockVersionProfile.V1_12_1, BlockVersionProfile.V1_12_2}) {
            LegacyBlockPackets packets = new LegacyBlockPackets(profile);
            ChunkSection[] sections = new ChunkSection[16]; sections[4] = new ChunkSectionImpl(true);
            sections[4].palette(PaletteType.BLOCKS).addId(0);
            sections[4].getLight().setBlockLight(new byte[2048]); sections[4].getLight().setSkyLight(new byte[2048]);
            List<CompoundTag> tags = new ArrayList<>();
            for (int color = 0; color < 16; color++) {
                int x = color % 4 * 4, z = color / 4 * 4;
                sections[4].palette(PaletteType.BLOCKS).setIdAt(z * 16 + x, 26 << 4 | 10);
                sections[4].palette(PaletteType.BLOCKS).setIdAt((z + 1) * 16 + x, 26 << 4 | 2);
                tags.add(tag(x - 16, 64, z + 32, color));
            }
            Chunk chunk = new BaseChunk(-1, 2, true, false, 16, sections, new int[256], tags);
            ByteBuf source = Unpooled.buffer(); Types.VAR_INT.writePrimitive(source, 0x20);
            new ChunkType1_9_3(true).write(source, chunk);
            try { packets.capture(source); } finally { source.release(); }
            for (int color = 0; color < 16; color++) {
                int x = color % 4 * 4 - 16, z = color / 4 * 4 + 32;
                assertEquals(LegacyBlockCatalog.bedState(10, color), packets.world().get(x, 64, z));
                assertEquals(LegacyBlockCatalog.bedState(2, color), packets.world().get(x, 64, z + 1));
            }
            packets.world().set(-16, 64, 32, 26 << 4 | 14);
            assertEquals(LegacyBlockCatalog.bedState(14, 0), packets.world().get(-16, 64, 32));
            ByteBuf change = tileUpdate(-16, 64, 32, 5);
            List<ByteBuf> changed;
            try { changed = packets.bedUpdate(change, state -> state); } finally { change.release(); }
            assertEquals(2, changed.size());
            for (ByteBuf packet : changed) packet.release();
            assertEquals(LegacyBlockCatalog.bedState(14, 5), packets.world().get(-16, 64, 32));
            assertEquals(LegacyBlockCatalog.bedState(2, 5), packets.world().get(-16, 64, 33));
            packets.world().set(-16, 64, 32, 0);
            packets.world().set(-16, 64, 32, 26 << 4 | 10);
            assertEquals(26 << 4 | 10, packets.world().get(-16, 64, 32));
            packets.world().unload(-1, 2);
            ByteBuf late = tileUpdate(-16, 64, 32, 3);
            try { assertTrue(packets.bedUpdate(late, state -> state).isEmpty()); } finally { late.release(); }
            assertEquals(0, packets.world().chunkCount());
        }
    }

    @Test public void oldProfilesAndInvalidColorsDoNotCreateColoredBeds() {
        ByteBuf update = tileUpdate(0, 64, 0, 5);
        try { assertNull(new LegacyBlockPackets(BlockVersionProfile.V1_11_2).bedUpdate(update, state -> state)); }
        finally { update.release(); }
        LegacyBlockWorld world = new LegacyBlockWorld(); world.replaceChunk(0, 0, true, new char[16][]);
        world.set(0, 64, 0, 26 << 4 | 8);
        assertFalse(world.setBedColor(0, 64, 0, 16)); assertFalse(world.setBedColor(0, 64, 0, -1));
        assertEquals(26 << 4 | 8, world.get(0, 64, 0));
    }

    private static CompoundTag tag(int x, int y, int z, int color) {
        CompoundTag tag = new CompoundTag(); tag.putString("id", "minecraft:bed");
        tag.putInt("x", x); tag.putInt("y", y); tag.putInt("z", z); tag.putInt("color", color); return tag;
    }
    private static ByteBuf tileUpdate(int x, int y, int z, int color) {
        ByteBuf packet = Unpooled.buffer(); Types.VAR_INT.writePrimitive(packet, 0x09);
        Types.BLOCK_POSITION1_8.write(packet, new BlockPosition(x, y, z)); packet.writeByte(11);
        Types.NAMED_COMPOUND_TAG.write(packet, tag(x, y, z, color)); return packet;
    }
}
