package com.viaversion.viaforge.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.MathHelper;

/** Local vanilla attack timer; damage and successful sweep attacks remain server-owned. */
public final class ServerCombatState {
    private static final UUID WEAPON_SPEED = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    private static EntityPlayer player;
    private static ItemStack previous;
    private static ItemStack attributeHand;
    private static boolean hasAttributes;
    private static int ticks;
    private static double base = 4;
    private static final Map<UUID, Modifier> MODIFIERS = new LinkedHashMap<>();
    public static boolean active() { return ServerBlockSession.supportsProtocol(107); }
    public static void clear() { player = null; previous = null; attributeHand = null; hasAttributes = false; ticks = 0; base = 4; MODIFIERS.clear(); }
    private static boolean ready() {
        if (!active() || Minecraft.getMinecraft().thePlayer == null) return false;
        if (player != Minecraft.getMinecraft().thePlayer) { clear(); player = Minecraft.getMinecraft().thePlayer; }
        return true;
    }
    public static void tick(EntityPlayer entity) {
        if (entity != Minecraft.getMinecraft().thePlayer || !ready()) return;
        if (ticks < Integer.MAX_VALUE) ticks++;
        ItemStack held = player.getHeldItem();
        if (!ItemStack.areItemStacksEqual(previous, held)) {
            if (previous == null || held == null || previous.getItem() != held.getItem() || !previous.isItemStackDamageable() && previous.getMetadata() != held.getMetadata()) ticks = 0;
            previous = held == null ? null : held.copy();
        }
    }
    public static void attack() { if (ready()) ticks = 0; }
    public static float period() { return (float)(20D / speed()); }
    public static float strength(float partialTicks) { return !ready() ? 1 : MathHelper.clamp_float((ticks + partialTicks) / period(), 0, 1); }
    public static double speed() {
        if (!ready()) return 4;
        Map<UUID, Modifier> values = new LinkedHashMap<>(MODIFIERS);
        ItemStack held = player.getHeldItem();
        // An attribute packet is authoritative until the held item changes locally.
        boolean serverHand = hasAttributes && sameHand(attributeHand, held);
        if (!serverHand) values.remove(WEAPON_SPEED);
        if (!serverHand && attributeHand != null && attributeHand.hasTagCompound()) {
            NBTTagList old = attributeHand.getTagCompound().getTagList("AttributeModifiers", 10);
            for (int i = 0; i < old.tagCount(); i++) {
                NBTTagCompound tag = old.getCompoundTagAt(i);
                if (tag.getString("AttributeName").equals("generic.attackSpeed") && (!tag.hasKey("Slot", 8) || tag.getString("Slot").equals("mainhand"))) values.remove(new UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast")));
            }
        }
        if (!serverHand && held != null && held.hasTagCompound() && held.getTagCompound().hasKey("AttributeModifiers", 9)) {
            NBTTagList list = held.getTagCompound().getTagList("AttributeModifiers", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                if (!tag.getString("AttributeName").equals("generic.attackSpeed") || tag.hasKey("Slot", 8) && !tag.getString("Slot").equals("mainhand")) continue;
                int operation = tag.getInteger("Operation"); double amount = tag.getDouble("Amount");
                UUID id = new UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast"));
                if (operation >= 0 && operation <= 2 && Double.isFinite(amount) && id.getMostSignificantBits() != 0 && id.getLeastSignificantBits() != 0) values.put(id, new Modifier(amount, operation));
            }
        } else if (!serverHand && held != null) {
            double amount = weaponModifier(held.getItem());
            if (amount != 0) values.put(WEAPON_SPEED, new Modifier(amount, 0));
        }
        double value = base;
        for (Modifier modifier : values.values()) if (modifier.operation == 0) value += modifier.amount;
        double additive = value;
        for (Modifier modifier : values.values()) if (modifier.operation == 1) value += additive * modifier.amount;
        for (Modifier modifier : values.values()) if (modifier.operation == 2) value *= 1 + modifier.amount;
        return Math.max(0, Math.min(1024, value));
    }
    private static double weaponModifier(Item item) {
        String name = String.valueOf(Item.itemRegistry.getNameForObject(item));
        if (!name.startsWith("minecraft:")) return 0;
        if (item instanceof ItemSword) return (double)-2.4F;
        if (item instanceof ItemSpade) return (double)-3F;
        if (item instanceof ItemPickaxe) return (double)-2.8F;
        if (item instanceof ItemAxe) return name.contains("wooden") || name.contains("stone") ? (double)-3.2F : name.contains("iron") ? (double)-3.1F : -3;
        if (item instanceof ItemHoe) return name.contains("diamond") ? 0 : name.contains("iron") ? -1 : name.contains("stone") ? -2 : -3;
        return 0;
    }
    public static void attributes(ByteBuf input) throws Exception {
        int id = Types.VAR_INT.readPrimitive(input), count = input.readInt();
        boolean local = ready() && id == player.getEntityId();
        for (int i = 0; i < count; i++) {
            String name = Types.STRING.read(input); double value = input.readDouble(); int size = Types.VAR_INT.readPrimitive(input);
            Map<UUID, Modifier> values = new LinkedHashMap<>();
            for (int j = 0; j < size; j++) {
                UUID uuid = new UUID(input.readLong(), input.readLong()); double amount = input.readDouble(); int operation = input.readUnsignedByte();
                if (operation <= 2 && Double.isFinite(amount)) values.put(uuid, new Modifier(amount, operation));
            }
            if (local && name.equals("generic.attackSpeed") && Double.isFinite(value)) {
                base = value; MODIFIERS.clear(); MODIFIERS.putAll(values);
                attributeHand = player.getHeldItem() == null ? null : player.getHeldItem().copy(); hasAttributes = true;
            }
        }
    }
    private static boolean sameHand(ItemStack a, ItemStack b) {
        return a == null || b == null ? a == b : a.getItem() == b.getItem() && (a.isItemStackDamageable() || a.getMetadata() == b.getMetadata()) && ItemStack.areItemStackTagsEqual(a, b);
    }
    private static final class Modifier {
        final double amount; final int operation;
        Modifier(double amount, int operation) { this.amount = amount; this.operation = operation; }
    }
    private ServerCombatState() { }
}
