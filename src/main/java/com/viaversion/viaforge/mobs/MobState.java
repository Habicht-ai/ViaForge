package com.viaversion.viaforge.mobs;

import com.viaversion.viaforge.common.blocks.MobKind;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import java.util.*;

/** Original metadata is kept apart from 1.8's incompatible DataWatcher slots. */
public final class MobState {
    public final int protocol, wireType, first;
    public final Map<Integer, Object> data = new HashMap<>();
    public MobKind kind;
    public MobState(int protocol, int type) {
        this.protocol = protocol; this.wireType = type; first = protocol >= 210 ? 12 : 11;
        kind = MobKind.resolve(protocol, type, data);
    }
    public void update(List<EntityData> entries) {
        for (EntityData entry : entries) data.put(entry.id(), entry.getValue());
        kind = MobKind.resolve(protocol, wireType, data);
    }
    public int number(int id, int fallback) { return MobKind.number(data.get(id), fallback); }
    public boolean flag(int id) { return Boolean.TRUE.equals(data.get(id)); }
    public boolean baby() { return (kind == MobKind.POLAR_BEAR || kind == MobKind.LLAMA || kind == MobKind.PARROT || kind.zombie()) && flag(first); }
    public boolean leftHanded() { return (number(first - 1, 0) & 2) != 0; }
    public boolean armsRaised() {
        if (kind.skeleton()) return flag(first + (protocol < 315 ? 1 : 0));
        if (kind.zombie()) return flag(first + (protocol < 315 ? 3 : 2));
        return (number(first, 0) & 1) != 0;
    }
    public int spell() { return kind == MobKind.EVOKER || kind == MobKind.ILLUSIONER ? number(first + (protocol >= 335 ? 1 : 0), 0) : 0; }
    public int profession() { return protocol >= 315 ? number(first + 4, 0) : Math.max(0, number(first + 1, 1) - 1); }
    public boolean converting() { return flag(first + (protocol < 315 ? 2 : 3)); }
}
