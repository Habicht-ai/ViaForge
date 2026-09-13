package com.viaversion.viaforge.items;

import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.ResourceLocation;

public final class ServerArrowRenderer extends RenderArrow {
    private static final ResourceLocation NORMAL = texture("arrow"), TIPPED = texture("tipped_arrow"), SPECTRAL = texture("spectral_arrow");
    public ServerArrowRenderer(RenderManager manager) { super(manager); }
    @Override protected ResourceLocation getEntityTexture(EntityArrow entity) {
        ServerArrow arrow = (ServerArrow)entity;
        return arrow.spectral ? SPECTRAL : arrow.color > 0 ? TIPPED : NORMAL;
    }
    private static ResourceLocation texture(String name) { return new ResourceLocation("viaforge", "textures/entity/projectiles/" + name + ".png"); }
}
