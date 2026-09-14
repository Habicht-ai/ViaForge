package com.viaversion.viaforge.blocks;
import com.viaversion.viaforge.compatibility.ServerSession;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public final class ShulkerBlockRenderer extends TileEntitySpecialRenderer<ShulkerBlockEntity> {
    private final ModelRenderer bottom, lid;
    public ShulkerBlockRenderer() {
        ModelBase model = new ModelBase() { };
        model.textureWidth = model.textureHeight = 64;
        bottom = new ModelRenderer(model, 0, 28); bottom.addBox(-8, -8, -8, 16, 8, 16);
        lid = new ModelRenderer(model, 0, 0); lid.addBox(-8, -16, -8, 16, 12, 16);
    }
    @Override public void renderTileEntityAt(ShulkerBlockEntity tile, double x, double y, double z, float partialTicks, int destroyStage) {
        com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition definition = ClientBlocks.definition(tile.getBlockType());
        if (definition == null) return;
        ResourceLocation texture = new ResourceLocation("viaforge:textures/entity/shulker/shulker_" + ClientBlocks.COLORS[definition.id - 219] + ".png");
        if (ServerSession.getLoadedResourceVersion() == null) texture = new ResourceLocation("minecraft:textures/entity/chest/ender.png");
        bindTexture(texture);
        EnumFacing facing = EnumFacing.getFront(tile.getBlockMetadata() & 7);
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.translate(x + .5, y + .5, z + .5);
        // Vanilla slightly insets the shell around its block center so its
        // bottom does not share the supporting block's exact depth plane.
        GlStateManager.scale(.9995F, .9995F, .9995F);
        switch (facing) {
            case DOWN: GlStateManager.rotate(180, 1, 0, 0); break;
            case NORTH: GlStateManager.rotate(-90, 1, 0, 0); break;
            case SOUTH: GlStateManager.rotate(90, 1, 0, 0); break;
            case WEST: GlStateManager.rotate(90, 0, 0, 1); break;
            case EAST: GlStateManager.rotate(-90, 0, 0, 1); break;
            default: break;
        }
        GlStateManager.translate(0, -.5, 0);
        GlStateManager.scale(1, -1, -1);
        float progress = tile.progress(partialTicks);
        lid.rotationPointY = -8 * progress;
        lid.rotateAngleY = (float) (Math.PI * 1.5) * progress;
        // Vanilla draws both sides of the shell: the alpha cutouts expose its
        // back faces when the lid opens, so culling would leave holes inside.
        GlStateManager.disableCull();
        try {
            bottom.render(1F / 16);
            lid.render(1F / 16);
        } finally {
            GlStateManager.enableCull();
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
        }
    }
}
