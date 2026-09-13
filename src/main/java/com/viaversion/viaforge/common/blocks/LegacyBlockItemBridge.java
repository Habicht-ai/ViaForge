package com.viaversion.viaforge.common.blocks;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import java.util.List;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;

/** Uses Via's reversible item mappings, preserving original names, metadata and NBT. */
public final class LegacyBlockItemBridge {
    private final IntBinaryOperator localItem;
    private final IntFunction<? extends LegacyItemDefinition> serverItem;
    public LegacyBlockItemBridge(IntBinaryOperator localItem, IntFunction<? extends LegacyItemDefinition> serverItem) {
        this.localItem = localItem; this.serverItem = serverItem;
    }
    public Item toClient(UserConnection user, Item fallback) {
        BlockVersionProfile profile = profile(user);
        if (fallback == null || profile == null) return fallback;
        // Each Via layer stores its original id/data and any display-name changes in NBT.
        // Undo those transformations on a copy to recover the untouched server item.
        Item original = fallback.copy();
        for (Protocol pipe : user.getProtocolInfo().getPipeline().pipes()) {
            if (eligible(pipe)) original = pipe.getItemRewriter().handleItemToServer(user, original);
        }
        if (original == null) return fallback;
        int localId = localItem.applyAsInt(original.identifier(), original.data());
        LegacyItemDefinition block = localId < 0 ? null : serverItem.apply(localId);
        if (block == null) return nativeEnchantments(original, profile) ? original : fallback;
        if (profile.protocol() < block.itemProtocol()) return fallback;
        original.setIdentifier(localId);
        if (!block.preservesDamage()) original.setData((short) 0);
        return original;
    }
    public Item toServer(UserConnection user, Item local) {
        if (local == null) return null;
        LegacyItemDefinition block = serverItem.apply(local.identifier());
        BlockVersionProfile profile = profile(user);
        if (block == null && !nativeEnchantments(local, profile)) return local;
        // A stack carried over from another server must never send a local Forge id.
        if (profile == null || block != null && profile.protocol() < block.itemProtocol()) return null;
        Item fallback = local.copy();
        if (block != null) {
            fallback.setIdentifier(block.itemId());
            if (!block.preservesDamage()) fallback.setData((short) block.itemData());
        }
        List<Protocol> pipes = user.getProtocolInfo().getPipeline().pipes();
        for (int i = pipes.size() - 1; i >= 0; i--) {
            Protocol pipe = pipes.get(i);
            if (eligible(pipe)) fallback = pipe.getItemRewriter().handleItemToClient(user, fallback);
        }
        // The ordinary serverbound pipeline now restores this to the server item.
        return fallback;
    }
    private boolean eligible(Protocol pipe) {
        return pipe.getItemRewriter() != null && !(pipe.getItemRewriter() instanceof ClientBlockItemRewriter);
    }
    // Vanilla equipment also needs the actual enchantments instead of Via's fallback lore.
    static boolean nativeEnchantments(Item item, BlockVersionProfile profile) {
        if (profile == null || item.tag() == null || item.identifier() > 431) return false;
        for (String key : new String[]{"ench", "StoredEnchantments"}) {
            com.viaversion.nbt.tag.ListTag<com.viaversion.nbt.tag.CompoundTag> enchantments = item.tag().getListTag(key, com.viaversion.nbt.tag.CompoundTag.class);
            if (enchantments == null) continue;
            for (com.viaversion.nbt.tag.CompoundTag enchantment : enchantments) {
                int id = enchantment.getShort("id");
                if (id == 9 || id == 70 || profile.protocol() >= 315 && (id == 10 || id == 71) || profile.protocol() >= 316 && id == 22) return true;
            }
        }
        return false;
    }
    private BlockVersionProfile profile(UserConnection user) {
        return BlockVersionProfile.forProtocol(user.getProtocolInfo().getServerProtocolVersion());
    }
}
