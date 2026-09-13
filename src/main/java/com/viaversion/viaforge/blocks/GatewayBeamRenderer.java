package com.viaversion.viaforge.blocks;

import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

/** The native beacon beam geometry with the gateway's .15/.175 radii. */
final class GatewayBeamRenderer {
    static void render(double x, double y, double z, float partial, float progress, long ticks, int height, float[] color) {
        GL11.glTexParameteri(3553, 10242, 10497); GL11.glTexParameteri(3553, 10243, 10497);
        GlStateManager.disableLighting(); GlStateManager.disableCull(); GlStateManager.disableBlend(); GlStateManager.depthMask(true);
        GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
        double time = ticks + (double)partial, direction = height < 0 ? time : -time;
        double scroll = MathHelper.func_181162_h(direction * .2 - MathHelper.floor_double(direction * .1));
        double angle = time * .025 * -1.5;
        double[][] core = new double[4][2];
        for (int i = 0; i < 4; i++) { double a = angle + Math.PI * (.75 - i * .5); core[i][0] = .5 + Math.cos(a) * .15; core[i][1] = .5 + Math.sin(a) * .15; }
        draw(x, y, z, height, core, -1 + scroll, height * (double)progress * (.5 / .15) - 1 + scroll, color, 1);
        GlStateManager.enableBlend(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); GlStateManager.depthMask(false);
        draw(x, y, z, height, new double[][]{{.325,.325},{.675,.325},{.675,.675},{.325,.675}}, -1 + scroll, height * (double)progress - 1 + scroll, color, .125F);
        GlStateManager.enableLighting(); GlStateManager.enableTexture2D(); GlStateManager.depthMask(true);
    }
    private static void draw(double x, double y, double z, int height, double[][] points, double v0, double v1, float[] color, float alpha) {
        WorldRenderer b = Tessellator.getInstance().getWorldRenderer(); b.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 4; i++) {
            double[] a = points[i], c = points[(i + 1) % 4];
            b.pos(x+a[0], y+height, z+a[1]).tex(1,v1).color(color[0],color[1],color[2],alpha).endVertex();
            b.pos(x+a[0], y, z+a[1]).tex(1,v0).color(color[0],color[1],color[2],alpha).endVertex();
            b.pos(x+c[0], y, z+c[1]).tex(0,v0).color(color[0],color[1],color[2],alpha).endVertex();
            b.pos(x+c[0], y+height, z+c[1]).tex(0,v1).color(color[0],color[1],color[2],alpha).endVertex();
        }
        Tessellator.getInstance().draw();
    }
    private GatewayBeamRenderer() { }
}
