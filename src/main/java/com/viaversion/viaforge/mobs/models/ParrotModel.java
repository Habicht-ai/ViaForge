package com.viaversion.viaforge.mobs.models;
import com.viaversion.viaforge.mobs.ServerMob;
import net.minecraft.entity.*;
import net.minecraft.util.MathHelper;
public final class ParrotModel extends MobGeometry {
    public ParrotModel() { parrotGeometry(); }
    @Override public void setLivingAnimations(EntityLivingBase entity, float limb, float amount, float partial) {
        ServerMob mob = (ServerMob)entity;
        crest.rotateAngleX = -.2214F; body.rotateAngleX = .4937F;
        leftWing.rotateAngleX = rightWing.rotateAngleX = -.6981F;
        leftWing.rotateAngleY = rightWing.rotateAngleY = -(float)Math.PI;
        leg1.rotateAngleX = leg2.rotateAngleX = -.0299F;
        leg1.rotationPointY = leg2.rotationPointY = 22;
        leg1.rotateAngleZ = leg2.rotateAngleZ = 0;
        if (mob.jukebox != null) { leg1.rotateAngleZ = -.34906584F; leg2.rotateAngleZ = .34906584F; }
        else if (mob.sitting()) {
            head.rotationPointY = 17.59F; tail.rotateAngleX = 1.5388988F; tail.rotationPointY = 22.97F; body.rotationPointY = 18.4F;
            leftWing.rotateAngleZ = -.0873F; rightWing.rotateAngleZ = .0873F; leftWing.rotationPointY = rightWing.rotationPointY = 18.84F;
            leg1.rotationPointY = leg2.rotationPointY = 23.9F; leg1.rotateAngleX += (float)Math.PI / 2; leg2.rotateAngleX += (float)Math.PI / 2;
        } else if (!mob.onGround) { leg1.rotateAngleX += .6981317F; leg2.rotateAngleX += .6981317F; }
    }
    @Override public void setRotationAngles(float limb, float amount, float flap, float yaw, float pitch, float scale, Entity entity) {
        ServerMob mob = (ServerMob)entity;
        head.rotateAngleX = pitch / 57.295776F; head.rotateAngleY = yaw / 57.295776F; head.rotateAngleZ = 0;
        head.rotationPointX = body.rotationPointX = tail.rotationPointX = 0; rightWing.rotationPointX = -1.5F; leftWing.rotationPointX = 1.5F;
        if (mob.jukebox != null) {
            float x = MathHelper.cos(mob.ticksExisted), y = MathHelper.sin(mob.ticksExisted);
            head.rotationPointX = body.rotationPointX = tail.rotationPointX = x;
            head.rotationPointY = 15.69F + y; body.rotationPointY = 16.5F + y; tail.rotationPointY = 21.07F + y;
            head.rotateAngleX = head.rotateAngleY = 0; head.rotateAngleZ = y * .4F;
            leftWing.rotationPointX = 1.5F + x; rightWing.rotationPointX = -1.5F + x;
            leftWing.rotationPointY = rightWing.rotationPointY = 16.94F + y;
            leftWing.rotateAngleZ = -.0873F - flap; rightWing.rotateAngleZ = .0873F + flap;
            return;
        }
        if (mob.sitting()) return;
        if (mob.onGround) { leg1.rotateAngleX += MathHelper.cos(limb * .6662F) * 1.4F * amount; leg2.rotateAngleX += MathHelper.cos(limb * .6662F + (float)Math.PI) * 1.4F * amount; }
        float bob = flap * .3F;
        head.rotationPointY = 15.69F + bob; body.rotationPointY = 16.5F + bob;
        tail.rotateAngleX = 1.015F + MathHelper.cos(limb * .6662F) * .3F * amount; tail.rotationPointY = 21.07F + bob;
        leftWing.rotateAngleZ = -.0873F - flap; rightWing.rotateAngleZ = .0873F + flap;
        leftWing.rotationPointY = rightWing.rotationPointY = 16.94F + bob; leg1.rotationPointY = leg2.rotationPointY = 22 + bob;
    }
    @Override public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
        body.render(scale); leftWing.render(scale); rightWing.render(scale); tail.render(scale); head.render(scale); leg1.render(scale); leg2.render(scale);
    }
}
