package com.viaversion.viaforge.mobs.models;
import com.viaversion.viaforge.mobs.ServerMob;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
public final class ShulkerModel extends MobGeometry {
    public ShulkerModel() { shulkerGeometry(); }
    @Override public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
        ServerMob mob = (ServerMob)entity;
        float peek = mob.peek(age - mob.ticksExisted), angle = (.5F + peek) * (float)Math.PI;
        float wave = MathHelper.sin(angle), twist = wave - 1;
        lid.setRotationPoint(0, 16 + wave * 8 + (angle > Math.PI ? MathHelper.sin(age * .1F) * .7F : 0), 0);
        lid.rotateAngleY = peek > .3F ? twist * twist * twist * twist * (float)Math.PI * .125F : 0;
        head.rotateAngleX = pitch / 57.295776F; head.rotateAngleY = yaw / 57.295776F;
    }
    @Override public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
        bottom.render(scale); lid.render(scale);
        // The head remains world-oriented; the renderer draws it in its own layer.
    }
}
