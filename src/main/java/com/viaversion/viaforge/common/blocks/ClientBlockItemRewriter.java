package com.viaversion.viaforge.common.blocks;

import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.platform.ViaForgeProtocol;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_8;
import com.viaversion.viaversion.rewriter.ItemRewriter;

/** The native 1.8 edge of the pipeline: inventory, held/dropped items and placement. */
public final class ClientBlockItemRewriter extends ItemRewriter<ClientboundPackets1_8, ServerboundPackets1_8, ViaForgeProtocol> {
    private final LegacyBlockItemBridge bridge = new LegacyBlockItemBridge(ClientBlocks::localItem, ClientBlocks::serverItem);
    public ClientBlockItemRewriter(ViaForgeProtocol protocol) { super(protocol, Types.ITEM1_8, Types.ITEM1_8_SHORT_ARRAY); }
    @Override protected void registerPackets() {
        registerSetContent(ClientboundPackets1_8.CONTAINER_SET_CONTENT);
        registerSetSlot(ClientboundPackets1_8.CONTAINER_SET_SLOT);
        registerSetCreativeModeSlot(ServerboundPackets1_8.SET_CREATIVE_MODE_SLOT);
        registerCustomPayloadTradeList(ClientboundPackets1_8.CUSTOM_PAYLOAD);
        protocol.registerClientbound(ClientboundPackets1_8.SET_EQUIPPED_ITEM, wrapper -> {
            wrapper.passthrough(Types.VAR_INT);
            wrapper.passthrough(Types.SHORT); // 1.8 equipment slots are shorts, not varints
            passthroughClientboundItem(wrapper);
        });
        protocol.registerClientbound(ClientboundPackets1_8.SET_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.VAR_INT);
            for (EntityData data : wrapper.passthrough(Types.ENTITY_DATA_LIST1_8)) {
                if (data.dataType().type() == Types.ITEM1_8) data.setValue(handleItemToClient(wrapper.user(), (Item) data.getValue()));
            }
        });
        protocol.registerServerbound(ServerboundPackets1_8.CONTAINER_CLICK, wrapper -> {
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.BYTE);
            wrapper.passthrough(Types.SHORT);
            wrapper.passthrough(Types.BYTE); // 1.8 click mode
            wrapper.write(Types.ITEM1_8, handleItemToServer(wrapper.user(), wrapper.read(Types.ITEM1_8)));
        });
        protocol.registerServerbound(ServerboundPackets1_8.USE_ITEM_ON, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_8);
            // ViaRewind consumes a signed BYTE here (255 means use-in-air).
            // Typed wrapper fields must match even when their wire width is equal.
            wrapper.passthrough(Types.BYTE);
            wrapper.write(Types.ITEM1_8, handleItemToServer(wrapper.user(), wrapper.read(Types.ITEM1_8)));
        });
    }
    @Override public Item handleItemToClient(UserConnection connection, Item item) { return bridge.toClient(connection, item); }
    @Override public Item handleItemToServer(UserConnection connection, Item item) { return bridge.toServer(connection, item); }
}
