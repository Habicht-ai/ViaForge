package com.viaversion.viaforge.development;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.*;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.*;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.*;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.NettyCompressionDecoder;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

final class BlockItemPipelineSmokeTest {
    static void verify(BlockVersionProfile profile, EmbeddedChannel channel, EmbeddedChannel server, net.minecraft.client.multiplayer.WorldClient world) throws Exception {
        server.pipeline().addFirst("server-decompress", new NettyCompressionDecoder(256));
        verifyRightClicks(profile, channel, server);
        for (LegacyBlockCatalog.Definition block : LegacyBlockCatalog.BLOCKS) {
            if (block.itemProtocol() > profile.protocol() || block.itemId() < 0) continue;
            CompoundTag originalTag = new CompoundTag();
            CompoundTag display = new CompoundTag(); display.putString("Name", "Named " + block.name); originalTag.put("display", display);
            CompoundTag custom = new CompoundTag(); custom.putInt("TestValue", 12345); originalTag.put("CustomBlockData", custom);
            Item original = new DataItem(block.itemId(), (byte) 1, (short) block.itemData(), originalTag);
            ByteBuf source = BlockPipelineSmokeTest.packet(clientbound(profile, "CONTAINER_SET_SLOT"));
            source.writeByte(0).writeShort(36); Types.ITEM1_8.write(source, original);
            BlockPipelineSmokeTest.receiveCompressed(channel, server, source);
            ByteBuf received = BlockPipelineSmokeTest.take(channel, 0x2f);
            Item local;
            try {
                S2FPacketSetSlot nativePacket = new S2FPacketSetSlot();
                nativePacket.readPacketData(new PacketBuffer(received.duplicate()));
                require(nativePacket.func_149174_e() != null && nativePacket.func_149174_e().getDisplayName().equals("Named " + block.name), "Native named inventory item " + block.name);
                received.skipBytes(3); local = Types.ITEM1_8.read(received);
            } finally { received.release(); }
            require(local.identifier() == ClientBlocks.localItem(block.itemId(), block.itemData()), "Inventory id " + block.name + " on " + profile);
            require(local.data() == 0, "Local item metadata " + block.name);
            require(local.tag().getCompoundTag("CustomBlockData").getInt("TestValue") == 12345, "Custom NBT " + block.name);

            // Creative pick and normal inventory clicks must send server ids, never Forge registry ids.
            for (boolean creative : new boolean[]{true, false}) {
                ByteBuf outbound = BlockPipelineSmokeTest.packet(creative ? 0x10 : 0x0e);
                if (creative) outbound.writeShort(36);
                else outbound.writeByte(0).writeShort(36).writeByte(0).writeShort(1).writeByte(0);
                Types.ITEM1_8.write(outbound, local);
                Item roundTrip = sendItem(channel, server, outbound, serverbound(profile, creative ? "SET_CREATIVE_MODE_SLOT" : "CONTAINER_CLICK"), creative);
                require(roundTrip.identifier() == block.itemId() && roundTrip.data() == block.itemData(), "Server item id/metadata " + block.name);
                require(roundTrip.amount() == original.amount(), "Server item amount " + block.name);
                require(roundTrip.tag().equals(originalTag), "Server item NBT round trip " + block.name + ": " + roundTrip.tag());
            }
            // A freshly picked block has no Via NBT attached. It must work as well.
            Item fresh = local.copy(); fresh.setTag(null);
            ByteBuf creative = BlockPipelineSmokeTest.packet(0x10); creative.writeShort(37); Types.ITEM1_8.write(creative, fresh);
            Item picked = sendItem(channel, server, creative, serverbound(profile, "SET_CREATIVE_MODE_SLOT"), true);
            require(picked.identifier() == block.itemId() && picked.data() == block.itemData(), "Fresh creative pick " + block.name);
        }
        ByteBuf equipment = BlockPipelineSmokeTest.packet(clientbound(profile, "SET_EQUIPPED_ITEM"));
        Types.VAR_INT.writePrimitive(equipment, 1); Types.VAR_INT.writePrimitive(equipment, 0);
        Types.ITEM1_8.write(equipment, new DataItem(201, (byte) 1, (short) 0, null));
        BlockPipelineSmokeTest.receiveCompressed(channel, server, equipment);
        ByteBuf held = BlockPipelineSmokeTest.take(channel, 0x04);
        try {
            Types.VAR_INT.readPrimitive(held); held.readShort();
            require(Types.ITEM1_8.read(held).identifier() == ClientBlocks.localItem(201, 0), "Held block item");
        } finally { held.release(); }
        Item[] slots = new Item[46];
        slots[36] = new DataItem(201, (byte) 2, (short) 0, null);
        slots[45] = new DataItem(201, (byte) 1, (short) 0, null);
        ByteBuf contents = BlockPipelineSmokeTest.packet(clientbound(profile, "CONTAINER_SET_CONTENT"));
        contents.writeByte(0); Types.ITEM1_8_SHORT_ARRAY.write(contents, slots);
        BlockPipelineSmokeTest.receiveCompressed(channel, server, contents);
        ByteBuf nativeContents = BlockPipelineSmokeTest.take(channel, 0x30);
        try {
            nativeContents.readByte(); Item[] inventory = Types.ITEM1_8_SHORT_ARRAY.read(nativeContents);
            require(inventory.length == 45, "Native inventory length (offhand omitted)");
            require(inventory[36].identifier() == ClientBlocks.localItem(201, 0), "Bulk inventory block item");
        } finally { nativeContents.release(); }
        if (profile.protocol() >= 315) verifyShulkerWindow(profile, channel, server, world);
    }

