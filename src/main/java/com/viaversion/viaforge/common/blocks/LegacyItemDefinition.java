package com.viaversion.viaforge.common.blocks;

/** Wire identity shared by imported block items and standalone items. */
public interface LegacyItemDefinition {
    int itemId();
    int itemData();
    int itemProtocol();
    default boolean preservesDamage() { return false; }
}
