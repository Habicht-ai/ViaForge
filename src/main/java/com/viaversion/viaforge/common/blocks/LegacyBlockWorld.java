package com.viaversion.viaforge.common.blocks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Original server states, owned exclusively by one connection's Netty event loop. */
public final class LegacyBlockWorld {
    public static final int UNKNOWN = -1;
    private static final char UNKNOWN_STATE = '\uffff';
    private final Map<Long, Column> columns = new HashMap<>();

    public void replaceChunk(int x, int z, boolean full, char[][] sections) {
        if (sections.length != 16) throw new IllegalArgumentException("Expected 16 legacy sections");
        Column column = full ? new Column(true) : columns.get(key(x, z));
        if (column == null) column = new Column(false);
        for (int i = 0; i < 16; i++) {
            if (sections[i] != null) {
                if (sections[i].length != 4096) throw new IllegalArgumentException("Invalid section size");
                char[] replacement = sections[i].clone();
                if (!full && column.sections[i] != null) for (int j = 0; j < 4096; j++) {
                    replacement[j] = (char) preserveBedColor(replacement[j], column.sections[i][j]);
                }
                column.sections[i] = replacement;
            }
        }
        columns.put(key(x, z), column);
    }

    public int get(int x, int y, int z) {
        if (y < 0 || y > 255) return UNKNOWN;
        Column column = columns.get(key(x >> 4, z >> 4));
        if (column == null) return UNKNOWN;
        char[] section = column.sections[y >> 4];
        if (section == null) return column.full ? 0 : UNKNOWN;
        char state = section[index(x, y, z)];
        return state == UNKNOWN_STATE ? UNKNOWN : state;
    }

    public void set(int x, int y, int z, int state) {
        if (y < 0 || y > 255 || state < 0 || state >= UNKNOWN_STATE) return;
        // Ignore changes outside loaded chunks instead of retaining unbounded stray updates.
        Column column = columns.get(key(x >> 4, z >> 4));
        if (column == null) return;
        char[] section = column.sections[y >> 4];
        if (section == null) {
            section = new char[4096];
            if (!column.full) Arrays.fill(section, UNKNOWN_STATE);
            column.sections[y >> 4] = section;
        }
        int index = index(x, y, z);
        section[index] = (char) preserveBedColor(state, section[index]);
    }

    private static int preserveBedColor(int state, int previous) {
        if (state >> 4 == 26 && previous >= 4096 && previous < LegacyBlockCatalog.STATE_LIMIT
                && (state & 11) == (previous & 11)) return (previous & ~15) | (state & 15);
        return state;
    }

    public boolean setBedColor(int x, int y, int z, int color) {
        int state = get(x, y, z);
        if (color < 0 || color > 15 || !LegacyBlockCatalog.isBedState(state)) return false;
        set(x, y, z, LegacyBlockCatalog.bedState(state & 15, color));
        return true;
    }

    public void unload(int x, int z) { columns.remove(key(x, z)); }
    public void clear() { columns.clear(); }
    public int chunkCount() { return columns.size(); }

    private static int index(int x, int y, int z) { return ((y & 15) << 8) | ((z & 15) << 4) | (x & 15); }
    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }

    private static final class Column {
        final boolean full;
        final char[][] sections = new char[16][];
        Column(boolean full) { this.full = full; }
    }
}
