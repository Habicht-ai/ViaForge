package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.items.ClientItems;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Shadow private ItemStack itemToRender;

    @Inject(method = "doBlockTransformations", at = @At("HEAD"), cancellable = true)
    private void shieldBlocking(CallbackInfo ci) {
        if (ClientItems.is(itemToRender, com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind.SHIELD)) ci.cancel();
        else if(itemToRender!=null && itemToRender.getItem() instanceof net.minecraft.item.ItemSword
                && com.viaversion.viaforge.compatibility.ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.COMPONENT_BLOCKING)) {
            com.viaversion.viaforge.hands.HandRenderer.blockTransform(1);ci.cancel();
        }
    }

    @Inject(method = "transformFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void versionedHandTransform(float equipProgress, float swingProgress, CallbackInfo ci) {
        if (!isServerBlockItem(itemToRender)) return;
        // 1.9+ puts the block's .4 scale in its model. 1.8 applies another .4 here.
        // Keep the newer right-hand placement/swing, including its final -45 rotation.
        GlStateManager.translate(.56F, -.52F - equipProgress * .6F, -.72F);
        float swingSquared = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float swingRoot = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(45F - swingSquared * 20F, 0, 1, 0);
        GlStateManager.rotate(-swingRoot * 20F, 0, 0, 1);
        GlStateManager.rotate(-swingRoot * 80F, 1, 0, 0);
        GlStateManager.rotate(-45F, 0, 1, 0);
        ci.cancel();
    }

    private static boolean isServerBlockItem(ItemStack stack) {
        return com.viaversion.viaforge.items.ServerCombatModels.imported(stack);
    }

    @Redirect(method = "renderItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"))
    private void versionedHandScale(float x, float y, float z, EntityLivingBase entity,
            ItemStack stack, ItemCameraTransforms.TransformType transform) {
        // Compensate for renderItem's half-scale after the display transform instead,
        // so first-person model translations are not doubled along with geometry.
        if (!isServerBlockItem(stack)) GlStateManager.scale(x, y, z);
    }
}
