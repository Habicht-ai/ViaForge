package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import java.util.List;

/** Uses the installed, bidirectional item codecs, including Damage, enchantments,
 * JSON names, banner colors and patch-specific flattened IDs. Only copies are rewritten. */
public final class FlattenedItemDataAdapter implements ItemDataAdapter {
    public Item toClientData(Item item) { throw new IllegalStateException("Flattened items require their connection"); }
    public Item toServerData(Item item) { throw new IllegalStateException("Flattened items require their connection"); }
    public Item toClientData(UserConnection user, Item item) {
        if (item == null) return null;
        Item result = item.copy();
        List<Protocol> pipes = user.getProtocolInfo().getPipeline().pipes();
        for (int i = pipes.size() - 1; i >= boundary(pipes); i--) {
            Protocol pipe = pipes.get(i);
            if (pipe.getItemRewriter() != null) result = pipe.getItemRewriter().handleItemToClient(user, result);
        }
        return result;
    }
    public Item toServerData(UserConnection user, Item item) {
        if (item == null) return null;
        Item result = item.copy();
        List<Protocol> pipes = user.getProtocolInfo().getPipeline().pipes();
        for (int i = boundary(pipes); i < pipes.size(); i++) {
            Protocol pipe = pipes.get(i);
            if (pipe.getItemRewriter() != null) result = pipe.getItemRewriter().handleItemToServer(user, result);
        }
        return result;
    }
    private static int boundary(List<Protocol> pipes) {
        for (int i = 0; i < pipes.size(); i++) if (pipes.get(i) instanceof Protocol1_13To1_12_2) return i;
        throw new IllegalStateException("Missing 1.13 item translation boundary");
    }
}
