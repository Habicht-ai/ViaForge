package com.viaversion.viaforge.mobs;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.*;

public final class ServerMobProjectileRenderer extends Render<ServerMobProjectile> {
    private final ModelBase model = new ModelBase() { };
    private final ModelRenderer bullet = new ModelRenderer(model), spit = new ModelRenderer(model), base = new ModelRenderer(model,0,0), jaw1 = new ModelRenderer(model,40,0), jaw2 = new ModelRenderer(model,40,0);
    public ServerMobProjectileRenderer(RenderManager manager) {
        super(manager);
        bullet.setTextureOffset(0,0).addBox(-4,-4,-1,8,8,2); bullet.setTextureOffset(0,10).addBox(-1,-4,-4,2,8,8); bullet.setTextureOffset(20,0).addBox(-4,-1,-4,8,2,8);
        for (int[] pos : new int[][]{{-4,0,0},{0,-4,0},{0,0,-4},{0,0,0},{2,0,0},{0,2,0},{0,0,2}}) spit.setTextureOffset(0,0).addBox(pos[0],pos[1],pos[2],2,2,2);
        base.setRotationPoint(-5,22,-5); base.addBox(0,0,0,10,12,10);
        jaw1.setRotationPoint(1.5F,22,-4); jaw1.addBox(0,0,0,4,14,8);
        jaw2.setRotationPoint(-1.5F,22,4); jaw2.addBox(0,0,0,4,14,8);
    }
    @Override protected ResourceLocation getEntityTexture(ServerMobProjectile entity) {
        return new ResourceLocation("viaforge","textures/entity/"+(entity.type == 67 ? "shulker/spark.png" : entity.type == 68 ? "llama/spit.png" : entity.type == 79 ? "illager/fangs.png" : "enderdragon/dragon_fireball.png"));
    }
    @Override public void doRender(ServerMobProjectile entity,double x,double y,double z,float yaw,float partial) {
        float progress = entity.fangProgress(partial);
        if (entity.type == 79 && progress == 0) return;
        bindEntityTexture(entity); GlStateManager.pushMatrix(); GlStateManager.translate(x,y,z);
        try {
            if (entity.type == 67) {
                float age = entity.ticksExisted+partial;
                GlStateManager.translate(0,.15F,0); GlStateManager.rotate(MathHelper.sin(age*.1F)*180,0,1,0); GlStateManager.rotate(MathHelper.cos(age*.1F)*180,1,0,0); GlStateManager.rotate(MathHelper.sin(age*.15F)*360,0,0,1);
                GlStateManager.enableRescaleNormal(); GlStateManager.scale(-1,-1,1);
                bullet.rotateAngleY = (entity.prevRotationYaw+MathHelper.wrapAngleTo180_float(entity.rotationYaw-entity.prevRotationYaw)*partial)/57.295776F;
                bullet.rotateAngleX = (entity.prevRotationPitch+(entity.rotationPitch-entity.prevRotationPitch)*partial)/57.295776F;
                bullet.render(.03125F);
                GlStateManager.enableBlend(); GlStateManager.blendFunc(770,771); GlStateManager.color(1,1,1,.5F); GlStateManager.scale(1.5F,1.5F,1.5F); bullet.render(.03125F);
            } else if (entity.type == 68) {
                GlStateManager.translate(0,.15F,0); GlStateManager.rotate(entity.prevRotationYaw+(entity.rotationYaw-entity.prevRotationYaw)*partial-90,0,1,0); GlStateManager.rotate(entity.prevRotationPitch+(entity.rotationPitch-entity.prevRotationPitch)*partial,0,0,1); spit.render(.0625F);
            } else if (entity.type == 79) {
                float size = progress > .9F ? 2*(1-progress)/.1F : 2;
                GlStateManager.disableCull(); GlStateManager.enableAlpha(); GlStateManager.rotate(90-entity.rotationYaw,0,1,0); GlStateManager.scale(-size,-size,size); GlStateManager.translate(0,-.626F,0);
                float opening = Math.min(progress*2,1); opening = 1-opening*opening*opening;
                jaw1.rotateAngleZ = (float)Math.PI-opening*.35F*(float)Math.PI; jaw2.rotateAngleZ = (float)Math.PI+opening*.35F*(float)Math.PI; jaw2.rotateAngleY = (float)Math.PI;
                base.rotationPointY = jaw1.rotationPointY = jaw2.rotationPointY = 24-(progress+MathHelper.sin(progress*2.7F))*.6F*12;
                base.render(.03125F); jaw1.render(.03125F); jaw2.render(.03125F);
            } else {
                GlStateManager.enableRescaleNormal(); GlStateManager.scale(2,2,2); GlStateManager.rotate(180-renderManager.playerViewY,0,1,0);
                GlStateManager.rotate((renderManager.options.thirdPersonView == 2 ? -1 : 1)*-renderManager.playerViewX,1,0,0);
                WorldRenderer vertices = Tessellator.getInstance().getWorldRenderer(); vertices.begin(7,DefaultVertexFormats.POSITION_TEX_NORMAL);
                vertices.pos(-.5,-.25,0).tex(0,1).normal(0,1,0).endVertex(); vertices.pos(.5,-.25,0).tex(1,1).normal(0,1,0).endVertex();
                vertices.pos(.5,.75,0).tex(1,0).normal(0,1,0).endVertex(); vertices.pos(-.5,.75,0).tex(0,0).normal(0,1,0).endVertex(); Tessellator.getInstance().draw();
            }
        } finally { GlStateManager.color(1,1,1,1); GlStateManager.disableBlend(); GlStateManager.enableCull(); GlStateManager.disableRescaleNormal(); GlStateManager.popMatrix(); }
        super.doRender(entity,x,y,z,yaw,partial);
    }
}
