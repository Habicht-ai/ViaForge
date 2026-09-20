package com.viaversion.viaforge.common;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaaprilfools.api.AprilFoolsProtocolVersion;
import java.util.*;
/** Use the library's authoritative joke-version registry; retain combat tests and legacy releases. */
public final class ProtocolSelection {
    public static boolean selectable(ProtocolVersion version) {
        return !AprilFoolsProtocolVersion.APRIL_FOOLS_PROTOCOLS.contains(version);
    }
    public static List<ProtocolVersion> versions() {
        List<ProtocolVersion> versions = new ArrayList<>();
        for (ProtocolVersion version : ProtocolVersion.getReversedProtocols()) if (selectable(version)) versions.add(version);
        return Collections.unmodifiableList(versions);
    }
    private ProtocolSelection() { }
}
