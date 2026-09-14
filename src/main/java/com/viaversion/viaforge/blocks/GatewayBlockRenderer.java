package com.viaversion.viaforge.blocks;
import com.viaversion.viaforge.compatibility.ServerSession;

import java.nio.FloatBuffer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.*;

/** Original projected, additive portal layers, with the gateway's six full-height faces. */
public final class GatewayBlockRenderer extends TileEntitySpecialRenderer<GatewayBlockEntity> {
    private static final ResourceLocation SKY = new ResourceLocation("viaforge:textures/environment/end_sky.png");
    private static final ResourceLocation PORTAL = new ResourceLocation("viaforge:textures/entity/end_portal.png");
    private static final ResourceLocation BEAM = new ResourceLocation("viaforge:textures/entity/end_gateway_beam.png");
    // EnumFacing order: down, up, north, south, west, east.
    private static final int[][][] CORNERS = {
        {{0,0,0},{1,0,0},{1,0,1},{0,0,1}}, {{0,1,1},{1,1,1},{1,1,0},{0,1,0}},
        {{0,1,0},{1,1,0},{1,0,0},{0,0,0}}, {{0,0,1},{1,0,1},{1,1,1},{0,1,1}},
        {{0,0,0},{0,0,1},{0,1,1},{0,1,0}}, {{1,1,0},{1,1,1},{1,0,1},{1,0,0}}
    };
    private final FloatBuffer model = GLAllocation.createDirectFloatBuffer(16), projection = GLAllocation.createDirectFloatBuffer(16), plane = GLAllocation.createDirectFloatBuffer(16);
    private final Random random = new Random(31100);
    @Override public void renderTileEntityAt(GatewayBlockEntity tile, double x, double y, double z, float partial, int stage) {
        GlStateManager.disableFog();
        if (tile.spawning() || tile.cooling()) {
            GlStateManager.alphaFunc(516, .1F); bindTexture(BEAM);
            float progress = MathHelper.sin(tile.progress(partial) * (float)Math.PI);
            int height = MathHelper.floor_double(progress * (tile.spawning() ? 256D - y : ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.EXTENDED_GATEWAY_BEAM) ? 50D : 25D));
            float[] color = beamColor(tile.spawning());
            GatewayBeamRenderer.render(x, y, z, partial, progress, tile.getWorld().getTotalWorldTime(), height, color);
            GatewayBeamRenderer.render(x, y, z, partial, progress, tile.getWorld().getTotalWorldTime(), -height, color);
        }
        GlStateManager.disableLighting(); random.setSeed(31100);
        model.clear(); projection.clear(); GlStateManager.getFloat(2982, model); GlStateManager.getFloat(2983, projection);
        int layers = layers(x * x + y * y + z * z);
        int visibleFaces = 0;
        for (EnumFacing face : EnumFacing.values()) if (tile.visible(face)) visibleFaces |= 1 << face.ordinal();
        for (int i = 0; i < layers; i++) {
            GlStateManager.pushMatrix();
            float brightness = i == 0 ? .15F : 2F / (18 - i);
            bindTexture(i == 0 ? SKY : PORTAL);
            if (i < 2) { GlStateManager.enableBlend(); GlStateManager.blendFunc(i == 0 ? 770 : 1, i == 0 ? 771 : 1); }
            texGen(GlStateManager.TexGen.S, 1, 0, 0); texGen(GlStateManager.TexGen.T, 0, 1, 0); texGen(GlStateManager.TexGen.R, 0, 0, 1);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5890); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
            GlStateManager.translate(.5F, .5F, 0); GlStateManager.scale(.5F, .5F, 1);
            float layer = i + 1;
            GlStateManager.translate(17F / layer, (2F + layer / 1.5F) * ((float)Minecraft.getSystemTime() % 800000F / 800000F), 0);
            GlStateManager.rotate((layer * layer * 4321F + layer * 9F) * 2F, 0, 0, 1);
            GlStateManager.scale(4.5F - layer / 4F, 4.5F - layer / 4F, 1);
            projection.rewind(); model.rewind(); GlStateManager.multMatrix(projection); GlStateManager.multMatrix(model);
            float red = (random.nextFloat() * .5F + .1F) * brightness;
            float green = (random.nextFloat() * .5F + .4F) * brightness;
            float blue = (random.nextFloat() * .5F + .5F) * brightness;
            if (i == 0 && !ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.EXTENDED_GATEWAY_BEAM)) red = green = blue = brightness;
            WorldRenderer buffer = Tessellator.getInstance().getWorldRenderer(); buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            faces(buffer, visibleFaces, x, y, z, red, green, blue);
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix(); GlStateManager.matrixMode(5888); bindTexture(SKY);
        }
        GlStateManager.disableBlend();
        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.S); GlStateManager.disableTexGenCoord(GlStateManager.TexGen.T); GlStateManager.disableTexGenCoord(GlStateManager.TexGen.R);
        GlStateManager.enableLighting(); GlStateManager.enableFog();
    }
    private void texGen(GlStateManager.TexGen coordinate, float x, float y, float z) {
        plane.clear(); plane.put(x).put(y).put(z).put(0); plane.flip();
        GlStateManager.texGen(coordinate, 9216); GlStateManager.texGen(coordinate, 9474, plane); GlStateManager.enableTexGenCoord(coordinate);
    }
    public static int layers(double squaredDistance) {
        return squaredDistance > 36864 ? 2 : squaredDistance > 25600 ? 4 : squaredDistance > 16384 ? 6 : squaredDistance > 9216 ? 8 : squaredDistance > 4096 ? 10 : squaredDistance > 1024 ? 12 : squaredDistance > 576 ? 14 : squaredDistance > 256 ? 15 : 16;
    }
    public static float[] beamColor(boolean spawning) {
        if (ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.UPDATED_DYE_COLORS)) {
            int rgb = spawning ? 0xC74EBD : 0x8932B8;
            return new float[]{(rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F};
        }
        return spawning ? new float[]{.7F, .3F, .85F} : ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.EXTENDED_GATEWAY_BEAM) ? new float[]{.5F, .25F, .7F} : new float[]{.9F, .9F, .2F};
    }
    private static void faces(WorldRenderer b, int visible, double x, double y, double z, float r, float g, float blue) {
        for (int face = 0; face < CORNERS.length; face++) {
            if ((visible & 1 << face) == 0) continue;
            for (int[] c : CORNERS[face]) b.pos(x + c[0], y + c[1], z + c[2]).color(r, g, blue, 1).endVertex();
        }
    }
    @Override public boolean forceTileEntityRender() { return true; }
}
