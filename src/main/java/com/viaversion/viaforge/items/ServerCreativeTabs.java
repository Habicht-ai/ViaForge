package com.viaversion.viaforge.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.blocks.*;
import net.minecraft.creativetab.CreativeTabs;

/** Vanilla category assignments; blocks without a tab remain obtainable through server commands. */
public final class ServerCreativeTabs {
    public static CreativeTabs materials() { return ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.MERGED_MATERIALS_TAB) ? CreativeTabs.tabMisc : CreativeTabs.tabMaterials; }
    public static CreativeTabs block(LegacyBlockCatalog.Definition block) {
        switch (block.kind) {
            case ROD: case CHORUS: case FLOWER: case SHULKER: case GLAZED: case BED: return CreativeTabs.tabDecorations;
            case OBSERVER: return CreativeTabs.tabRedstone;
            case CROP: return materials();
            case COMMAND: case STRUCTURE: case VOID: case GATEWAY: case ICE: case PATH: case DOUBLE_SLAB: return null;
            default: return CreativeTabs.tabBlock;
        }
    }
    public static CreativeTabs item(LegacyItemCatalog.Definition item) {
        if (item.id == 437) return CreativeTabs.tabBrewing;
        switch (item.kind) {
            case FOOD: case SOUP: return CreativeTabs.tabFood;
            case POTION: case SPLASH: case LINGERING: return CreativeTabs.tabBrewing;
            case SHIELD: case ARROW: case TIPPED_ARROW: case TOTEM: return CreativeTabs.tabCombat;
            case BOAT: case ELYTRA: return CreativeTabs.tabTransport;
            case HEAD: case CRYSTAL: return CreativeTabs.tabDecorations;
            case MATERIAL: return materials();
            case BOOK: return null;
            default: return CreativeTabs.tabMisc;
        }
    }
    private ServerCreativeTabs() { }
}
