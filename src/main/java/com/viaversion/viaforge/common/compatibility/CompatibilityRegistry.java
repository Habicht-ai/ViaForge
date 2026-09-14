package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.connection.UserConnection;
import java.util.*;

/** Exact codec registration; inheritance happens in VersionRules, never by rounding wire IDs. */
public final class CompatibilityRegistry {
    public static final CompatibilityRegistry DEFAULT = LegacyCompatibility.create();
    private final Map<Integer,CompatibilityProfile> profiles;
    public CompatibilityRegistry(Collection<CompatibilityProfile> profiles) {
        Map<Integer,CompatibilityProfile> map=new HashMap<>();
        for(CompatibilityProfile profile:profiles)if(map.put(profile.serverProtocol(),profile)!=null)throw new IllegalArgumentException("Duplicate protocol "+profile.serverProtocol());
        this.profiles=Collections.unmodifiableMap(map);
    }
    public CompatibilityProfile resolve(int protocol) { CompatibilityProfile profile=profiles.get(protocol);return profile==null?CompatibilityProfile.unsupported(protocol):profile; }
    public static CompatibilityProfile forUser(UserConnection user) {
        int protocol=user.getProtocolInfo().serverProtocolVersion().getVersion();
        CompatibilityProfile profile=user.get(CompatibilityProfile.class);
        return profile!=null&&profile.serverProtocol()==protocol?profile:DEFAULT.resolve(protocol);
    }
}
