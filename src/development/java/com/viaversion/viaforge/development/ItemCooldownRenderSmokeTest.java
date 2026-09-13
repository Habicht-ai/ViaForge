package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.items.ServerItemCooldowns;
import com.viaversion.viaforge.mixin.impl.items.MinecraftItemTimer;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

final class ItemCooldownRenderSmokeTest {
    static void verify(BlockVersionProfile profile, ItemStack... stacks) {
        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer frame = new Framebuffer(96, 96, true); frame.setFramebufferColor(.1F, .1F, .1F, 1);
        net.minecraft.util.Timer timer = ((MinecraftItemTimer)mc).viaForge$itemTimer(); float partial = timer.renderPartialTicks;
        timer.renderPartialTicks = 0;
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.pushMatrix();
        try {
            for (ItemStack original : stacks) {
                ItemStack stack = original.copy(); stack.stackSize = 1;
                ServerItemCooldowns.clear();
                ByteBuffer bare = render(frame, stack);
                ServerItemCooldowns.set(ServerItemCooldowns.itemId(stack), 20);
                for (int tick = 0; tick < 10; tick++) ServerItemCooldowns.tick(mc.thePlayer);
                ByteBuffer half = render(frame, stack);
                int above = 0, below = 0;
                for (int y = 16; y < 80; y++) for (int x = 16; x < 80; x++) {
                    int index = ((95-y) * 96 + x) * 4;
                    if (bare.getInt(index) != half.getInt(index)) { if (y < 48) above++; else below++; }
                }
                require(above == 0 && below > 0, "Native inventory cooldown overlays exactly the remaining lower half for " + stack.getDisplayName());
                if (profile == BlockVersionProfile.V1_12_2) ScreenShotHelper.saveScreenshot(Paths.get(System.getenv("VIAFORGE_BLOCK_SMOKE_TEST")).toAbsolutePath().getParent().toFile(), "item-cooldown-" + ServerItemCooldowns.itemId(stack) + ".png", 96, 96, frame);
                for (int tick = 0; tick < 10; tick++) ServerItemCooldowns.tick(mc.thePlayer);
                ByteBuffer expired = render(frame, stack);
                require(bare.equals(expired), "Expired cooldown restores exact original item overlay pixels");
                require(GL11.glGetError() == 0, "Cooldown overlay leaves valid GL state");
            }
        } finally {
            ServerItemCooldowns.clear(); timer.renderPartialTicks = partial;
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); RenderHelper.disableStandardItemLighting();
            frame.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true);
        }
    }
    private static ByteBuffer render(Framebuffer frame, ItemStack stack) {
        Minecraft mc = Minecraft.getMinecraft(); frame.framebufferClear(); frame.bindFramebuffer(true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity(); GlStateManager.ortho(0, 24, 24, 0, -1000, 1000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity();
        GlStateManager.enableDepth(); GlStateManager.enableAlpha(); GlStateManager.enableTexture2D(); GlStateManager.color(1, 1, 1, 1);
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 4, 4);
        mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, 4, 4, null);
        ByteBuffer pixels = BufferUtils.createByteBuffer(96 * 96 * 4);
        GL11.glReadPixels(0, 0, 96, 96, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        return pixels;
    }
}
