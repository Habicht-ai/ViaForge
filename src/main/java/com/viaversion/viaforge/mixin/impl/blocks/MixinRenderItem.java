package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.blocks.ClientBlocks;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Imported GUI transforms already contain the rotation and scale hard-coded in 1.8. */
@Mixin(RenderItem.class)
public abstract class MixinRenderItem {
    @Shadow public float zLevel;
    @Shadow private void setupGuiTransform(int x, int y, boolean gui3d) { throw new AssertionError(); }
    @Shadow private void preTransform(ItemStack stack) { throw new AssertionError(); }

    @Redirect(method = "renderItemIntoGUI", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderItem;setupGuiTransform(IIZ)V"))
    private void versionedGuiTransform(RenderItem renderer, int x, int y, boolean gui3d,
            ItemStack stack, int slotX, int slotY) {
        if (ClientBlocks.serverItem(Item.getIdFromItem(stack.getItem())) == null) {
            setupGuiTransform(x, y, gui3d);
            return;
        }
        GlStateManager.translate(x + 8F, y + 8F, 100F + zLevel);
        GlStateManager.scale(16F, -16F, 16F);
        if (gui3d) GlStateManager.enableLighting();
        else GlStateManager.disableLighting();
    }

    @Redirect(method = "renderItemIntoGUI", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V"))
    private void versionedGuiGeometry(RenderItem renderer, ItemStack stack, IBakedModel model) {
        // Undo renderItem's legacy half-scale after the model's display transform,
        // keeping display translations at their original size as well.
        if (ClientBlocks.serverItem(Item.getIdFromItem(stack.getItem())) != null) GlStateManager.scale(2F, 2F, 2F);
        renderer.renderItem(stack, model);
    }

    @Redirect(method = "renderItemModelTransform", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderItem;preTransform(Lnet/minecraft/item/ItemStack;)V"))
    private void versionedHandPreTransform(RenderItem renderer, ItemStack stack,
            ItemStack original, IBakedModel model, ItemCameraTransforms.TransformType transform) {
        if (transform == ItemCameraTransforms.TransformType.FIRST_PERSON && ClientBlocks.serverItem(Item.getIdFromItem(stack.getItem())) != null) {
            GlStateManager.color(1, 1, 1, 1);
        } else preTransform(stack);
    }

    @Redirect(method = "renderItemModelTransform", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V"))
    private void versionedHandGeometry(RenderItem renderer, ItemStack stack, IBakedModel model,
            ItemStack original, IBakedModel originalModel, ItemCameraTransforms.TransformType transform) {
        if (transform == ItemCameraTransforms.TransformType.FIRST_PERSON && ClientBlocks.serverItem(Item.getIdFromItem(stack.getItem())) != null) GlStateManager.scale(2F, 2F, 2F);
        renderer.renderItem(stack, model);
    }
}
