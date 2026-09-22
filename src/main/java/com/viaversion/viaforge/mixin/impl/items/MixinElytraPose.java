package com.viaversion.viaforge.mixin.impl.items;
import com.viaversion.viaforge.items.ServerElytraFlight;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RendererLivingEntity.class)
public abstract class MixinElytraPose {
    @Inject(method="rotateCorpse", at=@At("RETURN"))
    private void flightPose(EntityLivingBase entity, float age, float yaw, float partial, CallbackInfo ci) {
        if (!ServerElytraFlight.flying(entity)) {
            float crawl=com.viaversion.viaforge.items.ServerElytraVisuals.crawlAmount(entity,partial);
            if(crawl>0) {
                GlStateManager.rotate((-90-(entity.isInWater()?entity.rotationPitch:0))*crawl,1,0,0);
                if(ServerElytraFlight.crawling(entity)||com.viaversion.viaforge.compatibility.ServerSwimming.pose(entity))GlStateManager.translate(0,-1,.3F);
            }
            return;
        }
        float ticks = com.viaversion.viaforge.items.ServerElytraVisuals.ticks(entity) + partial;
        GlStateManager.rotate(Math.min(1, ticks * ticks / 100) * (-90 - entity.rotationPitch), 1, 0, 0);
        if(com.viaversion.viaforge.items.InventoryEntityPreview.captured(entity)) {
            GlStateManager.rotate(com.viaversion.viaforge.items.InventoryEntityPreview.flightYaw(),0,1,0);
            return;
        }
        Vec3 look = entity.getLook(partial);
        double speed = entity.motionX * entity.motionX + entity.motionZ * entity.motionZ;
        double horizontal = look.xCoord * look.xCoord + look.zCoord * look.zCoord;
        if (speed > 0 && horizontal > 0) {
            double cosine = (entity.motionX * look.xCoord + entity.motionZ * look.zCoord) / Math.sqrt(speed * horizontal);
            double cross = entity.motionX * look.zCoord - entity.motionZ * look.xCoord;
            GlStateManager.rotate((float)(Math.signum(cross) * Math.acos(Math.max(-1, Math.min(1, cosine))) * 180 / Math.PI), 0, 1, 0);
        }
    }
}
