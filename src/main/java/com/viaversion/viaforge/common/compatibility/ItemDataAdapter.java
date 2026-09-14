package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.minecraft.item.Item;

/** Maps server identities to stable client catalog identities and back, including NBT.
 * The current catalog uses the legacy id/data namespace internally. This is not
 * permission to write those IDs onto a newer server's wire. Implementations own copies.
 */
public interface ItemDataAdapter {
    Item toClientData(Item serverItem);
    Item toServerData(Item clientItem);
    ItemDataAdapter LEGACY = new ItemDataAdapter() {
        public Item toClientData(Item item) { return item==null?null:item.copy(); }
        public Item toServerData(Item item) { return item==null?null:item.copy(); }
    };
}
