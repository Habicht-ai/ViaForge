package com.viaversion.viaforge.mobs.models;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/** Model boxes and UV coordinates of the 1.9-1.12 entities. Animation lives in the model classes. */
public abstract class MobGeometry extends ModelBase {
    public ModelRenderer head, body, leg1, leg2, leg3, leg4, bottom, lid, leftChest, rightChest,
            hat, arms, nose, rightArm, leftArm, tail, leftWing, rightWing, headCap, beak, lowerBeak, crest;
    protected void shulkerGeometry() {
        this.textureHeight = 64;
        this.textureWidth = 64;
        this.lid = new ModelRenderer(this);
        this.bottom = new ModelRenderer(this);
        this.head = new ModelRenderer(this);
        this.lid.setTextureOffset(0, 0).addBox(-8.0f, -16.0f, -8.0f, 16, 12, 16);
        this.lid.setRotationPoint(0.0f, 24.0f, 0.0f);
        this.bottom.setTextureOffset(0, 28).addBox(-8.0f, -8.0f, -8.0f, 16, 8, 16);
        this.bottom.setRotationPoint(0.0f, 24.0f, 0.0f);
        this.head.setTextureOffset(0, 52).addBox(-3.0f, 0.0f, -3.0f, 6, 6, 6);
        this.head.setRotationPoint(0.0f, 12.0f, 0.0f);
    }

