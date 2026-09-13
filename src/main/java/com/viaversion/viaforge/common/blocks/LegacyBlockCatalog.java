package com.viaversion.viaforge.common.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Vanilla registry additions after 1.8, with wire metadata and release boundaries. */
public final class LegacyBlockCatalog {
    public enum Kind { CUBE, STAIRS, PILLAR, SLAB, DOUBLE_SLAB, ROD, CHORUS, FLOWER, CROP, PATH,
        ICE, COMMAND, OBSERVER, SHULKER, VOID, GATEWAY, STRUCTURE, GLAZED, POWDER, BED }
    public static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    public static final List<Definition> BLOCKS;
    public static final int STATE_LIMIT = 4352;
    private static final Definition[] STATES = new Definition[STATE_LIMIT];
    static {
        List<Definition> blocks = new ArrayList<>();
        add(blocks, 137, "command_block", 107, Kind.COMMAND, -1, 3600000);
        add(blocks, 198, "end_rod", 107, Kind.ROD, 0, 0);
        add(blocks, 199, "chorus_plant", 107, Kind.CHORUS, .4F, .4F);
        add(blocks, 200, "chorus_flower", 107, Kind.FLOWER, .4F, .4F);
        add(blocks, 201, "purpur_block", 107, Kind.CUBE, 1.5F, 6);
        add(blocks, 202, "purpur_pillar", 107, Kind.PILLAR, 1.5F, 6);
        add(blocks, 203, "purpur_stairs", 107, Kind.STAIRS, 1.5F, 6);
        add(blocks, 204, "purpur_double_slab", 107, Kind.DOUBLE_SLAB, 2, 6);
        add(blocks, 205, "purpur_slab", 107, Kind.SLAB, 2, 6);
        add(blocks, 206, "end_bricks", 107, Kind.CUBE, .8F, .8F);
        add(blocks, 207, "beetroots", 107, Kind.CROP, 0, 0);
        add(blocks, 208, "grass_path", 107, Kind.PATH, .65F, .65F);
        add(blocks, 209, "end_gateway", 107, Kind.GATEWAY, -1, 3600000);
        add(blocks, 210, "repeating_command_block", 107, Kind.COMMAND, -1, 3600000);
        add(blocks, 211, "chain_command_block", 107, Kind.COMMAND, -1, 3600000);
        add(blocks, 212, "frosted_ice", 107, Kind.ICE, .5F, .5F);
        add(blocks, 213, "magma", 210, Kind.CUBE, .5F, .5F);
        add(blocks, 214, "nether_wart_block", 210, Kind.CUBE, 1, 1);
        add(blocks, 215, "red_nether_brick", 210, Kind.CUBE, 2, 6);
        add(blocks, 216, "bone_block", 210, Kind.PILLAR, 2, 2);
        add(blocks, 217, "structure_void", 210, Kind.VOID, 0, 0);
        add(blocks, 218, "observer", 315, Kind.OBSERVER, 3, 3);
        for (int color = 0; color < 16; color++) {
            blocks.add(new Definition(26, COLORS[color] + "_bed", 335, Kind.BED, .2F, .2F, color));
            add(blocks, 219 + color, COLORS[color] + "_shulker_box", 315, Kind.SHULKER, 2, 2);
            add(blocks, 235 + color, COLORS[color] + "_glazed_terracotta", 335, Kind.GLAZED, 1.4F, 1.4F);
            blocks.add(new Definition(251, COLORS[color] + "_concrete", 335, Kind.CUBE, 1.8F, 1.8F, color));
            blocks.add(new Definition(252, COLORS[color] + "_concrete_powder", 335, Kind.POWDER, .5F, .5F, color));
        }
        add(blocks, 255, "structure_block", 107, Kind.STRUCTURE, -1, 3600000);
        BLOCKS = Collections.unmodifiableList(blocks);
        for (Definition block : blocks) for (int meta = 0; meta < 16; meta++) {
            if (block.acceptsMetadata(meta)) STATES[block.stateId(meta)] = block;
        }
        // Uncolored wire bed states use the vanilla red default until their NBT arrives.
        for (int meta = 0; meta < 16; meta++) STATES[26 << 4 | meta] = STATES[bedState(meta, 14)];
    }

    private static void add(List<Definition> blocks, int id, String name, int protocol, Kind kind, float hardness, float resistance) {
        blocks.add(new Definition(id, name, protocol, kind, hardness, resistance, -1));
    }

    public static Definition state(int state) {
        if (state < 0 || state >= STATE_LIMIT) return null;
        return STATES[state];
    }

    /** Connection-store representation only: never sent as a server block id. */
    public static int bedState(int meta, int color) { return 4096 + (color << 4) + (meta & 15); }
    public static boolean isBedState(int state) { return (state >> 4) == 26 || state >= 4096 && state < STATE_LIMIT; }

    public static final class Definition implements LegacyItemDefinition {
        public final int id, protocol, color;
        public final String name;
        public final Kind kind;
        public final float hardness, resistance;
        private Definition(int id, String name, int protocol, Kind kind, float hardness, float resistance, int color) {
            this.id = id; this.name = name; this.protocol = protocol; this.kind = kind;
            this.hardness = hardness; this.resistance = resistance; this.color = color;
        }
        public boolean acceptsMetadata(int meta) {
            if (meta < 0 || meta > 15) return false;
            if (kind == Kind.BED) return (meta & 8) != 0 || (meta & 4) == 0;
            if (color >= 0) return meta == color;
            switch (kind) {
                case ROD: case SHULKER: return meta < 6;
                case FLOWER: return meta <= 5;
                case PILLAR: return meta == 0 || meta == 4 || meta == 8;
                case STAIRS: return meta < 8;
                case SLAB: return meta == 0 || meta == 8;
                case CROP: case ICE: case STRUCTURE: case GLAZED: return meta < 4;
                case COMMAND: case OBSERVER: return (meta & 7) < 6;
                default: return meta == 0;
            }
        }
        public int itemId() {
            if (kind == Kind.GATEWAY || kind == Kind.ICE || kind == Kind.DOUBLE_SLAB) return -1;
            return kind == Kind.CROP ? 435 : kind == Kind.BED ? 355 : id;
        }
        public int stateId(int meta) { return kind == Kind.BED ? bedState(meta, color) : id << 4 | meta; }
        public int itemData() { return color < 0 ? 0 : color; }
        // 1.9 has a structure block but no corresponding inventory item yet.
        public int itemProtocol() { return kind == Kind.STRUCTURE ? 210 : protocol; }
    }
    private LegacyBlockCatalog() { }
}
