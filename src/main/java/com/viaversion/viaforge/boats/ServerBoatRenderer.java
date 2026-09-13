package com.viaversion.viaforge.boats;

import net.minecraft.client.model.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.*;

public final class ServerBoatRenderer extends Render<ServerBoat> {
    private static final String[] WOODS={"oak","spruce","birch","jungle","acacia","darkoak"};
    public final ModelRenderer[] hull=new ModelRenderer[5], oars=new ModelRenderer[2];
    private final ModelRenderer waterMask;
    public ServerBoatRenderer(RenderManager manager) {
        super(manager); shadowSize=.5F;
        ModelBase model=new ModelBase() { }; model.textureWidth=128; model.textureHeight=64;
        int[] uv={0,19,27,35,43}; for(int i=0;i<5;i++) hull[i]=new ModelRenderer(model,0,uv[i]);
        hull[0].addBox(-14,-9,-3,28,16,3); hull[0].setRotationPoint(0,3,1); hull[0].rotateAngleX=(float)Math.PI/2;
        hull[1].addBox(-13,-7,-1,18,6,2); hull[1].setRotationPoint(-15,4,4); hull[1].rotateAngleY=(float)Math.PI*1.5F;
        hull[2].addBox(-8,-7,-1,16,6,2); hull[2].setRotationPoint(15,4,0); hull[2].rotateAngleY=(float)Math.PI/2;
        hull[3].addBox(-14,-7,-1,28,6,2); hull[3].setRotationPoint(0,4,-9); hull[3].rotateAngleY=(float)Math.PI;
        hull[4].addBox(-14,-7,-1,28,6,2); hull[4].setRotationPoint(0,4,9);
        for(int i=0;i<2;i++) {
            oars[i]=new ModelRenderer(model,62,i*20); oars[i].addBox(-1,0,-5,2,2,18);
            oars[i].addBox(i==0 ? -1.001F:.001F,-3,8,1,6,7); oars[i].setRotationPoint(3,-5,i==0?9:-9); oars[i].rotateAngleZ=.19634955F;
        }
        waterMask=new ModelRenderer(model,0,0); waterMask.addBox(-14,-9,-3,28,16,3); waterMask.setRotationPoint(0,-3,1); waterMask.rotateAngleX=(float)Math.PI/2;
    }
    @Override public ResourceLocation getEntityTexture(ServerBoat boat) { return new ResourceLocation("viaforge","textures/entity/boat/boat_"+WOODS[boat.wood]+".png"); }
    @Override public void doRender(ServerBoat boat,double x,double y,double z,float yaw,float partial) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x,y+.375,z); GlStateManager.rotate(180-yaw,0,1,0);
            float time=boat.hurtTime-partial, damage=Math.max(boat.damage-partial,0);
            if(time>0) GlStateManager.rotate(MathHelper.sin(time)*time*damage/10*boat.hurtDirection,1,0,0);
            GlStateManager.scale(-1,-1,1); GlStateManager.rotate(90,0,1,0); bindEntityTexture(boat);
            for(ModelRenderer part:hull) part.render(.0625F);
            for(int i=0;i<2;i++) { float phase=boat.paddlePhase(i,partial);
                oars[i].rotateAngleX=lerp(-1.0471975803375244,-.2617993950843811,(MathHelper.sin(-phase)+1)/2);
                oars[i].rotateAngleY=lerp(-.7853981852531433,.7853981852531433,(MathHelper.sin(-phase+1)+1)/2);
                if(i==1) oars[i].rotateAngleY=(float)Math.PI-oars[i].rotateAngleY;
                oars[i].render(.0625F);
            }
            // Write depth before the translucent water pass, keeping the hull dry.
            GlStateManager.colorMask(false,false,false,false); waterMask.render(.0625F);
        } finally { GlStateManager.colorMask(true,true,true,true); GlStateManager.popMatrix(); }
        super.doRender(boat,x,y,z,yaw,partial);
    }
    private static float lerp(double from,double to,float progress) { return (float)(from+(to-from)*progress); }
}
