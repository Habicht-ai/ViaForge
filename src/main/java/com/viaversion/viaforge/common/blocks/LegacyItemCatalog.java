package com.viaversion.viaforge.common.blocks;

import java.util.*;

/** Vanilla item registrations from 1.9 through 1.12.2. Seeds and beds are block items. */
public final class LegacyItemCatalog {
    public enum Kind { MATERIAL, FOOD, SOUP, CRYSTAL, POTION, SPLASH, LINGERING, ARROW, TIPPED_ARROW,
        SHIELD, ELYTRA, BOAT, TOTEM, BOOK, ENCHANTED_BOOK, EGG, HEAD }
    public static final List<Definition> ITEMS;
    static {
        List<Definition> items = new ArrayList<>();
        add(items, 333, "oak_boat", "oak_boat", 107, Kind.BOAT, 1, 0);
        add(items, 373, "potion", "bottle_drinkable", 107, Kind.POTION, 1, 0);
        add(items, 383, "spawn_egg", "spawn_egg", 107, Kind.EGG, 64, 0);
        items.add(new Definition(397, 5, "dragon_head", "skull_dragon", 107, Kind.HEAD, 64, 0));
        add(items, 403, "enchanted_book", "enchanted_book", 107, Kind.ENCHANTED_BOOK, 1, 0);
        add(items, 426, "end_crystal", "end_crystal", 107, Kind.CRYSTAL, 64, 0);
        add(items, 432, "chorus_fruit", "chorus_fruit", 107, Kind.FOOD, 64, 0);
        add(items, 433, "chorus_fruit_popped", "chorus_fruit_popped", 107, Kind.MATERIAL, 64, 0);
        add(items, 434, "beetroot", "beetroot", 107, Kind.FOOD, 64, 0);
        add(items, 436, "beetroot_soup", "beetroot_soup", 107, Kind.SOUP, 1, 0);
        add(items, 437, "dragon_breath", "dragon_breath", 107, Kind.MATERIAL, 64, 0);
        add(items, 438, "splash_potion", "bottle_splash", 107, Kind.SPLASH, 1, 0);
        add(items, 439, "spectral_arrow", "spectral_arrow", 107, Kind.ARROW, 64, 0);
        add(items, 440, "tipped_arrow", "tipped_arrow", 107, Kind.TIPPED_ARROW, 64, 0);
        add(items, 441, "lingering_potion", "bottle_lingering", 107, Kind.LINGERING, 1, 0);
        add(items, 442, "shield", "shield", 107, Kind.SHIELD, 1, 336);
        add(items, 443, "elytra", "elytra", 107, Kind.ELYTRA, 1, 432);
        String[] woods = {"spruce", "birch", "jungle", "acacia", "dark_oak"};
        for (int i = 0; i < woods.length; i++) add(items, 444 + i, woods[i] + "_boat", woods[i] + "_boat", 107, Kind.BOAT, 1, 0);
        add(items, 449, "totem_of_undying", "totem", 315, Kind.TOTEM, 1, 0);
        add(items, 450, "shulker_shell", "shulker_shell", 315, Kind.MATERIAL, 64, 0);
        // 451 is unassigned. Iron nuggets were introduced in 1.11.1, not 1.11.
        add(items, 452, "iron_nugget", "iron_nugget", 316, Kind.MATERIAL, 64, 0);
        add(items, 453, "knowledge_book", "knowledge_book", 335, Kind.BOOK, 1, 0);
        ITEMS = Collections.unmodifiableList(items);
    }
    private static void add(List<Definition> list, int id, String name, String model, int protocol, Kind kind, int count, int damage) {
        list.add(new Definition(id, 0, name, model, protocol, kind, count, damage));
    }
    public static final class Definition implements LegacyItemDefinition {
        public final int id, data, protocol, stackSize, durability;
        public final String name, model;
        public final Kind kind;
        private Definition(int id, int data, String name, String model, int protocol, Kind kind, int count, int durability) {
            this.id = id; this.data = data; this.name = name; this.model = model; this.protocol = protocol;
            this.kind = kind; this.stackSize = count; this.durability = durability;
        }
        public int itemId() { return id; }
        public int itemData() { return data; }
        public int itemProtocol() { return protocol; }
        public boolean preservesDamage() { return durability > 0; }
    }
    private LegacyItemCatalog() { }
}
