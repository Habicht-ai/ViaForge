package com.viaversion.viaforge.items;

import com.viaversion.viaforge.blocks.ServerBlockSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.*;

public final class ServerCrystalVisuals {
    private static final ModelBase NO_BASE = new ModelEnderCrystal(0, false);
    public static ModelBase model(ModelBase original, Entity entity) {
        ServerEntityViews.View view = ServerEntityViews.get(entity.getEntityId());
        return ServerBlockSession.supportsProtocol(107) && view != null && view.type == 51 && !view.crystalBase ? NO_BASE : original;
    }
    public static boolean hasBeam(Entity entity) {
        ServerEntityViews.View view = ServerEntityViews.get(entity.getEntityId());
        return ServerBlockSession.supportsProtocol(107) && view != null && view.type == 51 && view.crystalBeam != null;
    }
    public static void beam(EntityEnderCrystal entity, double x, double y, double z, float partial) {
        if (!hasBeam(entity)) return;
        BlockPos pos = ServerEntityViews.get(entity.getEntityId()).crystalBeam;
        float tx = pos.getX() + .5F, ty = pos.getY() + .5F, tz = pos.getZ() + .5F;
        float bob = MathHelper.sin((entity.innerRotation + partial) * .2F) / 2F + .5F; bob = bob * bob + bob;
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("viaforge:textures/entity/endercrystal/endercrystal_beam.png"));
        if (ServerBlockSession.supportsProtocol(210)) {
            renderBeam(x + tx - entity.posX, y - .3 + bob * .4F + ty - entity.posY, z + tz - entity.posZ,
                    partial, tx, ty, tz, entity.innerRotation, entity.posX, entity.posY, entity.posZ);
        } else {
            renderBeam(x, y - (double)1.3F + bob * .4F, z, partial, entity.posX, entity.posY, entity.posZ, entity.innerRotation, tx, ty, tz);
        }
    }
    private static void renderBeam(double x, double y, double z, float partial, double startX, double startY, double startZ, int ticks, double endX, double endY, double endZ) {
        float dx = (float)(endX - startX), dy = (float)(endY - 1 - startY), dz = (float)(endZ - startZ);
        float horizontal = MathHelper.sqrt_float(dx * dx + dz * dz), length = MathHelper.sqrt_float(dx * dx + dy * dy + dz * dz);
        GlStateManager.pushMatrix(); GlStateManager.translate((float)x, (float)y + 2F, (float)z);
        GlStateManager.rotate((float)-Math.atan2(dz, dx) * 57.295776F - 90F, 0, 1, 0);
        GlStateManager.rotate((float)-Math.atan2(horizontal, dy) * 57.295776F - 90F, 1, 0, 0);
        RenderHelper.disableStandardItemLighting(); GlStateManager.disableCull(); GlStateManager.shadeModel(7425);
        float v0 = -(ticks + partial) * .01F, v1 = length / 32F - (ticks + partial) * .01F;
        WorldRenderer b = Tessellator.getInstance().getWorldRenderer(); b.begin(5, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i <= 8; i++) {
            float a = (i % 8) * (float)Math.PI * 2F / 8F, bx = MathHelper.sin(a) * .75F, by = MathHelper.cos(a) * .75F, u = (i % 8) / 8F;
            b.pos(bx * .2F, by * .2F, 0).tex(u, v0).color(0,0,0,255).endVertex();
            b.pos(bx, by, length).tex(u, v1).color(255,255,255,255).endVertex();
        }
        Tessellator.getInstance().draw(); GlStateManager.enableCull(); GlStateManager.shadeModel(7424); RenderHelper.enableStandardItemLighting(); GlStateManager.popMatrix();
    }
    private ServerCrystalVisuals() { }
}
