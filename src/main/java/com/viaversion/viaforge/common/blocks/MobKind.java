package com.viaversion.viaforge.common.blocks;

import java.util.Map;

/** Wire identities, including the variants which only received separate IDs in 1.11. */
public enum MobKind {
    ELDER_GUARDIAN(4,315), WITHER_SKELETON(5,315), STRAY(6,315), HUSK(23,315),
    ZOMBIE_VILLAGER(27,315), SKELETON_HORSE(28,315), ZOMBIE_HORSE(29,315), DONKEY(31,315), MULE(32,315),
    EVOKER(34,315), VEX(35,315), VINDICATOR(36,315), ILLUSIONER(37,335),
    CREEPER(50,107), SKELETON(51,107), GIANT(53,107), ZOMBIE(54,107), SLIME(55,107), GHAST(56,107),
    ZOMBIE_PIGMAN(57,107), ENDERMAN(58,107), CAVE_SPIDER(59,107), SPIDER(52,107), SILVERFISH(60,107),
    BLAZE(61,107), MAGMA_CUBE(62,107), ENDER_DRAGON(63,107), WITHER(64,107), BAT(65,107), WITCH(66,107),
    ENDERMITE(67,107), GUARDIAN(68,107), SHULKER(69,107), PIG(90,107), SHEEP(91,107), COW(92,107),
    CHICKEN(93,107), SQUID(94,107), WOLF(95,107), MOOSHROOM(96,107), SNOW_GOLEM(97,107), OCELOT(98,107),
    IRON_GOLEM(99,107), HORSE(100,107), RABBIT(101,107), POLAR_BEAR(102,210), LLAMA(103,315),
    PARROT(105,335), VILLAGER(120,107);

    public final int id, protocol;
    MobKind(int id, int protocol) { this.id = id; this.protocol = protocol; }
    public static MobKind resolve(int protocol, int id, Map<Integer, Object> data) {
        int first = protocol >= 210 ? 12 : 11;
        if (protocol < 315) {
            if (id == 51) {
                int type = number(data.get(first), 0);
                if (type == 1) return WITHER_SKELETON;
                if (type == 2 && protocol >= 210) return STRAY;
            } else if (id == 54) {
                int type = number(data.get(first + 1), 0);
                if (type >= 1 && type <= 5) return ZOMBIE_VILLAGER;
                if (type == 6 && protocol >= 210) return HUSK;
            } else if (id == 100) {
                int type = number(data.get(first + 2), 0);
                if (type >= 1 && type <= 4) return new MobKind[]{DONKEY, MULE, ZOMBIE_HORSE, SKELETON_HORSE}[type - 1];
            } else if (id == 68 && (number(data.get(first), 0) & 4) != 0) return ELDER_GUARDIAN;
        }
        for (MobKind kind : values()) if (kind.id == id && protocol >= kind.protocol) return kind;
        return null;
    }
    public boolean custom() {
        switch (this) {
            case SHULKER: case POLAR_BEAR: case LLAMA: case PARROT: case EVOKER: case VINDICATOR:
            case VEX: case ILLUSIONER: case STRAY: case HUSK: case ZOMBIE_VILLAGER: case ZOMBIE:
            case SKELETON: case WITHER_SKELETON: return true;
            default: return false;
        }
    }
    public boolean skeleton() { return this == SKELETON || this == WITHER_SKELETON || this == STRAY; }
    public boolean zombie() { return this == ZOMBIE || this == HUSK || this == ZOMBIE_VILLAGER; }
    public boolean illager() { return this == EVOKER || this == VINDICATOR || this == ILLUSIONER; }
    public static int number(Object value, int fallback) { return value instanceof Number ? ((Number)value).intValue() : fallback; }
}
