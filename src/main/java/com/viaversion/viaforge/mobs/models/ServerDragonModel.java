package com.viaversion.viaforge.mobs.models;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(value=Side.CLIENT)
public final class ServerDragonModel
extends net.minecraft.client.model.ModelDragon {
    private ModelRenderer head;
    private ModelRenderer spine;
    private ModelRenderer jaw;
    private ModelRenderer body;
    private ModelRenderer rearLeg;
    private ModelRenderer frontLeg;
    private ModelRenderer rearLegTip;
    private ModelRenderer frontLegTip;
    private ModelRenderer rearFoot;
    private ModelRenderer frontFoot;
    private ModelRenderer wing;
    private ModelRenderer wingTip;
    private float partialTicks;

    public ServerDragonModel() {
        super(0);
        this.textureWidth = 256;
        this.textureHeight = 256;
        this.setTextureOffset("body.body", 0, 0);
        this.setTextureOffset("wing.skin", -56, 88);
        this.setTextureOffset("wingtip.skin", -56, 144);
        this.setTextureOffset("rearleg.main", 0, 0);
        this.setTextureOffset("rearfoot.main", 112, 0);
        this.setTextureOffset("rearlegtip.main", 196, 0);
        this.setTextureOffset("head.upperhead", 112, 30);
        this.setTextureOffset("wing.bone", 112, 88);
        this.setTextureOffset("head.upperlip", 176, 44);
        this.setTextureOffset("jaw.jaw", 176, 65);
        this.setTextureOffset("frontleg.main", 112, 104);
        this.setTextureOffset("wingtip.bone", 112, 136);
        this.setTextureOffset("frontfoot.main", 144, 104);
        this.setTextureOffset("neck.box", 192, 104);
        this.setTextureOffset("frontlegtip.main", 226, 138);
        this.setTextureOffset("body.scale", 220, 53);
        this.setTextureOffset("head.scale", 0, 0);
        this.setTextureOffset("neck.scale", 48, 0);
        this.setTextureOffset("head.nostril", 112, 0);
        float f = -16.0f;
        this.head = new ModelRenderer(this, "head");
        this.head.addBox("upperlip", -6.0f, -1.0f, -8.0f + f, 12, 5, 16);
        this.head.addBox("upperhead", -8.0f, -8.0f, 6.0f + f, 16, 16, 16);
        this.head.mirror = true;
        this.head.addBox("scale", -5.0f, -12.0f, 12.0f + f, 2, 4, 6);
        this.head.addBox("nostril", -5.0f, -3.0f, -6.0f + f, 2, 2, 4);
        this.head.mirror = false;
        this.head.addBox("scale", 3.0f, -12.0f, 12.0f + f, 2, 4, 6);
        this.head.addBox("nostril", 3.0f, -3.0f, -6.0f + f, 2, 2, 4);
        this.jaw = new ModelRenderer(this, "jaw");
        this.jaw.setRotationPoint(0.0f, 4.0f, 8.0f + f);
        this.jaw.addBox("jaw", -6.0f, 0.0f, -16.0f, 12, 4, 16);
        this.head.addChild(this.jaw);
        this.spine = new ModelRenderer(this, "neck");
        this.spine.addBox("box", -5.0f, -5.0f, -5.0f, 10, 10, 10);
        this.spine.addBox("scale", -1.0f, -9.0f, -3.0f, 2, 4, 6);
        this.body = new ModelRenderer(this, "body");
        this.body.setRotationPoint(0.0f, 4.0f, 8.0f);
        this.body.addBox("body", -12.0f, 0.0f, -16.0f, 24, 24, 64);
        this.body.addBox("scale", -1.0f, -6.0f, -10.0f, 2, 6, 12);
        this.body.addBox("scale", -1.0f, -6.0f, 10.0f, 2, 6, 12);
        this.body.addBox("scale", -1.0f, -6.0f, 30.0f, 2, 6, 12);
        this.wing = new ModelRenderer(this, "wing");
        this.wing.setRotationPoint(-12.0f, 5.0f, 2.0f);
        this.wing.addBox("bone", -56.0f, -4.0f, -4.0f, 56, 8, 8);
        this.wing.addBox("skin", -56.0f, 0.0f, 2.0f, 56, 0, 56);
        this.wingTip = new ModelRenderer(this, "wingtip");
        this.wingTip.setRotationPoint(-56.0f, 0.0f, 0.0f);
        this.wingTip.addBox("bone", -56.0f, -2.0f, -2.0f, 56, 4, 4);
        this.wingTip.addBox("skin", -56.0f, 0.0f, 2.0f, 56, 0, 56);
        this.wing.addChild(this.wingTip);
        this.frontLeg = new ModelRenderer(this, "frontleg");
        this.frontLeg.setRotationPoint(-12.0f, 20.0f, 2.0f);
        this.frontLeg.addBox("main", -4.0f, -4.0f, -4.0f, 8, 24, 8);
        this.frontLegTip = new ModelRenderer(this, "frontlegtip");
        this.frontLegTip.setRotationPoint(0.0f, 20.0f, -1.0f);
        this.frontLegTip.addBox("main", -3.0f, -1.0f, -3.0f, 6, 24, 6);
        this.frontLeg.addChild(this.frontLegTip);
        this.frontFoot = new ModelRenderer(this, "frontfoot");
        this.frontFoot.setRotationPoint(0.0f, 23.0f, 0.0f);
        this.frontFoot.addBox("main", -4.0f, 0.0f, -12.0f, 8, 4, 16);
        this.frontLegTip.addChild(this.frontFoot);
        this.rearLeg = new ModelRenderer(this, "rearleg");
        this.rearLeg.setRotationPoint(-16.0f, 16.0f, 42.0f);
        this.rearLeg.addBox("main", -8.0f, -4.0f, -8.0f, 16, 32, 16);
        this.rearLegTip = new ModelRenderer(this, "rearlegtip");
        this.rearLegTip.setRotationPoint(0.0f, 32.0f, -4.0f);
        this.rearLegTip.addBox("main", -6.0f, -2.0f, 0.0f, 12, 32, 12);
        this.rearLeg.addChild(this.rearLegTip);
        this.rearFoot = new ModelRenderer(this, "rearfoot");
        this.rearFoot.setRotationPoint(0.0f, 31.0f, 4.0f);
        this.rearFoot.addBox("main", -9.0f, 0.0f, -20.0f, 18, 6, 24);
        this.rearLegTip.addChild(this.rearFoot);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float f, float g, float partialTickTime) {
        this.partialTicks = partialTickTime;
    }

    @Override
    public void render(Entity entityIn, float f, float g, float h, float i, float j, float scale) {
        float v;
        GlStateManager.pushMatrix();
        EntityDragon entityDragon = (EntityDragon)entityIn;
        float l = entityDragon.prevAnimTime + (entityDragon.animTime - entityDragon.prevAnimTime) * this.partialTicks;
        this.jaw.rotateAngleX = (float)(Math.sin(l * (float)Math.PI * 2.0f) + 1.0) * 0.2f;
        float m = (float)(Math.sin(l * (float)Math.PI * 2.0f - 1.0f) + 1.0);
        m = (m * m * 1.0f + m * 2.0f) * 0.05f;
        GlStateManager.translate(0.0f, m - 2.0f, -3.0f);
        GlStateManager.rotate(m * 2.0f, 1.0f, 0.0f, 0.0f);
        float n = -30.0f;
        float o = 0.0f;
        float p = 1.5f;
        double[] ds = entityDragon.getMovementOffsets(6, this.partialTicks);
        float q = this.updateRotations(entityDragon.getMovementOffsets(5, this.partialTicks)[0] - entityDragon.getMovementOffsets(10, this.partialTicks)[0]);
        float r = this.updateRotations(entityDragon.getMovementOffsets(5, this.partialTicks)[0] + (double)(q / 2.0f));
        n += 2.0f;
        float s = l * (float)Math.PI * 2.0f;
        n = 20.0f;
        float t = -12.0f;
        for (int u = 0; u < 5; ++u) {
            double[] es = entityDragon.getMovementOffsets(5 - u, this.partialTicks);
            v = (float)Math.cos((float)u * 0.45f + s) * 0.15f;
            this.spine.rotateAngleY = this.updateRotations(es[0] - ds[0]) * (float)Math.PI / 180.0f * p;
            this.spine.rotateAngleX = v + com.viaversion.viaforge.mobs.DragonVisualState.headOffset(entityDragon, u, ds, es) * (float)Math.PI / 180.0f * p * 5.0f;
            this.spine.rotateAngleZ = -this.updateRotations(es[0] - (double)r) * (float)Math.PI / 180.0f * p;
            this.spine.rotationPointY = n;
            this.spine.rotationPointZ = t;
            this.spine.rotationPointX = o;
            n = (float)((double)n + Math.sin(this.spine.rotateAngleX) * 10.0);
            t = (float)((double)t - Math.cos(this.spine.rotateAngleY) * Math.cos(this.spine.rotateAngleX) * 10.0);
            o = (float)((double)o - Math.sin(this.spine.rotateAngleY) * Math.cos(this.spine.rotateAngleX) * 10.0);
            this.spine.render(scale);
        }
        this.head.rotationPointY = n;
        this.head.rotationPointZ = t;
        this.head.rotationPointX = o;
        double[] fs = entityDragon.getMovementOffsets(0, this.partialTicks);
        this.head.rotateAngleY = this.updateRotations(fs[0] - ds[0]) * (float)Math.PI / 180.0f * 1.0f;
        this.head.rotateAngleZ = -this.updateRotations(fs[0] - (double)r) * (float)Math.PI / 180.0f * 1.0f;
        this.head.rotateAngleX = com.viaversion.viaforge.mobs.DragonVisualState.headOffset(entityDragon, 6, ds, fs) * (float)Math.PI / 180.0F * 1.5F * 5;
        this.head.render(scale);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-q * p * 1.0f, 0.0f, 0.0f, 1.0f);
        GlStateManager.translate(0.0f, -1.0f, 0.0f);
        this.body.rotateAngleZ = 0.0f;
        this.body.render(scale);
        for (int w = 0; w < 2; ++w) {
            GlStateManager.enableCull();
            v = l * (float)Math.PI * 2.0f;
            this.wing.rotateAngleX = 0.125f - (float)Math.cos(v) * 0.2f;
            this.wing.rotateAngleY = 0.25f;
            this.wing.rotateAngleZ = (float)(Math.sin(v) + 0.125) * 0.8f;
            this.wingTip.rotateAngleZ = -((float)(Math.sin(v + 2.0f) + 0.5)) * 0.75f;
            this.rearLeg.rotateAngleX = 1.0f + m * 0.1f;
            this.rearLegTip.rotateAngleX = 0.5f + m * 0.1f;
            this.rearFoot.rotateAngleX = 0.75f + m * 0.1f;
            this.frontLeg.rotateAngleX = 1.3f + m * 0.1f;
            this.frontLegTip.rotateAngleX = -0.5f - m * 0.1f;
            this.frontFoot.rotateAngleX = 0.75f + m * 0.1f;
            this.wing.render(scale);
            this.frontLeg.render(scale);
            this.rearLeg.render(scale);
            GlStateManager.scale(-1.0f, 1.0f, 1.0f);
            if (w != 0) continue;
            GlStateManager.cullFace(1028);
        }
        GlStateManager.popMatrix();
        GlStateManager.cullFace(1029);
        GlStateManager.disableCull();
        float x = -((float)Math.sin(l * (float)Math.PI * 2.0f)) * 0.0f;
        s = l * (float)Math.PI * 2.0f;
        n = 10.0f;
        t = 60.0f;
        o = 0.0f;
        ds = entityDragon.getMovementOffsets(11, this.partialTicks);
        for (int y = 0; y < 12; ++y) {
            fs = entityDragon.getMovementOffsets(12 + y, this.partialTicks);
            x = (float)((double)x + Math.sin((float)y * 0.45f + s) * (double)0.05f);
            this.spine.rotateAngleY = (this.updateRotations(fs[0] - ds[0]) * p + 180.0f) * (float)Math.PI / 180.0f;
            this.spine.rotateAngleX = x + (float)(fs[1] - ds[1]) * (float)Math.PI / 180.0f * p * 5.0f;
            this.spine.rotateAngleZ = this.updateRotations(fs[0] - (double)r) * (float)Math.PI / 180.0f * p;
            this.spine.rotationPointY = n;
            this.spine.rotationPointZ = t;
            this.spine.rotationPointX = o;
            n = (float)((double)n + Math.sin(this.spine.rotateAngleX) * 10.0);
            t = (float)((double)t - Math.cos(this.spine.rotateAngleY) * Math.cos(this.spine.rotateAngleX) * 10.0);
            o = (float)((double)o - Math.sin(this.spine.rotateAngleY) * Math.cos(this.spine.rotateAngleX) * 10.0);
            this.spine.render(scale);
        }
        GlStateManager.popMatrix();
    }

    private float updateRotations(double d) {
        while (d >= 180.0) {
            d -= 360.0;
        }
        while (d < -180.0) {
            d += 360.0;
        }
        return (float)d;
    }
}
