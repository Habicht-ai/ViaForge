package com.viaversion.viaforge.mobs.models;

import com.viaversion.viaforge.mobs.ServerMob;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

/** 1.19.3's 32x32 Vex mesh, pivots and animation, using original target texels. */
public final class SmallVexModel extends ModelBiped {
    private final ModelRenderer rightWing,leftWing;
    public SmallVexModel() {
        super(0,0,32,32);
        bipedHead=box(0,0,-2.5F,-5,-2.5F,5,5,5,0,0,20,0,false);
        bipedBody=box(0,10,-1.5F,0,-1,3,4,2,0,0,20,0,false);
        bipedBody.setTextureOffset(0,16).addBox(-1.5F,1,-1,3,5,2,-.2F);
        bipedRightArm=box(23,0,-1.25F,-.5F,-1,2,4,2,-.1F,-1.75F,.25F,0,false);
        bipedLeftArm=box(23,6,-.75F,-.5F,-1,2,4,2,-.1F,1.75F,.25F,0,false);
        leftWing=box(16,14,0,0,0,0,5,8,0,.5F,1,1,true);
        rightWing=box(16,14,0,0,0,0,5,8,0,-.5F,1,1,false);
        bipedBody.addChild(bipedRightArm);bipedBody.addChild(bipedLeftArm);bipedBody.addChild(leftWing);bipedBody.addChild(rightWing);
        bipedHeadwear.showModel=bipedLeftLeg.showModel=bipedRightLeg.showModel=false;
    }
    private ModelRenderer box(int u,int v,float x,float y,float z,int w,int h,int d,float inflate,float px,float py,float pz,boolean mirror) {
        ModelRenderer part=new ModelRenderer(this,u,v);part.mirror=mirror;part.addBox(x,y,z,w,h,d,inflate);part.setRotationPoint(px,py,pz);return part;
    }
    @Override public void setRotationAngles(float limb,float amount,float age,float yaw,float pitch,float scale,Entity entity) {
        bipedHead.rotateAngleY=yaw*(float)Math.PI/180;bipedHead.rotateAngleX=pitch*(float)Math.PI/180;
        float idle=.62831855F+MathHelper.cos(age*5.5F*(float)Math.PI/180)*.1F;
        boolean charging=((ServerMob)entity).charging();bipedBody.rotateAngleX=charging?0:.15707964F;
        bipedRightArm.rotateAngleX=charging?3.6651914F:0;bipedRightArm.rotateAngleY=charging?.2617994F:0;bipedRightArm.rotateAngleZ=charging?-.47123888F:idle;
        bipedLeftArm.rotateAngleX=bipedLeftArm.rotateAngleY=0;bipedLeftArm.rotateAngleZ=-idle;
        leftWing.rotateAngleY=1.0995574F+MathHelper.cos(age*45.836624F*(float)Math.PI/180)*(float)Math.PI/180*16.2F;
        rightWing.rotateAngleY=-leftWing.rotateAngleY;leftWing.rotateAngleX=rightWing.rotateAngleX=.47123888F;
        leftWing.rotateAngleZ=-.47123888F;rightWing.rotateAngleZ=.47123888F;
    }
    @Override public void render(Entity entity,float limb,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(limb,amount,age,yaw,pitch,scale,entity);
        GlStateManager.pushMatrix();try{GlStateManager.translate(0,-2.5F*scale,0);bipedHead.render(scale);bipedBody.render(scale);}finally{GlStateManager.popMatrix();}
    }
    public void beforeHand(){GlStateManager.translate(0,-2.5F/16,0);bipedBody.postRender(1F/16);}
    public void afterHand(boolean left){GlStateManager.scale(.55F,.55F,.55F);GlStateManager.translate(left?-.046875F:.046875F,-.15625F,.078125F);}
}
