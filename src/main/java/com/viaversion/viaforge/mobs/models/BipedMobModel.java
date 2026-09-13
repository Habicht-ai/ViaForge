package com.viaversion.viaforge.mobs.models;
import com.viaversion.viaforge.mobs.ServerMob;
import com.viaversion.viaforge.common.blocks.MobKind;
import net.minecraft.client.model.*;
import net.minecraft.entity.*;
import net.minecraft.init.Items;
import net.minecraft.util.MathHelper;

public final class BipedMobModel extends ModelBiped {
    private final boolean armor;
    private ModelRenderer leftWing, rightWing;
    public BipedMobModel(MobKind kind, float inflate, boolean armor) {
        super(inflate, 0, 64, armor || kind.skeleton() ? 32 : 64);
        this.armor = armor;
        if (kind.skeleton() && !armor) {
            bipedRightArm = box(40,16,-1,-2,-1,2,12,2,inflate,-5,2,0,false);
            bipedLeftArm = box(40,16,-1,-2,-1,2,12,2,inflate,5,2,0,true);
            bipedRightLeg = box(0,16,-1,0,-1,2,12,2,inflate,-2,12,0,false);
            bipedLeftLeg = box(0,16,-1,0,-1,2,12,2,inflate,2,12,0,true);
        } else if (kind == MobKind.ZOMBIE_VILLAGER) {
            if (armor) {
                bipedHead = box(0,0,-4,-10,-4,8,8,8,inflate,0,0,0,false);
                bipedBody = box(16,16,-4,0,-2,8,12,4,inflate+.1F,0,0,0,false);
                bipedRightLeg = box(0,16,-2,0,-2,4,12,4,inflate+.1F,-2,12,0,false);
                bipedLeftLeg = box(0,16,-2,0,-2,4,12,4,inflate+.1F,2,12,0,true);
            } else {
                bipedHead = box(0,0,-4,-10,-4,8,10,8,inflate,0,0,0,false);
                bipedHead.setTextureOffset(24,0).addBox(-1,-3,-6,2,4,2,inflate);
                bipedBody = box(16,20,-4,0,-3,8,12,6,inflate,0,0,0,false);
                bipedBody.setTextureOffset(0,38).addBox(-4,0,-3,8,18,6,inflate+.05F);
                bipedRightArm = box(44,38,-3,-2,-2,4,12,4,inflate,-5,2,0,false);
                bipedLeftArm = box(44,38,-1,-2,-2,4,12,4,inflate,5,2,0,true);
                bipedRightLeg = box(0,22,-2,0,-2,4,12,4,inflate,-2,12,0,false);
                bipedLeftLeg = box(0,22,-2,0,-2,4,12,4,inflate,2,12,0,true);
            }
        } else if (kind == MobKind.VEX) {
            bipedHeadwear.showModel = bipedLeftLeg.showModel = false;
            bipedRightLeg = box(32,0,-1,-1,-2,6,10,4,0,-1.9F,12,0,false);
            rightWing = box(0,32,-20,0,0,20,12,1,0,0,1,2,false);
            leftWing = box(0,32,0,0,0,20,12,1,0,0,1,2,true);
        }
    }
    private ModelRenderer box(int u,int v,float x,float y,float z,int w,int h,int d,float inflate,float px,float py,float pz,boolean mirror) {
        ModelRenderer part = new ModelRenderer(this,u,v); part.mirror = mirror; part.addBox(x,y,z,w,h,d,inflate); part.setRotationPoint(px,py,pz); return part;
    }
    @Override public void setLivingAnimations(EntityLivingBase entity, float limb, float amount, float partial) {
        ServerMob mob = (ServerMob)entity;
        heldItemRight = (mob.state.leftHanded() ? mob.offhand : mob.getHeldItem()) == null ? 0 : 1;
        heldItemLeft = (mob.state.leftHanded() ? mob.getHeldItem() : mob.offhand) == null ? 0 : 1;
        aimedBow = false; isSneak = mob.isSneaking();
    }
    @Override public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
        super.setRotationAngles(limb,amount,age,yaw,pitch,scale,entity);
        ServerMob mob = (ServerMob)entity;
        float swing = MathHelper.sin(swingProgress * (float)Math.PI), ease = MathHelper.sin((1 - (1-swingProgress)*(1-swingProgress)) * (float)Math.PI);
        boolean bow = mob.kind().skeleton() && mob.getHeldItem() != null && mob.getHeldItem().getItem() == Items.bow;
        if (mob.kind().zombie() || mob.kind().skeleton() && !bow && mob.state.armsRaised()) {
            bipedRightArm.rotateAngleZ = bipedLeftArm.rotateAngleZ = 0;
            bipedRightArm.rotateAngleY = -(0.1F - swing*.6F); bipedLeftArm.rotateAngleY = .1F - swing*.6F;
            float angle = mob.kind().zombie() ? -(float)Math.PI / (mob.state.armsRaised() ? 1.5F : 2.25F) : -(float)Math.PI / 2;
            bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = angle;
            float attack = swing*1.2F - ease*.4F;
            bipedRightArm.rotateAngleX += mob.kind().zombie() ? attack : -attack;
            bipedLeftArm.rotateAngleX += mob.kind().zombie() ? attack : -attack;
            idle(age);
        } else if (bow && mob.state.armsRaised()) {
            float sign = mob.state.leftHanded() ? -1 : 1;
            ModelRenderer main = mob.state.leftHanded() ? bipedLeftArm : bipedRightArm, other = mob.state.leftHanded() ? bipedRightArm : bipedLeftArm;
            main.rotateAngleY = -.1F*sign + bipedHead.rotateAngleY;
            other.rotateAngleY = .1F*sign + bipedHead.rotateAngleY + .4F*sign;
            main.rotateAngleX = other.rotateAngleX = -(float)Math.PI/2 + bipedHead.rotateAngleX;
            main.rotateAngleZ = other.rotateAngleZ = 0; idle(age);
        }
        if (mob.kind() == MobKind.VEX && !armor) {
            if (mob.charging()) (mob.state.leftHanded() ? bipedLeftArm : bipedRightArm).rotateAngleX = 3.7699115F;
            bipedRightLeg.rotateAngleX += .62831855F;
            rightWing.rotateAngleY = .47123894F + MathHelper.cos(age*.8F)*(float)Math.PI*.05F;
            leftWing.rotateAngleY = -rightWing.rotateAngleY;
            leftWing.rotateAngleZ = -.47123894F; rightWing.rotateAngleZ = .47123894F;
            leftWing.rotateAngleX = rightWing.rotateAngleX = .47123894F;
        }
        copyModelAngles(bipedHead,bipedHeadwear);
    }
    private void idle(float age) {
        bipedRightArm.rotateAngleZ += MathHelper.cos(age*.09F)*.05F+.05F; bipedLeftArm.rotateAngleZ -= MathHelper.cos(age*.09F)*.05F+.05F;
        bipedRightArm.rotateAngleX += MathHelper.sin(age*.067F)*.05F; bipedLeftArm.rotateAngleX -= MathHelper.sin(age*.067F)*.05F;
    }
    @Override public void render(Entity entity,float limb,float amount,float age,float yaw,float pitch,float scale) {
        super.render(entity,limb,amount,age,yaw,pitch,scale);
        if (leftWing != null) { leftWing.render(scale); rightWing.render(scale); }
    }
}
