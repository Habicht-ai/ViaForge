package com.viaversion.viaforge.hands;

import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.Entity;

/** Body poses must not override RenderPlayer's neutral first-person arm. */
public final class FirstPersonArm {
    private static int depth;

    public static boolean active() { return depth != 0; }

    public static void angles(ModelPlayer model, float limb, float amount, float age,
                              float yaw, float pitch, float scale, Entity entity) {
        depth++;
        try {
            model.setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
        } finally {
            depth--;
        }
    }

    private FirstPersonArm() { }
}
