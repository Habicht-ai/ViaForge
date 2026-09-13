package com.viaversion.viaforge.items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/** Vanilla sweep attack: stationary four-tick, three-frame, camera-facing sheet. */
public final class ServerSweepParticle extends EntityFX {
    private static final ResourceLocation TEXTURE = new ResourceLocation("viaforge", "textures/entity/sweep.png");
    private int age;
    private final float size;
    public ServerSweepParticle(World world, double x, double y, double z, double speed) {
        super(world, x, y, z, 0, 0, 0);
        particleRed = particleGreen = particleBlue = rand.nextFloat() * .6F + .4F;
        size = 1 - (float)speed * .5F;
    }
    @Override public int getFXLayer() { return 3; }
    @Override public void onUpdate() { prevPosX = posX; prevPosY = posY; prevPosZ = posZ; if (++age == 4) setDead(); }
    @Override public int getBrightnessForRender(float partialTicks) { return 61680; }
    @Override public void renderParticle(WorldRenderer buffer, Entity camera, float partialTicks, float rx, float rz, float ryz, float rxy, float rxz) {
        int frame = (int)((age + partialTicks) * 3 / 4);
        if (frame > 7) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        float u = (frame % 4) / 4F, u1 = u + .24975F, v = (frame / 2) / 2F, v1 = v + .4995F;
        float x = (float)(prevPosX + (posX - prevPosX) * partialTicks - interpPosX);
        float y = (float)(prevPosY + (posY - prevPosY) * partialTicks - interpPosY);
        float z = (float)(prevPosZ + (posZ - prevPosZ) * partialTicks - interpPosZ);
        GlStateManager.color(1, 1, 1, 1); GlStateManager.disableLighting(); RenderHelper.disableStandardItemLighting();
        buffer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
        vertex(buffer, x - rx * size - rxy * size, y - rz * size * .5F, z - ryz * size - rxz * size, u1, v1);
        vertex(buffer, x - rx * size + rxy * size, y + rz * size * .5F, z - ryz * size + rxz * size, u1, v);
        vertex(buffer, x + rx * size + rxy * size, y + rz * size * .5F, z + ryz * size + rxz * size, u, v);
        vertex(buffer, x + rx * size - rxy * size, y - rz * size * .5F, z + ryz * size - rxz * size, u, v1);
        Tessellator.getInstance().draw(); GlStateManager.enableLighting();
    }
    private void vertex(WorldRenderer buffer, float x, float y, float z, float u, float v) {
        buffer.pos(x, y, z).tex(u, v).color(particleRed, particleGreen, particleBlue, 1).lightmap(0, 240).endVertex();
    }
}
