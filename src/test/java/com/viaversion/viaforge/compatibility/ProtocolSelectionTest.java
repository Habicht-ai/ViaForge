package com.viaversion.viaforge.compatibility;
import com.viaversion.viaforge.common.ProtocolSelection;
import com.viaversion.viaaprilfools.api.AprilFoolsProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProtocolSelectionTest {
    @Test public void jokesAreExcludedWithoutCollidingWithReleaseProtocolIds() {
        for(ProtocolVersion joke:AprilFoolsProtocolVersion.APRIL_FOOLS_PROTOCOLS) {
            assertFalse(ProtocolSelection.selectable(joke));
            assertFalse(ProtocolSelection.versions().contains(joke));
        }
        assertTrue(ProtocolSelection.selectable(AprilFoolsProtocolVersion.sCombatTest8c));
        assertTrue(ProtocolSelection.selectable(ProtocolVersion.v1_21_5));
        assertTrue(ProtocolSelection.selectable(ProtocolVersion.v1_8));
        assertTrue(ProtocolSelection.selectable(ProtocolVersion.v26_3));
        assertEquals(777,ProtocolVersion.v26_3.getVersion());
    }
}
