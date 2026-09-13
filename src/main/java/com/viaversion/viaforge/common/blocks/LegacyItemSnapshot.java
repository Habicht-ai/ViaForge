package com.viaversion.viaforge.common.blocks;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaforge.items.ClientItems;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;

/** Some fallback mappings lose item data. Snapshots are removed before either endpoint sees them. */
public final class LegacyItemSnapshot {
    private static final String KEY = "ViaForge|originalItem";
    public static void capture(UserConnection user, Item item, Object boundary) {
        BlockVersionProfile profile = BlockVersionProfile.forProtocol(user.getProtocolInfo().getServerProtocolVersion());
        if (item == null || profile == null) return;
        // At this boundary the item is in this layer's server format. Undo the higher layers
        // on a probe to determine whether our native registry can display it.
        Item probe = item.copy(); boolean higher = false;
        for (Protocol pipe : user.getProtocolInfo().getPipeline().pipes()) {
            if (pipe.getItemRewriter() == boundary) { higher = true; continue; }
            if (higher && pipe.getItemRewriter() != null) probe = pipe.getItemRewriter().handleItemToServer(user, probe);
        }
        if (probe == null) return;
        LegacyItemDefinition definition = ClientItems.serverItem(ClientItems.localItem(probe.identifier(), probe.data()));
        if (definition == null || profile.protocol() < definition.itemProtocol()) return;
        CompoundTag snapshot = new CompoundTag(); snapshot.putInt("id", item.identifier()); snapshot.putShort("data", item.data());
        if (item.tag() != null) {
            CompoundTag original = item.tag().copy(); original.remove(KEY);
            snapshot.put("tag", original);
        }
        if (item.tag() == null) item.setTag(new CompoundTag());
        CompoundTag snapshots = item.tag().getCompoundTag(KEY);
        if (snapshots == null) { snapshots = new CompoundTag(); item.tag().put(KEY, snapshots); }
        snapshots.put(boundary.getClass().getSimpleName(), snapshot);
    }
    public static void restore(Item item, Object boundary) {
        if (item == null || item.tag() == null) return;
        CompoundTag snapshots = item.tag().getCompoundTag(KEY);
        if (snapshots == null) return;
        CompoundTag snapshot = snapshots.getCompoundTag(boundary.getClass().getSimpleName());
        if (snapshot == null || !snapshot.contains("id") || !snapshot.contains("data")) return;
        item.setIdentifier(snapshot.getInt("id")); item.setData(snapshot.getShort("data"));
        // Preserve the current amount (inventory clicks can split a stack).
        CompoundTag tag = snapshot.getCompoundTag("tag"); item.setTag(tag == null ? null : tag.copy());
        snapshots.remove(boundary.getClass().getSimpleName());
        if (!snapshots.isEmpty()) {
            if (item.tag() == null) item.setTag(new CompoundTag());
            item.tag().put(KEY, snapshots);
        }
    }
    private LegacyItemSnapshot() { }
}
