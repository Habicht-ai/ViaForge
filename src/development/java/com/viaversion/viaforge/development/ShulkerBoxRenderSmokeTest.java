package com.viaversion.viaforge.development;

import com.viaversion.viaforge.blocks.*;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** The open shell must show the same interior regardless of the previous renderer's culling state. */
final class ShulkerBoxRenderSmokeTest {
    private static final int WIDTH = 1440, HEIGHT = 780;

    static void verify(BlockVersionProfile profile, WorldClient world, Path directory) throws Exception {
        if (profile.protocol() < 315) return;
        Minecraft mc = Minecraft.getMinecraft();
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TextureManager previousEngine = dispatcher.renderEngine;
        dispatcher.renderEngine = mc.getTextureManager();
        Framebuffer frame = new Framebuffer(WIDTH, HEIGHT, true);
        frame.setFramebufferColor(.12F, .18F, .12F, 1);
        BlockPos pos = new BlockPos(4, 75, 4);
        IBlockState previousBlock = world.getBlockState(pos);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.pushMatrix();
        try {
            ByteBuffer culled = render(profile, world, pos, frame, true, true);
            ScreenShotHelper.saveScreenshot(directory.toFile(), "shulker-boxes-" + profile.resourceVersion() + ".png", WIDTH, HEIGHT, frame);
            ByteBuffer unculled = render(profile, world, pos, frame, false, true);
            int differentPixels = 0;
            for (int i = 0; i < culled.capacity(); i += 4) if (culled.getInt(i) != unculled.getInt(i)) differentPixels++;
            require(differentPixels == 0, "Shulker box interior depends on incoming face culling: " + differentPixels + " differing pixels, " + profile);
            ByteBuffer supportLast = render(profile, world, pos, frame, true, false);
            differentPixels = 0;
            for (int i = 0; i < culled.capacity(); i += 4) if (culled.getInt(i) != supportLast.getInt(i)) differentPixels++;
            require(differentPixels == 0, "Shulker bottom fights with support surface depending on draw order: " + differentPixels + " pixels, " + profile);
            require(GL11.glIsEnabled(GL11.GL_CULL_FACE), "Shulker renderer restores face culling for following objects");
            require(GL11.glGetError() == 0, "Shulker box render leaves a valid OpenGL state");
        } finally {
            world.setBlockState(pos, previousBlock, 0); dispatcher.renderEngine = previousEngine;
            GlStateManager.enableCull(); GlStateManager.disableRescaleNormal();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            frame.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); GlStateManager.color(1, 1, 1, 1);
        }
    }

    private static ByteBuffer render(BlockVersionProfile profile, WorldClient world, BlockPos pos, Framebuffer frame, boolean cull, boolean supportFirst) {
        Minecraft mc = Minecraft.getMinecraft();
        frame.framebufferClear(); frame.bindFramebuffer(true);
        for (int stage = 0; stage < 3; stage++) for (EnumFacing face : EnumFacing.values()) {
            world.setBlockToAir(pos);
            world.setBlockState(pos, Block.BLOCK_STATE_IDS.getByValue(ClientBlocks.localState(220 << 4 | face.getIndex())), 0);
            ShulkerBlockEntity tile = (ShulkerBlockEntity)world.getTileEntity(pos);
            require(tile != null, "Shulker box render fixture");
            tile.receiveClientEvent(1, 1);
            for (int tick = 0; tick < stage * 5; tick++) tile.update();
            GlStateManager.viewport(face.getIndex() * 240, HEIGHT - 40 - (stage + 1) * 240, 240, 240);
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity(); GLU.gluPerspective(37, 1, .05F, 100);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity(); GLU.gluLookAt(3, 2.1F, 4, .5F, .65F, .5F, 0, 1, 0);
            GlStateManager.enableDepth(); GlStateManager.depthMask(true); GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.enableAlpha(); GlStateManager.alphaFunc(GL11.GL_GREATER, .1F); GlStateManager.disableBlend();
            GlStateManager.enableTexture2D(); GlStateManager.disableFog(); GlStateManager.color(1, 1, 1, 1);
            RenderHelper.enableStandardItemLighting(); OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            if (supportFirst) support(face);
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            TileEntityRendererDispatcher.instance.<ShulkerBlockEntity>getSpecialRenderer(tile).renderTileEntityAt(tile, 0, 0, 0, 1, -1);
            if (!supportFirst) support(face);
        }
        GlStateManager.viewport(0, 0, WIDTH, HEIGHT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity(); GlStateManager.ortho(0, WIDTH, HEIGHT, 0, -1000, 1000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity(); GlStateManager.disableDepth(); GlStateManager.disableLighting();
        mc.fontRendererObj.drawString("Shulker boxes | " + profile.resourceVersion() + " | Closed / half open / fully open", 14, 12, 0xffffff);
        for (int stage = 0; stage < 3; stage++) for (EnumFacing face : EnumFacing.values())
            mc.fontRendererObj.drawString(face.getName(), face.getIndex() * 240 + 12, stage * 240 + 45, 0xffffff);
        ByteBuffer pixels = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
        GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        return pixels;
    }
    private static void support(EnumFacing normal) {
        // The visible face of an adjacent full block, exactly on the shared
        // boundary. Reversing draw order detects a coplanar box bottom.
        int sign = normal.getFrontOffsetX() + normal.getFrontOffsetY() + normal.getFrontOffsetZ();
        int p = sign > 0 ? 0 : 1;
        int[][] corners = normal.getAxis() == EnumFacing.Axis.Y ? new int[][]{{0,p,0},{0,p,1},{1,p,1},{1,p,0}}
                : normal.getAxis() == EnumFacing.Axis.X ? new int[][]{{p,0,0},{p,1,0},{p,1,1},{p,0,1}}
                : new int[][]{{0,0,p},{1,0,p},{1,1,p},{0,1,p}};
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("viaforge", "textures/blocks/end_stone.png"));
        GlStateManager.enableCull();
        WorldRenderer vertices = Tessellator.getInstance().getWorldRenderer();
        vertices.begin(GL11.GL_QUADS, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX_NORMAL);
        for (int i = 0; i < 4; i++) {
            int index = sign > 0 ? i : 3-i;
            vertices.pos(corners[index][0], corners[index][1], corners[index][2]).tex(index == 1 || index == 2 ? 1 : 0, index >= 2 ? 1 : 0)
                    .normal(normal.getFrontOffsetX(), normal.getFrontOffsetY(), normal.getFrontOffsetZ()).endVertex();
        }
        Tessellator.getInstance().draw();
    }
}
