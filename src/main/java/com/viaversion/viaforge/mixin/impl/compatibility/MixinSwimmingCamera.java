package com.viaversion.viaforge.mixin.impl.compatibility;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.14's Camera.tick eye-height smoothing; physical eyes/raycast remain independent. */
@Mixin(EntityRenderer.class)
public abstract class MixinSwimmingCamera {
    @Unique private Entity viaForge$camera;
    @Unique private float viaForge$eye,viaForge$oldEye,viaForge$partial;
    @Inject(method="updateRenderer",at=@At("RETURN"))
    private void cameraTick(CallbackInfo ci) {
        Entity camera=Minecraft.getMinecraft().getRenderViewEntity();
        if(camera==null||!ServerSession.rule(ClientRule.CRAWLING_POSE)){viaForge$camera=null;return;}
        if(camera!=viaForge$camera){viaForge$camera=camera;viaForge$eye=viaForge$oldEye=camera.getEyeHeight();}
        viaForge$oldEye=viaForge$eye;viaForge$eye+=(camera.getEyeHeight()-viaForge$eye)*.5F;
    }
    @Inject(method="orientCamera",at=@At("HEAD"))
    private void renderPartial(float partial,CallbackInfo ci){viaForge$partial=partial;}
    @Redirect(method="orientCamera",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;getEyeHeight()F"))
    private float cameraEyes(Entity entity) {
        return entity==viaForge$camera&&ServerSession.rule(ClientRule.CRAWLING_POSE)?viaForge$oldEye+(viaForge$eye-viaForge$oldEye)*viaForge$partial:entity.getEyeHeight();
    }
}
