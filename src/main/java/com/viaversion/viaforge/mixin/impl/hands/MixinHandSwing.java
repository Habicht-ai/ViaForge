package com.viaversion.viaforge.mixin.impl.hands;
import com.viaversion.viaforge.hands.Offhand;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.*;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ModelBiped.class)
public abstract class MixinHandSwing {
    @Unique private float viaForge$leftSwing;
    @Inject(method="setRotationAngles",at=@At("HEAD"))
    private void hand(float a,float b,float c,float d,float e,float f,Entity entity,CallbackInfo ci){
        ModelBiped model=(ModelBiped)(Object)this;viaForge$leftSwing=0;
        if(Offhand.active()&&entity instanceof EntityLivingBase&&((Offhand.swingHand((EntityLivingBase)entity)==1)^Offhand.mainLeft((EntityLivingBase)entity))){viaForge$leftSwing=model.swingProgress;model.swingProgress=0;}
    }
    @Inject(method="setRotationAngles",at=@At("TAIL"))
    private void animate(float a,float b,float c,float d,float e,float f,Entity entity,CallbackInfo ci){
        if(viaForge$leftSwing<=0)return;ModelBiped m=(ModelBiped)(Object)this;float progress=viaForge$leftSwing;m.swingProgress=progress;
        m.bipedBody.rotateAngleY=-MathHelper.sin(MathHelper.sqrt_float(progress)*(float)Math.PI*2)*.2F;
        m.bipedRightArm.rotationPointZ=MathHelper.sin(m.bipedBody.rotateAngleY)*5;m.bipedRightArm.rotationPointX=-MathHelper.cos(m.bipedBody.rotateAngleY)*5;
        m.bipedLeftArm.rotationPointZ=-MathHelper.sin(m.bipedBody.rotateAngleY)*5;m.bipedLeftArm.rotationPointX=MathHelper.cos(m.bipedBody.rotateAngleY)*5;
        m.bipedRightArm.rotateAngleY+=m.bipedBody.rotateAngleY;m.bipedLeftArm.rotateAngleY+=m.bipedBody.rotateAngleY;
        m.bipedRightArm.rotateAngleX+=m.bipedBody.rotateAngleY;
        float eased=1-progress;eased=1-eased*eased*eased*eased;
        m.bipedLeftArm.rotateAngleX-=MathHelper.sin(eased*(float)Math.PI)*1.2F+MathHelper.sin(progress*(float)Math.PI)*-(m.bipedHead.rotateAngleX-.7F)*.75F;
        m.bipedLeftArm.rotateAngleY+=m.bipedBody.rotateAngleY*2;m.bipedLeftArm.rotateAngleZ=MathHelper.sin(progress*(float)Math.PI)*.4F;
    }
}
