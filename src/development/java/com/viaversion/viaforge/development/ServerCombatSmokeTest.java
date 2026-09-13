package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.network.*;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

final class ServerCombatSmokeTest {
    static void pipeline(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, WorldClient world) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); ItemStack previous = mc.thePlayer.getHeldItem();
        EffectRenderer oldEffects = mc.effectRenderer; Entity oldCamera = mc.getRenderViewEntity();
        List<EntityFX> particles = new ArrayList<>();
        mc.effectRenderer = new EffectRenderer(world, mc.getTextureManager()) { @Override public void addEffect(EntityFX effect) { particles.add(effect); super.addEffect(effect); } };
        mc.setRenderViewEntity(mc.thePlayer); mc.thePlayer.setPosition(4, 75, 4); ServerCombatState.clear();
        try {
            ByteBuf sweep = packet(profile, "LEVEL_PARTICLES"); sweep.writeInt(45).writeBoolean(false).writeFloat(4).writeFloat(75).writeFloat(4);
            sweep.writeFloat(1).writeFloat(0).writeFloat(0).writeFloat(0).writeInt(0);
            BlockPipelineSmokeTest.receiveCompressed(client, server, sweep);
            ByteBuf received = client.readInbound(); require(received != null, "Sweep delivered");
            try {
                require(Types.VAR_INT.readPrimitive(received) == 0x3f, "Sweep event bypasses replacement particles");
                net.minecraft.network.play.server.S3FPacketCustomPayload packet = new net.minecraft.network.play.server.S3FPacketCustomPayload();
                packet.readPacketData(new PacketBuffer(received)); packet.processPacket(handler);
            } finally { received.release(); }
            ByteBuf extra = client.readInbound(); if (extra != null) { extra.release(); throw new AssertionError("Duplicate sweep effect"); }
            require(particles.size() == 1 && particles.get(0) instanceof ServerSweepParticle, "Original sweep factory since 1.9");
            EntityFX fx = particles.get(0); for (int i = 0; i < 3; i++) fx.onUpdate(); require(!fx.isDead && fx.posX == 4 && fx.posY == 75, "Sweep stays stationary through third tick");
            fx.onUpdate(); require(fx.isDead, "Sweep completes after four ticks");
            ItemStack sword = new ItemStack(Items.diamond_sword); mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = sword;
            ServerCombatState.tick(mc.thePlayer); require(Math.abs(ServerCombatState.period() - 12.5) < .00001, "Vanilla sword cooldown");
            ServerCombatState.attack(); require(ServerCombatState.strength(0) == 0, "Attack resets charge");
            for (int tick = 0; tick < 6; tick++) ServerCombatState.tick(mc.thePlayer);
            require(Math.abs(ServerCombatState.strength(0) - .48) < .00001, "Charge advances by game ticks");
            sword.setItemDamage(1); ServerCombatState.tick(mc.thePlayer); require(ServerCombatState.strength(0) > .55, "Durability updates do not reset charge");
            sword.getItem().onItemRightClick(sword, world, mc.thePlayer); require(mc.thePlayer.getItemInUseCount() == 0 && sword.getItemUseAction() == EnumAction.NONE, "Modern sword does not start old blocking pose");
            ItemRenderer hand = new ItemRenderer(mc);
            set(hand, "itemToRender", sword); set(hand, "equippedProgress", 1F); set(hand, "prevEquippedProgress", 1F);
            ServerCombatState.attack(); hand.updateEquippedItem(); require(Math.abs(number(hand, "equippedProgress") - .6F) < .00001, "Original .4 maximum hand lowering step");
            for (int tick = 0; tick < 6; tick++) ServerCombatState.tick(mc.thePlayer);
            float charge = ServerCombatState.strength(1); set(hand, "equippedProgress", .1F); hand.updateEquippedItem();
            require(Math.abs(number(hand, "equippedProgress") - charge * charge * charge) < .00001, "Hand rise follows cubic attack strength");
            ByteBuf attributes = packet(profile, "UPDATE_ATTRIBUTES"); Types.VAR_INT.writePrimitive(attributes, mc.thePlayer.getEntityId()); attributes.writeInt(1);
            Types.STRING.write(attributes, "generic.attackSpeed"); attributes.writeDouble(6); Types.VAR_INT.writePrimitive(attributes, 4);
            modifier(attributes, UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"), (double)-2.4F, 0);
            modifier(attributes, new UUID(1, 1), 2, 0); modifier(attributes, new UUID(2, 2), .5, 1); modifier(attributes, new UUID(3, 3), .25, 2);
            send(client, server, handler, attributes);
            require(Math.abs(ServerCombatState.speed() - (6 + 2 - (double)2.4F) * 1.5 * 1.25) < .00001, "Server attack-speed base and all modifier operations retained");
            mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = new ItemStack(Items.diamond_axe);
            ServerCombatState.tick(mc.thePlayer); require(ServerCombatState.strength(0) == 0 && Math.abs(ServerCombatState.speed() - 9.375) < .00001, "Slot change resets timer and re-applies native tool speed with server modifiers");
            Item book = Item.getItemById(ClientItems.localItem(403, 0)); List<ItemStack> books = new ArrayList<>(); book.getSubItems(book, CreativeTabs.tabAllSearch, books);
            int levels = 0;
            for (ItemStack entry : books) {
                net.minecraft.nbt.NBTTagCompound stored = entry.getTagCompound().getTagList("StoredEnchantments", 10).getCompoundTagAt(0);
                if (stored.getShort("id") != 22) continue;
                levels++; require(stored.getShort("lvl") == levels, "Sweeping I, II, III search ordering");
                require(entry.getTooltip(mc.thePlayer, false).toString().contains("Sweeping Edge"), "Sweeping book displays enchantment name");
            }
            require(levels == (profile.protocol() >= 316 ? 3 : 0), "Sweeping Edge release boundary is 1.11.1");
            if (profile.protocol() >= 316) {
                sword.addEnchantment(Enchantment.getEnchantmentById(22), 3);
                require(sword.getTooltip(mc.thePlayer, false).toString().contains("Sweeping Edge III"), "Sword tooltip includes sweeping level");
                ByteBuf slot = packet(profile, "CONTAINER_SET_SLOT"); slot.writeByte(0).writeShort(36); Types.ITEM1_8.write(slot, ServerItemSmokeTest.wire(sword));
                BlockPipelineSmokeTest.receiveCompressed(client, server, slot);
                ByteBuf itemPacket = BlockPipelineSmokeTest.take(client, 0x2f);
                try {
                    itemPacket.skipBytes(3); ItemStack receivedSword = new PacketBuffer(itemPacket).readItemStackFromBuffer();
                    require(EnchantmentHelper.getEnchantmentLevel(22, receivedSword) == 3, "Sweeping enchantment survives Via on a native sword");
                    require(receivedSword.getTooltip(mc.thePlayer, false).toString().contains("Sweeping Edge III"), "Received sword tooltip includes sweeping level");
                } finally { itemPacket.release(); }
            }
            for (int enchantment : new int[]{9, 70, 10, 71, 22}) {
                if (profile.protocol() < (enchantment == 22 ? 316 : enchantment == 10 || enchantment == 71 ? 315 : 107)) continue;
                ItemStack equipment = new ItemStack(enchantment == 22 ? Items.diamond_sword : Items.diamond_boots, 1, 27);
                equipment.setStackDisplayName("Original equipment");
                equipment.addEnchantment(Enchantment.getEnchantmentById(enchantment), Enchantment.getEnchantmentById(enchantment).getMaxLevel());
                equipment.addEnchantment(Enchantment.unbreaking, 2);
                com.viaversion.viaversion.api.minecraft.item.Item original = ServerItemSmokeTest.wire(equipment);
                ByteBuf slot = packet(profile, "CONTAINER_SET_SLOT"); slot.writeByte(0).writeShort(36); Types.ITEM1_8.write(slot, original);
                BlockPipelineSmokeTest.receiveCompressed(client, server, slot);
                ByteBuf incoming = BlockPipelineSmokeTest.take(client, 0x2f); com.viaversion.viaversion.api.minecraft.item.Item local;
                try { incoming.skipBytes(3); local = Types.ITEM1_8.read(incoming); } finally { incoming.release(); }
                require(original.tag().equals(local.tag()) && local.data() == 27, "Native equipment enchantments, name and durability restored: " + enchantment + " " + local.tag());
                for (boolean fresh : new boolean[]{false, true}) for (boolean creative : new boolean[]{false, true}) {
                    ByteBuf click = BlockPipelineSmokeTest.packet(creative ? 0x10 : 0x0e);
                    if (creative) click.writeShort(36); else click.writeByte(0).writeShort(36).writeByte(0).writeShort(1).writeByte(0);
                    Types.ITEM1_8.write(click, fresh ? original : local);
                    com.viaversion.viaversion.api.minecraft.item.Item result = BlockItemPipelineSmokeTest.sendItem(client, server, click, BlockItemPipelineSmokeTest.serverbound(profile, creative ? "SET_CREATIVE_MODE_SLOT" : "CONTAINER_CLICK"), creative);
                    require(result.identifier() == original.identifier() && result.data() == 27 && original.tag().equals(result.tag()), "Native enchantment survives creative/inventory round trip: " + enchantment + " " + result.tag());
                }
            }
            attributes = packet(profile, "UPDATE_ATTRIBUTES"); Types.VAR_INT.writePrimitive(attributes, mc.thePlayer.getEntityId()); attributes.writeInt(1);
            Types.STRING.write(attributes, "generic.attackSpeed"); attributes.writeDouble(4); Types.VAR_INT.writePrimitive(attributes, 1);
            modifier(attributes, UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"), 5, 0);
            send(client, server, handler, attributes); require(ServerCombatState.speed() == 9, "Server override of weapon modifier takes precedence");
        } finally { mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = previous; mc.effectRenderer = oldEffects; mc.setRenderViewEntity(oldCamera); ServerCombatState.clear(); }
    }
    private static void modifier(ByteBuf packet, UUID id, double amount, int operation) { packet.writeLong(id.getMostSignificantBits()).writeLong(id.getLeastSignificantBits()).writeDouble(amount).writeByte(operation); }
    static void set(ItemRenderer hand, String name, Object value) throws Exception { Field field = ItemRenderer.class.getDeclaredField(name); field.setAccessible(true); field.set(hand, value); }
    static float number(ItemRenderer hand, String name) throws Exception { Field field = ItemRenderer.class.getDeclaredField(name); field.setAccessible(true); return field.getFloat(hand); }
}
