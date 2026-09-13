package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class MobCatalogTest {
    @Test public void mobTypesFollowTheirActualReleaseBoundaries() {
        Map<Integer, Object> data = Collections.emptyMap();
        assertNull(MobKind.resolve(47, 69, data));
        assertEquals(MobKind.SHULKER, MobKind.resolve(107, 69, data));
        assertNull(MobKind.resolve(110, 102, data));
        assertEquals(MobKind.POLAR_BEAR, MobKind.resolve(210, 102, data));
        assertNull(MobKind.resolve(210, 103, data));
        assertEquals(MobKind.LLAMA, MobKind.resolve(315, 103, data));
        assertNull(MobKind.resolve(316, 105, data));
        assertNull(MobKind.resolve(316, 37, data));
        assertEquals(MobKind.PARROT, MobKind.resolve(335, 105, data));
        assertEquals(MobKind.ILLUSIONER, MobKind.resolve(335, 37, data));
    }
    @Test public void preSplitVariantsUseMetadataAndItsVersionedOffset() {
        for (int protocol : new int[]{107,108,109,110,210}) {
            int first = protocol >= 210 ? 12 : 11;
            assertEquals(MobKind.WITHER_SKELETON, MobKind.resolve(protocol,51,Collections.<Integer,Object>singletonMap(first,1)));
            assertEquals(MobKind.ELDER_GUARDIAN, MobKind.resolve(protocol,68,Collections.<Integer,Object>singletonMap(first,(byte)4)));
            MobKind[] horses = {MobKind.DONKEY,MobKind.MULE,MobKind.ZOMBIE_HORSE,MobKind.SKELETON_HORSE};
            for (int variant=1;variant<=4;variant++) assertEquals(horses[variant-1], MobKind.resolve(protocol,100,Collections.<Integer,Object>singletonMap(first+2,variant)));
            assertEquals(protocol >= 210 ? MobKind.HUSK : MobKind.ZOMBIE, MobKind.resolve(protocol,54,Collections.<Integer,Object>singletonMap(first+1,6)));
        }
        assertEquals(MobKind.SKELETON, MobKind.resolve(315,51,Collections.<Integer,Object>singletonMap(12,1)));
        assertEquals(MobKind.WITHER_SKELETON, MobKind.resolve(315,5,Collections.<Integer,Object>emptyMap()));
    }
    @Test public void soundRegistryContainsEveryNumericEventOncePerProfile() {
        for (BlockVersionProfile profile : BlockVersionProfile.values()) {
            String version = profile.resourceVersion();
            int expected = profile.protocol() >= 335 ? 549 : profile.protocol() >= 315 ? 493 : profile.protocol() >= 210 ? 463 : profile.protocol() == 107 ? 443 : 444;
            Set<String> events = new HashSet<>();
            for (int id = 0; id < expected; id++) {
                String sound = MobSoundCatalog.name(version,id);
                assertNotNull(version + " sound " + id, sound);
                assertTrue(version + " duplicate " + sound, events.add(sound));
            }
            assertNull(MobSoundCatalog.name(version,expected));
            assertNull(MobSoundCatalog.name(version,-1));
            assertTrue(events.contains("entity.shulker.ambient"));
            assertEquals(profile.protocol() >= 335, events.contains("entity.parrot.ambient"));
        }
    }
}
