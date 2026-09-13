package com.viaversion.viaforge.items;

import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

/** Names/colors for newer effect NBT. Authoritative effects still arrive from the server. */
final class ServerPotions extends Potion {
    private ServerPotions(int id, String name, boolean bad, int color) { super(id, new ResourceLocation(name), bad, color); setPotionName("effect." + name); }
    static void register() {
        add(24, "glowing", false, 9740385); add(25, "levitation", true, 0xceffff);
        add(26, "luck", false, 0x339900); add(27, "unluck", true, 12624973);
    }
    private static void add(int id, String name, boolean bad, int color) { if (Potion.potionTypes[id] == null) new ServerPotions(id, name, bad, color); }
}
