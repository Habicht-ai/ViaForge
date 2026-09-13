package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.LegacyBlockWorld;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyBlockWorldTest {
    @Test
    public void fullChunksReplaceOldSectionsWhilePartialChunksPreserveOthers() {
        LegacyBlockWorld world = new LegacyBlockWorld();
        char[][] first = new char[16][];
        first[0] = new char[4096];
        first[0][4095] = (char) (203 << 4 | 7);
        world.replaceChunk(-2, 3, true, first);
        first[0][4095] = 0;
        assertEquals(203 << 4 | 7, world.get(-17, 15, 63));
        assertEquals(0, world.get(-17, 255, 63));

        char[][] patch = new char[16][];
        patch[15] = new char[4096];
        patch[15][4095] = (char) (251 << 4 | 15);
        world.replaceChunk(-2, 3, false, patch);
        assertEquals(203 << 4 | 7, world.get(-17, 15, 63));
        assertEquals(251 << 4 | 15, world.get(-17, 255, 63));

        world.replaceChunk(-2, 3, true, new char[16][]);
        assertEquals(0, world.get(-17, 15, 63));
        assertEquals(0, world.get(-17, 255, 63));
        world.unload(-2, 3);
        assertEquals(-1, world.get(-17, 15, 63));
        assertEquals(0, world.chunkCount());
    }

    @Test
    public void unknownSectionsAndStrayUpdatesAreNotMistakenForAir() {
        LegacyBlockWorld world = new LegacyBlockWorld();
        world.set(0, 0, 0, 123);
        assertEquals(0, world.chunkCount());
        world.replaceChunk(0, 0, false, new char[16][]);
        world.set(0, 0, 0, 201 << 4);
        assertEquals(201 << 4, world.get(0, 0, 0));
        assertEquals(-1, world.get(1, 0, 0));
        assertEquals(-1, world.get(0, -1, 0));
        assertEquals(-1, world.get(0, 256, 0));
        world.clear();
        assertEquals(0, world.chunkCount());
    }
}
