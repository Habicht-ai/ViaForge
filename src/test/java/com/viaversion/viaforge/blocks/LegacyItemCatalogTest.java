package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyItemCatalogTest {
    @Test public void coversEveryNewVanillaItemIdWithoutDuplicatingSeeds() {
        Set<Integer> added = new HashSet<>();
        for (LegacyItemCatalog.Definition item : LegacyItemCatalog.ITEMS) assertTrue("Duplicate item " + item.id, added.add(item.id));
        assertTrue(added.contains(426));
        for (int id = 432; id <= 453; id++) {
            if (id == 451) assertFalse(added.contains(id));
            else if (id == 435) assertEquals(435, LegacyBlockCatalog.state(207 << 4).itemId());
            else assertTrue("Missing item " + id, added.contains(id));
        }
        assertFalse(added.contains(435));
        assertEquals(5, item(397).itemData());
    }
    @Test public void preservesReleaseBoundariesAndDurability() {
        assertEquals(315, item(449).itemProtocol());
        assertEquals(315, item(450).itemProtocol());
        assertEquals(316, item(452).itemProtocol());
        assertEquals(335, item(453).itemProtocol());
        assertEquals(336, item(442).durability);
        assertEquals(432, item(443).durability);
        for (LegacyItemCatalog.Definition item : LegacyItemCatalog.ITEMS) {
            assertEquals(item.id == 442 || item.id == 443, item.preservesDamage());
            assertTrue(item.stackSize == 1 || item.stackSize == 64);
            if (item.id < 449) assertEquals(107, item.protocol);
        }
    }
    private static LegacyItemCatalog.Definition item(int id) {
        for (LegacyItemCatalog.Definition item : LegacyItemCatalog.ITEMS) if (item.id == id) return item;
        throw new AssertionError("Missing item " + id);
    }
}
