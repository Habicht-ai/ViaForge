package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyBlockCatalogTest {
    @Test public void coversEveryNewLegacyBlockIdAndRespectsReleaseBoundaries() {
        for (int id = 198; id <= 252; id++) assertNotNull("Missing block " + id, LegacyBlockCatalog.state(id << 4));
        assertNotNull(LegacyBlockCatalog.state(255 << 4));
        assertNull(LegacyBlockCatalog.state(253 << 4));
        assertNull(LegacyBlockCatalog.state(254 << 4));
        for (int id = 198; id <= 212; id++) assertTrue(BlockVersionProfile.V1_9.supportsState(id << 4));
        assertFalse(BlockVersionProfile.V1_9_4.supportsState(213 << 4));
        for (int id = 213; id <= 217; id++) assertTrue(BlockVersionProfile.V1_10_2.supportsState(id << 4));
        assertFalse(BlockVersionProfile.V1_10_2.supportsState(218 << 4));
        for (int id = 218; id <= 234; id++) assertTrue(BlockVersionProfile.V1_11.supportsState(id << 4));
        assertFalse(BlockVersionProfile.V1_11_2.supportsState(235 << 4));
        for (int id = 235; id <= 252; id++) assertTrue(BlockVersionProfile.V1_12.supportsState(id << 4));
    }
    @Test public void preservesOrientationAgeAndColorButRejectsInvalidStates() {
        for (int state : new int[]{198 << 4 | 5, 200 << 4 | 5, 202 << 4 | 8, 205 << 4 | 8, 207 << 4 | 3,
                210 << 4 | 13, 212 << 4 | 3, 218 << 4 | 13, 219 << 4 | 5, 251 << 4 | 15, 252 << 4 | 15}) {
            assertTrue("State " + state, BlockVersionProfile.V1_12_2.supportsState(state));
        }
        for (int state : new int[]{-1, LegacyBlockCatalog.STATE_LIMIT, 198 << 4 | 6, 200 << 4 | 6, 202 << 4 | 12, 205 << 4 | 1,
                207 << 4 | 4, 218 << 4 | 14, 219 << 4 | 6}) assertFalse(BlockVersionProfile.V1_12_2.supportsState(state));
        assertEquals(435, LegacyBlockCatalog.state(207 << 4).itemId());
        assertEquals(-1, LegacyBlockCatalog.state(209 << 4).itemId());
        assertEquals(-1, LegacyBlockCatalog.state(204 << 4).itemId());
        assertEquals(15, LegacyBlockCatalog.state(252 << 4 | 15).itemData());
    }
    @Test public void coloredBedsStartIn112AndKeepColorSeparateFromOrientation() {
        for (int color = 0; color < 16; color++) for (int meta : new int[]{0, 1, 2, 3, 8, 9, 10, 11, 12, 13, 14, 15}) {
            int state = LegacyBlockCatalog.bedState(meta, color);
            LegacyBlockCatalog.Definition bed = LegacyBlockCatalog.state(state);
            assertEquals(color, bed.color); assertEquals(26, bed.id);
            assertEquals(355, bed.itemId()); assertEquals(color, bed.itemData());
            assertFalse(BlockVersionProfile.V1_11_2.supportsState(state));
            assertTrue(BlockVersionProfile.V1_12.supportsState(state));
        }
        assertEquals(14, LegacyBlockCatalog.state(26 << 4).color);
        assertFalse(BlockVersionProfile.V1_11_2.supportsState(26 << 4));
    }
}
