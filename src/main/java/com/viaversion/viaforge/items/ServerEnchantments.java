package com.viaversion.viaforge.items;

import net.minecraft.enchantment.*;
import net.minecraft.util.*;

/** Display support for the four additions; server-side enchanting mechanics stay authoritative. */
public final class ServerEnchantments extends Enchantment {
    private final int maximum;
    private final boolean curse;
    private ServerEnchantments(int id, String name, int maximum, boolean curse) {
        super(id, new ResourceLocation(name), 2, EnumEnchantmentType.ALL); this.maximum = maximum; this.curse = curse; setName(name);
    }
    static void register() {
        add(9, "frostWalker", 2, false); add(70, "mending", 1, false);
        add(10, "binding_curse", 1, true); add(71, "vanishing_curse", 1, true);
    }
    private static void add(int id, String name, int max, boolean curse) { if (Enchantment.getEnchantmentById(id) == null) new ServerEnchantments(id, name, max, curse); }
    @Override public int getMaxLevel() { return maximum; }
    @Override public String getTranslatedName(int level) {
        String name = maximum == 1 && level == 1 ? StatCollector.translateToLocal(getName()) : super.getTranslatedName(level);
        return curse ? EnumChatFormatting.RED + name : name;
    }
}
