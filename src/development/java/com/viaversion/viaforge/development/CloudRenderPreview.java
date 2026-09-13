package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.items.ServerAreaEffectCloud;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.*;
import net.minecraft.world.*;
import org.lwjgl.util.glu.GLU;

/** The real cloud emits the native particles, rendered by Minecraft's EffectRenderer. */
final class CloudRenderPreview {
    static void capture(Path directory) {
        Minecraft mc = Minecraft.getMinecraft();
        EffectRenderer effects = new EffectRenderer(mc.theWorld, mc.getTextureManager());
        WorldClient world = new WorldClient(null, new WorldSettings(0, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT), 0, EnumDifficulty.PEACEFUL, new Profiler()) {
            @Override public void spawnParticle(EnumParticleTypes type, double x, double y, double z, double r, double g, double b, int... args) {
                effects.spawnEffectParticle(type.getParticleID(), x, y, z, r, g, b, args);
            }
        };
        ServerAreaEffectCloud cloud = new ServerAreaEffectCloud(world); cloud.setPosition(0, 65, 0); cloud.metadata(0, 3F); cloud.metadata(1, 0x7f3fb2);
        for (int tick = 0; tick < 35; tick++) { cloud.onUpdate(); effects.updateEffects(); }
        EntityOtherPlayerMP camera = new EntityOtherPlayerMP(world, new GameProfile(new UUID(0, 745), "CloudCamera"));
        camera.setPosition(0, 67, 8); camera.prevPosX = 0; camera.prevPosY = 67; camera.prevPosZ = 8;
        camera.lastTickPosX = 0; camera.lastTickPosY = 67; camera.lastTickPosZ = 8;
        camera.rotationYaw = camera.prevRotationYaw = 180; camera.rotationPitch = camera.prevRotationPitch = 14;
        int width = 1000, height = 640;
        Framebuffer target = new Framebuffer(width, height, true); target.setFramebufferColor(.08F, .09F, .12F, 1); target.framebufferClear(); target.bindFramebuffer(true);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.loadIdentity(); GLU.gluPerspective(55, width / (float)height, .05F, 100);
        GlStateManager.matrixMode(5888); GlStateManager.pushMatrix(); GlStateManager.loadIdentity(); GLU.gluLookAt(0, 0, 0, 0, -2, -8, 0, 1, 0);
        try {
            ActiveRenderInfo.updateRenderInfo(camera, false);
            GlStateManager.enableDepth(); GlStateManager.enableAlpha(); GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            effects.renderParticles(camera, .5F);
            GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GlStateManager.ortho(0, width, height, 0, -100, 100);
            GlStateManager.matrixMode(5888); GlStateManager.loadIdentity(); GlStateManager.disableDepth();
            mc.fontRendererObj.drawString("Forge 1.8.9 | lingering cloud | native particle renderer", 20, 16, 0xffffff);
            mc.fontRendererObj.drawString("Server radius: 3 blocks | server RGB: 7F3FB2 | 35 client ticks", 20, 610, 0xffffff);
            ScreenShotHelper.saveScreenshot(directory.toFile(), "lingering-cloud-1.12.2.png", width, height, target);
        } finally {
            GlStateManager.matrixMode(5888); GlStateManager.popMatrix(); GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); GlStateManager.color(1, 1, 1, 1);
            ActiveRenderInfo.updateRenderInfo(mc.thePlayer, false);
        }
    }
}
