package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.connection.StorableObject;

/** Original click rotation, scoped to one synchronous translation through Via. */
public final class InteractionRotation implements StorableObject {
    public final float yaw, pitch;
    public InteractionRotation(float yaw,float pitch){this.yaw=yaw;this.pitch=pitch;}
}