    protected void polarBearGeometry() {
        this.textureWidth = 128;
        this.textureHeight = 64;
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-3.5f, -3.0f, -3.0f, 7, 7, 7, 0.0f);
        this.head.setRotationPoint(0.0f, 10.0f, -16.0f);
        this.head.setTextureOffset(0, 44).addBox(-2.5f, 1.0f, -6.0f, 5, 3, 3, 0.0f);
        this.head.setTextureOffset(26, 0).addBox(-4.5f, -4.0f, -1.0f, 2, 2, 1, 0.0f);
        ModelRenderer extra = this.head.setTextureOffset(26, 0);
        extra.mirror = true;
        extra.addBox(2.5f, -4.0f, -1.0f, 2, 2, 1, 0.0f);
        this.body = new ModelRenderer(this);
        this.body.setTextureOffset(0, 19).addBox(-5.0f, -13.0f, -7.0f, 14, 14, 11, 0.0f);
        this.body.setTextureOffset(39, 0).addBox(-4.0f, -25.0f, -7.0f, 12, 12, 10, 0.0f);
        this.body.setRotationPoint(-2.0f, 9.0f, 12.0f);
        this.leg1 = new ModelRenderer(this, 50, 22);
        this.leg1.addBox(-2.0f, 0.0f, -2.0f, 4, 10, 8, 0.0f);
        this.leg1.setRotationPoint(-3.5f, 14.0f, 6.0f);
        this.leg2 = new ModelRenderer(this, 50, 22);
        this.leg2.addBox(-2.0f, 0.0f, -2.0f, 4, 10, 8, 0.0f);
        this.leg2.setRotationPoint(3.5f, 14.0f, 6.0f);
        this.leg3 = new ModelRenderer(this, 50, 40);
        this.leg3.addBox(-2.0f, 0.0f, -2.0f, 4, 10, 6, 0.0f);
        this.leg3.setRotationPoint(-2.5f, 14.0f, -7.0f);
        this.leg4 = new ModelRenderer(this, 50, 40);
        this.leg4.addBox(-2.0f, 0.0f, -2.0f, 4, 10, 6, 0.0f);
        this.leg4.setRotationPoint(2.5f, 14.0f, -7.0f);
        this.leg1.rotationPointX -= 1.0f;
        this.leg2.rotationPointX += 1.0f;
        this.leg1.rotationPointZ += 0.0f;
        this.leg2.rotationPointZ += 0.0f;
        this.leg3.rotationPointX -= 1.0f;
        this.leg4.rotationPointX += 1.0f;
        this.leg3.rotationPointZ -= 1.0f;
        this.leg4.rotationPointZ -= 1.0f;
    }

    protected void llamaGeometry(float inflate) {
        this.textureWidth = 128;
        this.textureHeight = 64;
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-2.0f, -14.0f, -10.0f, 4, 4, 9, inflate);
        this.head.setRotationPoint(0.0f, 7.0f, -6.0f);
        this.head.setTextureOffset(0, 14).addBox(-4.0f, -16.0f, -6.0f, 8, 18, 6, inflate);
        this.head.setTextureOffset(17, 0).addBox(-4.0f, -19.0f, -4.0f, 3, 3, 2, inflate);
        this.head.setTextureOffset(17, 0).addBox(1.0f, -19.0f, -4.0f, 3, 3, 2, inflate);
        this.body = new ModelRenderer(this, 29, 0);
        this.body.addBox(-6.0f, -10.0f, -7.0f, 12, 18, 10, inflate);
        this.body.setRotationPoint(0.0f, 5.0f, 2.0f);
        this.leftChest = new ModelRenderer(this, 45, 28);
        this.leftChest.addBox(-3.0f, 0.0f, 0.0f, 8, 8, 3, inflate);
        this.leftChest.setRotationPoint(-8.5f, 3.0f, 3.0f);
        this.leftChest.rotateAngleY = 1.5707964f;
        this.rightChest = new ModelRenderer(this, 45, 41);
        this.rightChest.addBox(-3.0f, 0.0f, 0.0f, 8, 8, 3, inflate);
        this.rightChest.setRotationPoint(5.5f, 3.0f, 3.0f);
        this.rightChest.rotateAngleY = 1.5707964f;
        this.leg1 = new ModelRenderer(this, 29, 29);
        this.leg1.addBox(-2.0f, 0.0f, -2.0f, 4, 14, 4, inflate);
        this.leg1.setRotationPoint(-2.5f, 10.0f, 6.0f);
        this.leg2 = new ModelRenderer(this, 29, 29);
        this.leg2.addBox(-2.0f, 0.0f, -2.0f, 4, 14, 4, inflate);
        this.leg2.setRotationPoint(2.5f, 10.0f, 6.0f);
        this.leg3 = new ModelRenderer(this, 29, 29);
        this.leg3.addBox(-2.0f, 0.0f, -2.0f, 4, 14, 4, inflate);
        this.leg3.setRotationPoint(-2.5f, 10.0f, -4.0f);
        this.leg4 = new ModelRenderer(this, 29, 29);
        this.leg4.addBox(-2.0f, 0.0f, -2.0f, 4, 14, 4, inflate);
        this.leg4.setRotationPoint(2.5f, 10.0f, -4.0f);
        this.leg1.rotationPointX -= 1.0f;
        this.leg2.rotationPointX += 1.0f;
        this.leg1.rotationPointZ += 0.0f;
        this.leg2.rotationPointZ += 0.0f;
        this.leg3.rotationPointX -= 1.0f;
        this.leg4.rotationPointX += 1.0f;
        this.leg3.rotationPointZ -= 1.0f;
        this.leg4.rotationPointZ -= 1.0f;
    }

    protected void illagerGeometry() {
        this.head = new ModelRenderer(this).setTextureSize(64, 64);
        this.head.setRotationPoint(0.0f, 0.0f + 0.0f, 0.0f);
        this.head.setTextureOffset(0, 0).addBox(-4.0f, -10.0f, -4.0f, 8, 10, 8, 0.0f);
        this.hat = new ModelRenderer(this, 32, 0).setTextureSize(64, 64);
        this.hat.addBox(-4.0f, -10.0f, -4.0f, 8, 12, 8, 0.0f + 0.45f);
        this.head.addChild(this.hat);
        this.hat.showModel = false;
        this.nose = new ModelRenderer(this).setTextureSize(64, 64);
        this.nose.setRotationPoint(0.0f, 0.0f - 2.0f, 0.0f);
        this.nose.setTextureOffset(24, 0).addBox(-1.0f, -1.0f, -6.0f, 2, 4, 2, 0.0f);
        this.head.addChild(this.nose);
        this.body = new ModelRenderer(this).setTextureSize(64, 64);
        this.body.setRotationPoint(0.0f, 0.0f + 0.0f, 0.0f);
        this.body.setTextureOffset(16, 20).addBox(-4.0f, 0.0f, -3.0f, 8, 12, 6, 0.0f);
        this.body.setTextureOffset(0, 38).addBox(-4.0f, 0.0f, -3.0f, 8, 18, 6, 0.0f + 0.5f);
        this.arms = new ModelRenderer(this).setTextureSize(64, 64);
        this.arms.setRotationPoint(0.0f, 0.0f + 0.0f + 2.0f, 0.0f);
        this.arms.setTextureOffset(44, 22).addBox(-8.0f, -2.0f, -2.0f, 4, 8, 4, 0.0f);
        ModelRenderer extra = new ModelRenderer(this, 44, 22).setTextureSize(64, 64);
        extra.mirror = true;
        extra.addBox(4.0f, -2.0f, -2.0f, 4, 8, 4, 0.0f);
        this.arms.addChild(extra);
        this.arms.setTextureOffset(40, 38).addBox(-4.0f, 2.0f, -2.0f, 8, 4, 4, 0.0f);
        this.leg1 = new ModelRenderer(this, 0, 22).setTextureSize(64, 64);
        this.leg1.setRotationPoint(-2.0f, 12.0f + 0.0f, 0.0f);
        this.leg1.addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4, 0.0f);
        this.leg2 = new ModelRenderer(this, 0, 22).setTextureSize(64, 64);
        this.leg2.mirror = true;
        this.leg2.setRotationPoint(2.0f, 12.0f + 0.0f, 0.0f);
        this.leg2.addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4, 0.0f);
        this.rightArm = new ModelRenderer(this, 40, 46).setTextureSize(64, 64);
        this.rightArm.addBox(-3.0f, -2.0f, -2.0f, 4, 12, 4, 0.0f);
        this.rightArm.setRotationPoint(-5.0f, 2.0f + 0.0f, 0.0f);
        this.leftArm = new ModelRenderer(this, 40, 46).setTextureSize(64, 64);
        this.leftArm.mirror = true;
        this.leftArm.addBox(-1.0f, -2.0f, -2.0f, 4, 12, 4, 0.0f);
        this.leftArm.setRotationPoint(5.0f, 2.0f + 0.0f, 0.0f);
    }

    protected void parrotGeometry() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.body = new ModelRenderer(this, 2, 8);
        this.body.addBox(-1.5f, 0.0f, -1.5f, 3, 6, 3);
        this.body.setRotationPoint(0.0f, 16.5f, -3.0f);
        this.tail = new ModelRenderer(this, 22, 1);
        this.tail.addBox(-1.5f, -1.0f, -1.0f, 3, 4, 1);
        this.tail.setRotationPoint(0.0f, 21.07f, 1.16f);
        this.leftWing = new ModelRenderer(this, 19, 8);
        this.leftWing.addBox(-0.5f, 0.0f, -1.5f, 1, 5, 3);
        this.leftWing.setRotationPoint(1.5f, 16.94f, -2.76f);
        this.rightWing = new ModelRenderer(this, 19, 8);
        this.rightWing.addBox(-0.5f, 0.0f, -1.5f, 1, 5, 3);
        this.rightWing.setRotationPoint(-1.5f, 16.94f, -2.76f);
        this.head = new ModelRenderer(this, 2, 2);
        this.head.addBox(-1.0f, -1.5f, -1.0f, 2, 3, 2);
        this.head.setRotationPoint(0.0f, 15.69f, -2.76f);
        this.headCap = new ModelRenderer(this, 10, 0);
        this.headCap.addBox(-1.0f, -0.5f, -2.0f, 2, 1, 4);
        this.headCap.setRotationPoint(0.0f, -2.0f, -1.0f);
        this.head.addChild(this.headCap);
        this.beak = new ModelRenderer(this, 11, 7);
        this.beak.addBox(-0.5f, -1.0f, -0.5f, 1, 2, 1);
        this.beak.setRotationPoint(0.0f, -0.5f, -1.5f);
        this.head.addChild(this.beak);
        this.lowerBeak = new ModelRenderer(this, 16, 7);
        this.lowerBeak.addBox(-0.5f, 0.0f, -0.5f, 1, 2, 1);
        this.lowerBeak.setRotationPoint(0.0f, -1.75f, -2.45f);
        this.head.addChild(this.lowerBeak);
        this.crest = new ModelRenderer(this, 2, 18);
        this.crest.addBox(0.0f, -4.0f, -2.0f, 0, 5, 4);
        this.crest.setRotationPoint(0.0f, -2.15f, 0.15f);
        this.head.addChild(this.crest);
        this.leg1 = new ModelRenderer(this, 14, 18);
        this.leg1.addBox(-0.5f, 0.0f, -0.5f, 1, 2, 1);
        this.leg1.setRotationPoint(1.0f, 22.0f, -1.05f);
        this.leg2 = new ModelRenderer(this, 14, 18);
        this.leg2.addBox(-0.5f, 0.0f, -0.5f, 1, 2, 1);
        this.leg2.setRotationPoint(-1.0f, 22.0f, -1.05f);
    }

}
