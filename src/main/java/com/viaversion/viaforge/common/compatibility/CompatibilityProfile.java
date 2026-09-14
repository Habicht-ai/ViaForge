package com.viaversion.viaforge.common.compatibility;

import java.util.Objects;

/** Real server target, inherited client behavior and exact assets are separate axes. */
public final class CompatibilityProfile implements com.viaversion.viaversion.api.connection.StorableObject {
    private final int serverProtocol;
    private final VersionRules rules;
    private final ResourceProfile resources;
    private final ProtocolAdapterFactory adapter;
    public CompatibilityProfile(int serverProtocol,VersionRules rules,ResourceProfile resources,ProtocolAdapterFactory adapter) {
        this.serverProtocol=serverProtocol;this.rules=Objects.requireNonNull(rules);this.resources=resources;this.adapter=adapter;
        if(!rules.features().isEmpty() && (adapter==null||resources==null))throw new IllegalArgumentException("Client extensions require an explicit codec and resources");
        if(adapter!=null&&!adapter.capabilities().containsAll(rules.features()))throw new IllegalArgumentException("Codec "+adapter.id()+" cannot supply requested features");
    }
    public static CompatibilityProfile unsupported(int protocol) { return new CompatibilityProfile(protocol,VersionRules.NATIVE,null,null); }
    public int serverProtocol() { return serverProtocol; }
    public VersionRules rules() { return rules; }
    public ResourceProfile resources() { return resources; }
    public ProtocolAdapterFactory adapter() { return adapter; }
    public boolean has(ClientFeature feature) { return rules.has(feature); }
    public boolean extended() { return adapter!=null&&!rules.features().isEmpty(); }
}
