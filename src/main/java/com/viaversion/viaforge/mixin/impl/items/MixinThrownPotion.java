package com.viaversion.viaforge.mixin.impl.items;

import com.viaversion.viaforge.items.ServerEntityViews;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSnowball.class)
public abstract class MixinThrownPotion {
    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true)
    private void potion(Entity entity, double x, double y, double z, float yaw, float partial, CallbackInfo ci) {
        if (!(entity instanceof EntityPotion)) return;
        ServerEntityViews.View view = ServerEntityViews.get(entity.getEntityId());
        if (view == null || view.potion == null) return;
        Minecraft mc = Minecraft.getMinecraft(); RenderManager manager = mc.getRenderManager();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z); GlStateManager.enableRescaleNormal(); GlStateManager.enableAlpha(); GlStateManager.scale(.5F, .5F, .5F);
            GlStateManager.rotate(180 - manager.playerViewY, 0, 1, 0);
            GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * -manager.playerViewX, 1, 0, 0);
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            mc.getRenderItem().renderItem(view.potion, ItemCameraTransforms.TransformType.GROUND);
        } finally { GlStateManager.disableRescaleNormal(); GlStateManager.popMatrix(); }
        ci.cancel();
    }
}
