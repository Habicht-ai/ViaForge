package com.viaversion.viaforge.mixin.impl.blocks;

import com.viaversion.viaforge.common.compatibility.ClientFeature;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.items.ServerCombatModels;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.TRSRTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Imported ground transforms already contain the target's full item scale. */
@Mixin(RenderEntityItem.class)
public abstract class MixinDroppedServerItem {
    @Redirect(method = "func_177077_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms;getTransform(Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)Lnet/minecraft/client/renderer/block/model/ItemTransformVec3f;"))
    private ItemTransformVec3f targetGroundHeight(ItemCameraTransforms transforms, ItemCameraTransforms.TransformType type,
            EntityItem entity, double x, double y, double z, float partialTicks, IBakedModel model) {
        ItemTransformVec3f transform = transforms.getTransform(type);
        if (imported(entity.getEntityItem()) && model instanceof IPerspectiveAwareModel) {
            // Forge-generated flat items store their display outside the legacy camera
            // fields. The native bobbing offset must use the same scale as their draw.
            javax.vecmath.Matrix4f matrix = ((IPerspectiveAwareModel) model).handlePerspective(type).getRight();
            if (matrix != null) return new ItemTransformVec3f(transform.rotation, transform.translation,
                    TRSRTransformation.toLwjgl(new TRSRTransformation(matrix).getScale()));
        }
        return transform;
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"))
    private void targetGroundScale(float x, float y, float z, EntityItem entity,
            double renderX, double renderY, double renderZ, float yaw, float partialTicks) {
        if (!imported(entity.getEntityItem())) GlStateManager.scale(x, y, z);
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V"))
    private void targetGroundGeometry(RenderItem renderer, ItemStack stack, IBakedModel model) {
        // Compensate only after the display transform, preserving its translation.
        if (imported(stack)) GlStateManager.scale(2F, 2F, 2F);
        renderer.renderItem(stack, model);
    }

    private static boolean imported(ItemStack stack) {
        return ServerSession.has(ClientFeature.ITEMS) && ServerCombatModels.imported(stack);
    }
}