    private static void verifyRightClicks(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server) throws Exception {
        BlockPos position = new BlockPos(1057, 169, 139931);
        ItemStack purpur = new ItemStack(net.minecraft.item.Item.getItemById(ClientBlocks.localItem(201, 0)));
        for (ItemStack stack : new ItemStack[]{null, new ItemStack(Blocks.stone), purpur}) {
            for (int face = 0; face < 6; face++) {
                ByteBuf packet = BlockPipelineSmokeTest.packet(0x08);
                new C08PacketPlayerBlockPlacement(position, face, stack, .25F, .5F, .75F).writePacketData(new PacketBuffer(packet));
                send(client, server, packet);
                ByteBuf sent = BlockPipelineSmokeTest.take(server, serverbound(profile, "USE_ITEM_ON"));
                try {
                    com.viaversion.viaversion.api.minecraft.BlockPosition actual = Types.BLOCK_POSITION1_8.read(sent);
                    require(actual.x() == position.getX() && actual.y() == position.getY() && actual.z() == position.getZ(), "Right-click position");
                    require(Types.VAR_INT.readPrimitive(sent) == face, "Right-click block face");
                    require(Types.VAR_INT.readPrimitive(sent) == 0, "Right-click main hand");
                    for (float hit : new float[]{.25F, .5F, .75F}) {
                        float coordinate = profile.protocol() >= 315 ? sent.readFloat() : sent.readUnsignedByte() / 16F;
                        require(Math.abs(coordinate - hit) < .00001F, "Right-click hit coordinate");
                    }
                    require(!sent.isReadable(), "Right-click packet fully consumed");
                } finally { sent.release(); }
            }
        }
        // 1.8 encodes use-in-air with face 255, which ViaRewind reads as signed -1.
        ByteBuf air = BlockPipelineSmokeTest.packet(0x08);
        new C08PacketPlayerBlockPlacement(purpur).writePacketData(new PacketBuffer(air));
        try { send(client, server, air); }
        catch (com.viaversion.viaversion.exception.CancelEncoderException expected) {
            // ViaRewind replaces C08's air sentinel with USE_ITEM. NetworkManager
            // normally ignores this cancellation; EmbeddedChannel exposes it.
        }
        ByteBuf use = BlockPipelineSmokeTest.take(server, serverbound(profile, "USE_ITEM"));
        try { require(Types.VAR_INT.readPrimitive(use) == 0 && !use.isReadable(), "Use-in-air sentinel"); }
        finally { use.release(); }
    }

