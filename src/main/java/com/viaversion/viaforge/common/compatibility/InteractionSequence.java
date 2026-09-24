package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.connection.StorableObject;
import java.util.Objects;

/** ClientLevel's prediction counter: shared by item use and start/finish digging. */
public final class InteractionSequence implements StorableObject {
    private String world;
    private int sequence;
    public void worldChanged(String currentWorld) {
        if(!Objects.equals(world,currentWorld)){world=currentWorld;sequence=0;}
    }
    public int next(String currentWorld) {
        worldChanged(currentWorld);
        return ++sequence;
    }
}
