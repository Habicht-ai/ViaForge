package com.viaversion.viaforge.common.compatibility;

import java.util.*;

/** Immutable, cumulative client rules; protocol parsing never uses this object. */
public final class VersionRules {
    public static final VersionRules NATIVE = new VersionRules(0, EnumSet.noneOf(ClientFeature.class), EnumSet.noneOf(ClientRule.class));
    private final int contentRevision;
    private final Set<ClientFeature> features;
    private final Set<ClientRule> rules;

    private VersionRules(int revision, Set<ClientFeature> features, Set<ClientRule> rules) {
        this.contentRevision=revision;
        this.features=Collections.unmodifiableSet(EnumSet.copyOf(features));
        this.rules=Collections.unmodifiableSet(EnumSet.copyOf(rules));
    }
    public Builder derive() { return new Builder(this); }
    public boolean has(ClientFeature feature) { return features.contains(feature); }
    public boolean enabled(ClientRule rule) { return rules.contains(rule); }
    /** Introduction revision of existing catalog content, not a server wire protocol. */
    public boolean contentSince(int revision) { return contentRevision >= revision; }
    public Set<ClientFeature> features() { return features; }
    public static final class Builder {
        private int revision;
        private final EnumSet<ClientFeature> features=EnumSet.noneOf(ClientFeature.class);
        private final EnumSet<ClientRule> rules=EnumSet.noneOf(ClientRule.class);
        private Builder(VersionRules parent) { revision=parent.contentRevision;features.addAll(parent.features);rules.addAll(parent.rules); }
        public Builder content(int revision) { if(revision<this.revision)throw new IllegalArgumentException("Content revisions cannot move backwards");this.revision=revision;return this; }
        public Builder enable(ClientFeature... values) { Collections.addAll(features,values);return this; }
        public Builder disable(ClientFeature feature) { features.remove(feature);return this; }
        public Builder rule(ClientRule rule,boolean enabled) { if(enabled)rules.add(rule);else rules.remove(rule);return this; }
        public VersionRules build() {
            require(ClientFeature.TWO_HANDS,ClientFeature.ITEMS,ClientFeature.ENTITY_VISUALS);
            require(ClientFeature.ELYTRA,ClientFeature.ITEMS,ClientFeature.ENTITY_VISUALS);
            require(ClientFeature.BOATS,ClientFeature.ITEMS,ClientFeature.ENTITY_VISUALS);
            require(ClientFeature.MOBS,ClientFeature.ENTITY_VISUALS);
            require(ClientFeature.TOTEM,ClientFeature.ITEMS,ClientFeature.ENTITY_VISUALS);
            require(ClientFeature.COOLDOWNS,ClientFeature.ITEMS);
            return new VersionRules(revision,features,rules);
        }
        private void require(ClientFeature feature,ClientFeature... dependencies) {
            if(features.contains(feature))for(ClientFeature dependency:dependencies)if(!features.contains(dependency))throw new IllegalStateException(feature+" requires "+dependency);
        }
    }
}
