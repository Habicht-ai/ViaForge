package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.items.*;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import org.lwjgl.util.glu.GLU;

/** Captures the actual Minecraft particle/item renderers; no illustrative substitute assets. */
final class ServerEffectsPreview {
    static void capture(BlockVersionProfile profile, Path directory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); EffectRenderer previous = mc.effectRenderer; Entity view = mc.getRenderViewEntity();
        int originalWidth = mc.displayWidth, originalHeight = mc.displayHeight, originalScale = mc.gameSettings.guiScale, originalParticles = mc.gameSettings.particleSetting;
        int width = 1000, height = 640; mc.displayWidth = width; mc.displayHeight = height; mc.gameSettings.guiScale = 2; mc.gameSettings.particleSetting = 0;
        EffectRenderer effects = new EffectRenderer(mc.theWorld, mc.getTextureManager()); mc.effectRenderer = effects;
        EntityOtherPlayerMP camera = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(new UUID(0, 888), "EffectsCamera"));
        camera.setPosition(0, 77, 10); camera.prevPosX = camera.lastTickPosX = 0; camera.prevPosY = camera.lastTickPosY = 77; camera.prevPosZ = camera.lastTickPosZ = 10;
        camera.rotationYaw = camera.prevRotationYaw = 180; camera.rotationPitch = camera.prevRotationPitch = 12; mc.setRenderViewEntity(camera);
        Framebuffer target = new Framebuffer(width, height, true); target.setFramebufferColor(.08F, .09F, .12F, 1);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.matrixMode(5888); GlStateManager.pushMatrix();
        try {
            int[] colors = {0x9612bc, 0xf82423, 0x4e9331};
            for (int i = 0; i < 3; i++) ServerPotionImpact.play(mc.theWorld, profile.protocol(), i == 1 ? 2007 : 2002, new BlockPos((i - 1) * 3, 75, 0), colors[i]);
            for (int i = 0; i < 7; i++) effects.updateEffects();
            worldFrame(target, camera, effects);
            label(mc, "Original potion impact | violet / instant healing / poison | " + profile.resourceVersion());
            ScreenShotHelper.saveScreenshot(directory.toFile(), "potion-impact-" + profile.resourceVersion() + ".png", width, height, target);
            effects.clearEffects(mc.theWorld);
            ServerArrowRenderer renderer = new ServerArrowRenderer(mc.getRenderManager());
            ServerArrow[] arrows = new ServerArrow[3];
            for (int i = 0; i < 3; i++) {
                ServerArrow arrow = new ServerArrow(mc.theWorld, i == 2); arrows[i] = arrow;
                arrow.color = i == 1 ? 0xf82423 : -1; arrow.rotationYaw = arrow.prevRotationYaw = 55;
                arrow.setPosition((i - 1) * 3, 75, 0); arrow.setVelocity(.03, 0, 0);
            }
            for (int t = 0; t < 15; t++) { for (ServerArrow arrow : arrows) arrow.onUpdate(); effects.updateEffects(); }
            camera.setPosition(0, 74, 10); camera.prevPosY = camera.lastTickPosY = 74;
            worldFrame(target, camera, effects);
            for (ServerArrow arrow : arrows) renderer.doRender(arrow, arrow.posX - camera.posX, arrow.posY - camera.posY, arrow.posZ - camera.posZ, 0, .5F);
            label(mc, "Arrow entities | normal / tipped (healing) / spectral | " + profile.resourceVersion());
            ScreenShotHelper.saveScreenshot(directory.toFile(), "arrow-effects-" + profile.resourceVersion() + ".png", width, height, target);
            camera.setPosition(0, 77, 10); camera.prevPosY = camera.lastTickPosY = 77;
            effects.clearEffects(mc.theWorld); ServerTotemAnimation.clear();
            double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
            mc.thePlayer.setPosition(0, 75, 0);
            try {
                ServerTotemAnimation.activate(mc.thePlayer);
                for (int tick = 0; tick < 40; tick++) {
                    if (tick == 4 || tick == 16 || tick == 32) {
                        worldFrame(target, camera, effects); ServerTotemAnimation.render(.5F);
                        label(mc, "Original Totem activation | tick " + tick + " / 40 | " + profile.resourceVersion());
                        ScreenShotHelper.saveScreenshot(directory.toFile(), "totem-" + profile.resourceVersion() + "-tick-" + tick + ".png", width, height, target);
                    }
                    ServerTotemAnimation.tick(); effects.updateEffects();
                }
                ServerEntitySmokeTest.require(ServerEffectsSmokeTest.timer() == 0, "Rendered totem animation completes");
            } finally { mc.thePlayer.setPosition(px, py, pz); }
        } finally {
            mc.effectRenderer = previous; mc.setRenderViewEntity(view); ServerTotemAnimation.clear();
            mc.displayWidth = originalWidth; mc.displayHeight = originalHeight; mc.gameSettings.guiScale = originalScale; mc.gameSettings.particleSetting = originalParticles;
            GlStateManager.matrixMode(5888); GlStateManager.popMatrix(); GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); GlStateManager.color(1, 1, 1, 1); ActiveRenderInfo.updateRenderInfo(mc.thePlayer, false);
        }
    }
    private static void worldFrame(Framebuffer target, EntityOtherPlayerMP camera, EffectRenderer effects) {
        target.framebufferClear(); target.bindFramebuffer(true);
        GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GLU.gluPerspective(55, 1000F / 640, .05F, 100);
        GlStateManager.matrixMode(5888); GlStateManager.loadIdentity(); GLU.gluLookAt(0, 0, 0, 0, -2, -10, 0, 1, 0);
        ActiveRenderInfo.updateRenderInfo(camera, false); GlStateManager.enableDepth(); GlStateManager.enableAlpha(); GlStateManager.disableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240); effects.renderParticles(camera, .5F);
    }
    private static void label(Minecraft mc, String text) {
        GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GlStateManager.ortho(0, 1000, 640, 0, -100, 100);
        GlStateManager.matrixMode(5888); GlStateManager.loadIdentity(); GlStateManager.disableDepth();
        mc.fontRendererObj.drawString(text, 20, 16, 0xffffff);
    }
}