    private static void verifyShulkerWindow(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, net.minecraft.client.multiplayer.WorldClient world) throws Exception {
        ByteBuf open = BlockPipelineSmokeTest.packet(clientbound(profile, "OPEN_SCREEN"));
        open.writeByte(2); Types.STRING.write(open, "minecraft:shulker_box");
        Types.STRING.write(open, "{\"text\":\"Test Shulker\"}"); open.writeByte(27);
        BlockPipelineSmokeTest.receiveCompressed(client, server, open);
        ByteBuf window = BlockPipelineSmokeTest.take(client, 0x2d);
        try {
            S2DPacketOpenWindow nativeWindow = new S2DPacketOpenWindow();
            nativeWindow.readPacketData(new PacketBuffer(window));
            require(nativeWindow.getWindowId() == 2 && nativeWindow.getSlotCount() == 27, "Shulker window slots");
            require(nativeWindow.getWindowTitle().getUnformattedText().equals("Test Shulker"), "Shulker title");
            verifyNativeShulkerMenu(nativeWindow, world);
        } finally { window.release(); }
        Item[] slots = new Item[63];
        slots[0] = new DataItem(201, (byte) 7, (short) 0, null);
        ByteBuf contents = BlockPipelineSmokeTest.packet(clientbound(profile, "CONTAINER_SET_CONTENT"));
        contents.writeByte(2); Types.ITEM1_8_SHORT_ARRAY.write(contents, slots);
        BlockPipelineSmokeTest.receiveCompressed(client, server, contents);
        ByteBuf received = BlockPipelineSmokeTest.take(client, 0x30);
        Item local;
        try {
            require(received.readUnsignedByte() == 2, "Shulker content window");
            Item[] inventory = Types.ITEM1_8_SHORT_ARRAY.read(received);
            require(inventory.length == 63, "Shulker and player slots");
            local = inventory[0];
            require(local.identifier() == ClientBlocks.localItem(201, 0) && local.amount() == 7, "Shulker block stack");
        } finally { received.release(); }
        ByteBuf click = BlockPipelineSmokeTest.packet(0x0e);
        click.writeByte(2).writeShort(0).writeByte(0).writeShort(2).writeByte(0); Types.ITEM1_8.write(click, local);
        Item moved = sendItem(client, server, click, serverbound(profile, "CONTAINER_CLICK"), false);
        require(moved.identifier() == 201 && moved.amount() == 7, "Shulker stack click");
        ByteBuf close = BlockPipelineSmokeTest.packet(0x0d); close.writeByte(2);
        send(client, server, close);
        ByteBuf closed = BlockPipelineSmokeTest.take(server, serverbound(profile, "CONTAINER_CLOSE"));
        try { require(closed.readUnsignedByte() == 2 && !closed.isReadable(), "Shulker window close"); }
        finally { closed.release(); }
    }

    private static void verifyNativeShulkerMenu(S2DPacketOpenWindow packet, net.minecraft.client.multiplayer.WorldClient world) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.client.network.NetHandlerPlayClient handler = new net.minecraft.client.network.NetHandlerPlayClient(mc, null,
                new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.CLIENTBOUND),
                new com.mojang.authlib.GameProfile(new java.util.UUID(0, 4), "ShulkerMenuTest"));
        net.minecraft.client.gui.inventory.GuiChest[] opened = new net.minecraft.client.gui.inventory.GuiChest[1];
        net.minecraft.client.entity.EntityPlayerSP previous = mc.thePlayer;
        net.minecraft.client.entity.EntityPlayerSP player = new net.minecraft.client.entity.EntityPlayerSP(mc, world, handler, new net.minecraft.stats.StatFileWriter()) {
            @Override public void displayGUIChest(net.minecraft.inventory.IInventory inventory) {
                // Exercise the actual native packet/menu path without replacing the test screen.
                opened[0] = new net.minecraft.client.gui.inventory.GuiChest(this.inventory, inventory);
                this.openContainer = opened[0].inventorySlots;
            }
        };
        try {
            mc.thePlayer = player;
            handler.handleOpenWindow(packet);
            require(opened[0] != null && player.openContainer.inventorySlots.size() == 63
                    && player.openContainer.windowId == 2, "Native shulker menu creation");
        } finally { mc.thePlayer = previous; }
    }

    static void send(EmbeddedChannel client, EmbeddedChannel server, ByteBuf packet) {
        try { client.writeOutbound(packet); }
        finally {
            client.runPendingTasks();
            ByteBuf compressed;
            while ((compressed = client.readOutbound()) != null) server.writeInbound(compressed);
        }
    }
    static Item sendItem(EmbeddedChannel client, EmbeddedChannel server, ByteBuf packet, int id, boolean creative) {
        client.writeOutbound(packet);
        ByteBuf compressed = (ByteBuf) client.readOutbound();
        require(compressed != null, "Missing outgoing inventory packet");
        server.writeInbound(compressed);
        ByteBuf sent = BlockPipelineSmokeTest.take(server, id);
        try {
            if (creative) sent.readShort();
            else { sent.skipBytes(6); Types.VAR_INT.readPrimitive(sent); }
            return Types.ITEM1_8.read(sent);
        } finally { sent.release(); }
    }
    static int clientbound(BlockVersionProfile profile, String name) {
        PacketType[] types = profile.protocol() >= 338 ? ClientboundPackets1_12_1.values() : profile.protocol() >= 335 ? ClientboundPackets1_12.values()
                : profile.protocol() >= 110 ? ClientboundPackets1_9_3.values() : ClientboundPackets1_9.values();
        return find(types, name);
    }
    static int serverbound(BlockVersionProfile profile, String name) {
        PacketType[] types = profile.protocol() >= 338 ? ServerboundPackets1_12_1.values() : profile.protocol() >= 335 ? ServerboundPackets1_12.values()
                : profile.protocol() >= 110 ? ServerboundPackets1_9_3.values() : ServerboundPackets1_9.values();
        return find(types, name);
    }
    private static int find(PacketType[] types, String name) { for (PacketType type : types) if (type.getName().equals(name)) return type.getId(); throw new AssertionError(name); }
    private static void require(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
