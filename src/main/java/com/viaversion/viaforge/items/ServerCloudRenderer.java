package com.viaversion.viaforge.items;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

/** Clouds emit particles during ticks; they have no visible entity mesh. */
public final class ServerCloudRenderer extends Render<ServerAreaEffectCloud> {
    public ServerCloudRenderer(RenderManager manager) { super(manager); }
    @Override public void doRender(ServerAreaEffectCloud entity, double x, double y, double z, float yaw, float partial) { }
    @Override protected ResourceLocation getEntityTexture(ServerAreaEffectCloud entity) { return null; }
}
