package com.viaversion.viaforge.mobs.models;
import com.viaversion.viaforge.mobs.ServerMob;
import com.viaversion.viaforge.common.blocks.MobKind;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
public final class IllagerModel extends MobGeometry {
    private boolean folded;
    private final net.minecraft.client.model.ModelRenderer oldArms;
    public IllagerModel() {
        illagerGeometry();
        oldArms = new net.minecraft.client.model.ModelRenderer(this).setTextureSize(64,64);
        oldArms.setRotationPoint(0,3,-1); oldArms.rotateAngleX = -.75F;
        oldArms.setTextureOffset(44,22).addBox(-8,-2,-2,4,8,4);
        oldArms.setTextureOffset(44,22).addBox(4,-2,-2,4,8,4);
        oldArms.setTextureOffset(40,38).addBox(-4,2,-2,8,4,4);
    }
    @Override public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
        ServerMob mob = (ServerMob)entity;
        hat.showModel = mob.kind() == MobKind.ILLUSIONER;
        head.rotateAngleY = yaw / 57.295776F; head.rotateAngleX = pitch / 57.295776F;
        arms.rotationPointY = 3; arms.rotationPointZ = -1; arms.rotateAngleX = -.75F;
        leg1.rotateAngleX = MathHelper.cos(limb * .6662F) * 1.4F * amount * .5F;
        leg2.rotateAngleX = MathHelper.cos(limb * .6662F + (float)Math.PI) * 1.4F * amount * .5F;
        leg1.rotateAngleY = leg2.rotateAngleY = 0;
        rightArm.setRotationPoint(-5,2,0); leftArm.setRotationPoint(5,2,0);
        rightArm.rotateAngleX = rightArm.rotateAngleY = rightArm.rotateAngleZ = leftArm.rotateAngleX = leftArm.rotateAngleY = leftArm.rotateAngleZ = 0;
        folded = true;
        if (mob.state.spell() != 0) {
            folded = false;
            rightArm.rotateAngleX = leftArm.rotateAngleX = MathHelper.cos(age * .6662F) * .25F;
            rightArm.rotateAngleZ = 2.3561945F; leftArm.rotateAngleZ = -2.3561945F;
        } else if (mob.kind() == MobKind.ILLUSIONER && mob.state.armsRaised()) {
            folded = false;
            rightArm.rotateAngleY = -.1F + head.rotateAngleY; rightArm.rotateAngleX = -(float)Math.PI / 2 + head.rotateAngleX;
            leftArm.rotateAngleX = -.9424779F + head.rotateAngleX; leftArm.rotateAngleY = head.rotateAngleY - .4F; leftArm.rotateAngleZ = (float)Math.PI / 2;
        } else if (mob.kind() == MobKind.VINDICATOR && mob.state.armsRaised()) {
            folded = false;
            float swing = MathHelper.sin(swingProgress * (float)Math.PI), ease = MathHelper.sin((1 - (1 - swingProgress) * (1 - swingProgress)) * (float)Math.PI);
            rightArm.rotateAngleY = .15707964F; leftArm.rotateAngleY = -.15707964F;
            net.minecraft.client.model.ModelRenderer main = mob.state.leftHanded() ? leftArm : rightArm, other = mob.state.leftHanded() ? rightArm : leftArm;
            main.rotateAngleX = -1.8849558F + MathHelper.cos(age * .09F) * .15F + swing * 2.2F - ease * .4F;
            other.rotateAngleX = MathHelper.cos(age * .19F) * .5F + swing * 1.2F - ease * .4F;
            rightArm.rotateAngleZ += MathHelper.cos(age * .09F) * .05F + .05F; leftArm.rotateAngleZ -= MathHelper.cos(age * .09F) * .05F + .05F;
            rightArm.rotateAngleX += MathHelper.sin(age * .067F) * .05F; leftArm.rotateAngleX -= MathHelper.sin(age * .067F) * .05F;
        }
    }
    @Override public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
        setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
        head.render(scale); body.render(scale); leg1.render(scale); leg2.render(scale);
        if (folded) (!com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.UPDATED_ILLAGER_ARMS) ? oldArms : arms).render(scale); else { rightArm.render(scale); leftArm.render(scale); }
    }
}
