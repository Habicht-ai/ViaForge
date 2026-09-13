package com.viaversion.viaforge.mobs.models;
import com.viaversion.viaforge.mobs.ServerMob;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
public final class QuadrupedMobModel extends MobGeometry {
    private final boolean llama;
    public QuadrupedMobModel(boolean llama, float inflate) {
        this.llama = llama;
        if (llama) llamaGeometry(inflate); else polarBearGeometry();
    }
    @Override public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
        head.rotateAngleX = pitch / 57.295776F; head.rotateAngleY = yaw / 57.295776F;
        body.rotateAngleX = (float)Math.PI / 2;
        leg1.rotateAngleX = leg4.rotateAngleX = MathHelper.cos(limb * .6662F) * 1.4F * amount;
        leg2.rotateAngleX = leg3.rotateAngleX = MathHelper.cos(limb * .6662F + (float)Math.PI) * 1.4F * amount;
        if (!llama) {
            float standing = ((ServerMob)entity).standing(age - entity.ticksExisted); standing *= standing;
            body.rotateAngleX -= standing * (float)Math.PI * .35F; body.rotationPointY = 9 + standing * 2;
            leg3.rotationPointY = leg4.rotationPointY = 14 - standing * 20;
            leg3.rotationPointZ = leg4.rotationPointZ = -8 + standing * 4;
            leg3.rotateAngleX -= standing * (float)Math.PI * .45F; leg4.rotateAngleX -= standing * (float)Math.PI * .45F;
            head.rotationPointY = 10 - standing * 22; head.rotationPointZ = -16 + standing * 13;
            head.rotateAngleX += standing * (float)Math.PI * .15F;
        }
    }
    @Override public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
        setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
        if (!isChild) { head.render(scale); body.render(scale); legs(scale); }
        else if (llama) {
            GlStateManager.pushMatrix(); GlStateManager.scale(.71428573F, .64935064F, .7936508F); GlStateManager.translate(0, 21 * scale, .22F); head.render(scale); GlStateManager.popMatrix();
            GlStateManager.pushMatrix(); GlStateManager.scale(.625F, .45454544F, .45454544F); GlStateManager.translate(0, 33 * scale, 0); body.render(scale); GlStateManager.popMatrix();
            GlStateManager.pushMatrix(); GlStateManager.scale(.45454544F, .41322312F, .45454544F); GlStateManager.translate(0, 33 * scale, 0); legs(scale); GlStateManager.popMatrix();
        } else {
            GlStateManager.pushMatrix(); GlStateManager.scale(.6666667F, .6666667F, .6666667F); GlStateManager.translate(0, 16 * scale, 4 * scale); head.render(scale); GlStateManager.popMatrix();
            GlStateManager.pushMatrix(); GlStateManager.scale(.5F, .5F, .5F); GlStateManager.translate(0, 24 * scale, 0); body.render(scale); legs(scale); GlStateManager.popMatrix();
        }
        if (llama && !isChild && ((ServerMob)entity).state.flag(((ServerMob)entity).state.first + 3)) { leftChest.render(scale); rightChest.render(scale); }
    }
    private void legs(float scale) { leg1.render(scale); leg2.render(scale); leg3.render(scale); leg4.render(scale); }
}
