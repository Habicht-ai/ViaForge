package com.viaversion.viaforge.items;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/** Vanilla's swimming stroke, also used for crawling on land since 1.14. */
public final class ServerCrawlingModel {
    public static void apply(ModelBiped model, Entity entity, float animation, float age) {
        if(!(entity instanceof EntityPlayer))return;
        float amount=ServerElytraVisuals.crawlAmount(entity,MathHelper.clamp_float(age-entity.ticksExisted,0,1));
        if(amount<=0||ServerElytraFlight.flying(entity))return;
        EntityPlayer player=(EntityPlayer)entity;
        if(ServerElytraFlight.crawling(entity)||com.viaversion.viaforge.compatibility.ServerSwimming.pose(entity)||com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.FIXED_CRAWLING_HEAD))
            model.bipedHead.rotateAngleX=rotate(amount,model.bipedHead.rotateAngleX,-.7853982F);
        ModelBiped.copyModelAngles(model.bipedHead,model.bipedHeadwear);
        boolean modern=com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.DOUBLE_SWIM_INPUT);
        if(!modern||!player.isUsingItem()) {
            boolean swingLeft=(com.viaversion.viaforge.hands.Offhand.swingHand==1)^com.viaversion.viaforge.hands.Offhand.mainLeft(player);
            float left=modern&&player.isSwingInProgress&&swingLeft?0:amount;
            float right=player.isSwingInProgress&&(!modern||!swingLeft)?0:amount;
            float phase=animation%26, x, lz, rz;
            if(phase<14) {
                x=0;float arc=1.8707964F*(-65*phase+phase*phase)/(-65*14+14*14);
                lz=(float)Math.PI+arc;rz=(float)Math.PI-arc;
            } else if(phase<22) {
                float t=(phase-14)/8;x=1.5707964F*t;
                lz=5.012389F-1.8707964F*t;rz=1.2707963F+1.8707964F*t;
            } else {x=1.5707964F-1.5707964F*((phase-22)/4);lz=rz=(float)Math.PI;}
            arm(model.bipedLeftArm,left,x,lz,true);arm(model.bipedRightArm,right,x,rz,false);
        }
        model.bipedLeftLeg.rotateAngleX=lerp(amount,model.bipedLeftLeg.rotateAngleX,.3F*MathHelper.cos(animation*.33333334F+(float)Math.PI));
        model.bipedRightLeg.rotateAngleX=lerp(amount,model.bipedRightLeg.rotateAngleX,.3F*MathHelper.cos(animation*.33333334F));
    }
    private static void arm(ModelRenderer arm,float amount,float x,float z,boolean angular) {
        arm.rotateAngleX=angular?rotate(amount,arm.rotateAngleX,x):lerp(amount,arm.rotateAngleX,x);
        arm.rotateAngleY=angular?rotate(amount,arm.rotateAngleY,(float)Math.PI):lerp(amount,arm.rotateAngleY,(float)Math.PI);
        arm.rotateAngleZ=angular?rotate(amount,arm.rotateAngleZ,z):lerp(amount,arm.rotateAngleZ,z);
    }
    private static float lerp(float a,float from,float to){return from+(to-from)*a;}
    private static float rotate(float a,float from,float to){
        float delta=(to-from)%((float)Math.PI*2);
        if(delta<-(float)Math.PI)delta+=(float)Math.PI*2;
        if(delta>=(float)Math.PI)delta-=(float)Math.PI*2;
        return from+a*delta;
    }
    private ServerCrawlingModel() { }
}
