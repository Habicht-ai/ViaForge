package com.viaversion.viaforge.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.*;

/** Modern arm grip, followed by the target model's display transform. */
public final class ServerHeldItemRenderer {
    private static EntityLivingBase owner;
    private static boolean offhand, left;
    public static boolean imported(ItemStack stack) { return ServerCombatModels.imported(stack); }
    public static boolean left() { return owner != null && left; }
    public static boolean blocking(ItemStack stack) {
        if (owner != null) return ServerEntityViews.blocking(owner, offhand, stack);
        return Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().thePlayer.getItemInUse() == stack;
    }
    public static void render(ModelBiped model, EntityLivingBase entity, ItemStack stack, boolean otherHand) {
        if (stack == null) return;
        ServerEntityViews.View view = ServerEntityViews.get(entity.getEntityId());
        boolean leftSide = otherHand ^ (view != null && view.leftHanded);
        EntityLivingBase oldOwner = owner; boolean oldOffhand = offhand, oldLeft = left;
        owner = entity; offhand = otherHand; left = leftSide;
        GlStateManager.pushMatrix();
        try {
            if (model.isChild) { GlStateManager.translate(0, .75F, 0); GlStateManager.scale(.5F, .5F, .5F); }
            net.minecraft.client.model.ModelRenderer arm = leftSide ? model.bipedLeftArm : model.bipedRightArm;
            float offset = model instanceof net.minecraft.client.model.ModelPlayer
                    && ((com.viaversion.viaforge.mixin.impl.items.ModelPlayerArms)model).viaForge$smallArms() ? (leftSide ? -.5F : .5F) : 0;
            arm.rotationPointX += offset;
            try { arm.postRender(.0625F); } finally { arm.rotationPointX -= offset; }
            if (entity.isSneaking()) GlStateManager.translate(0, .2F, 0);
            GlStateManager.rotate(-90, 1, 0, 0); GlStateManager.rotate(180, 0, 1, 0);
            GlStateManager.translate((leftSide ? -1 : 1) / 16F, .125F, -.625F);
            Minecraft.getMinecraft().getItemRenderer().renderItem(entity, stack, ItemCameraTransforms.TransformType.THIRD_PERSON);
        } finally { GlStateManager.popMatrix(); owner = oldOwner; offhand = oldOffhand; left = oldLeft; }
    }
    private ServerHeldItemRenderer() { }
}
