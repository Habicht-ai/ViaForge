package com.viaversion.viaforge.common.blocks;

/** Legacy wire layouts and their default resource releases.
 * Global connection support is selected by CompatibilityRegistry, not this enum. */
public enum BlockVersionProfile {
    V1_9(107, "1.9"),
    V1_9_1(108, "1.9.1"),
    V1_9_2(109, "1.9.2"),
    V1_9_4(110, "1.9.4"),
    V1_10_2(210, "1.10.2"),
    V1_11(315, "1.11"),
    V1_11_2(316, "1.11.2"),
    V1_12(335, "1.12"),
    V1_12_1(338, "1.12.1"),
    V1_12_2(340, "1.12.2");

    private final int protocol;
    private final String resourceVersion;

    BlockVersionProfile(int protocol, String resourceVersion) {
        this.protocol = protocol;
        this.resourceVersion = resourceVersion;
    }

    public int protocol() { return protocol; }
    public String resourceVersion() { return resourceVersion; }
    public boolean hasChunkBlockEntities() { return protocol >= 110; }
    public boolean hasIntJoinDimension() { return protocol >= 108; }

    /** Only states that exist in this server release are restored. */
    public boolean supportsState(int state) {
        LegacyBlockCatalog.Definition block = LegacyBlockCatalog.state(state);
        return block != null && protocol >= block.protocol;
    }

    public static BlockVersionProfile forProtocol(int protocol) {
        for (BlockVersionProfile profile : values()) {
            if (profile.protocol == protocol) return profile;
        }
        return null;
    }
}
