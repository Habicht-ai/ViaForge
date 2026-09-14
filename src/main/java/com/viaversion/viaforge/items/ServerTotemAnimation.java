package com.viaversion.viaforge.items;

import com.viaversion.viaforge.compatibility.ServerSession;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

/** World-scoped status 35: 30 ticks of particles, with the original 40-tick local item activation. */
public final class ServerTotemAnimation {
    private static final Random RANDOM = new Random();
    private static final List<Emitter> EMITTERS = new ArrayList<>();
    private static WorldClient world;
    private static ItemStack item;
    private static int remaining;
    private static float offsetX, offsetY;
    public static void clear() { EMITTERS.clear(); world = null; item = null; remaining = 0; }
    public static void activate(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.TOTEM) || entity.worldObj != mc.theWorld) return;
        if (world != mc.theWorld) { clear(); world = mc.theWorld; }
        Emitter emitter = new Emitter(entity); emitter.tick(); EMITTERS.add(emitter);
        entity.worldObj.playSound(entity.posX, entity.posY, entity.posZ, "viaforge:totem_use", 1, 1, false);
        if (entity == mc.thePlayer) {
            item = new ItemStack(Item.getItemById(ClientItems.localItem(449, 0)));
            remaining = 40; offsetX = RANDOM.nextFloat() * 2 - 1; offsetY = RANDOM.nextFloat() * 2 - 1;
        }
    }
    public static void tick() {
        if (world != Minecraft.getMinecraft().theWorld || !ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.TOTEM)) { clear(); return; }
        if (remaining > 0 && --remaining == 0) item = null;
        for (Iterator<Emitter> iterator = EMITTERS.iterator(); iterator.hasNext();) {
            Emitter emitter = iterator.next(); emitter.tick();
            if (emitter.age >= 30) iterator.remove();
        }
    }
    public static void render(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (remaining <= 0 || item == null || world != mc.theWorld || !ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.TOTEM)) return;
        ScaledResolution resolution = new ScaledResolution(mc);
        int width = resolution.getScaledWidth(), height = resolution.getScaledHeight();
        float progress = (40 - remaining + partialTicks) / 40F, square = progress * progress, cube = square * progress;
        float phase = (10.25F * cube * square - 24.95F * square * square + 25.5F * cube - 13.8F * square + 4F * progress) * (float)Math.PI;
        mc.entityRenderer.setupOverlayRendering();
        GlStateManager.enableAlpha(); GlStateManager.pushMatrix(); GlStateManager.pushAttrib();
        GlStateManager.enableDepth(); GlStateManager.disableCull(); RenderHelper.enableStandardItemLighting();
        try {
            float drift = MathHelper.abs(MathHelper.sin(phase * 2));
            GlStateManager.translate(width / 2 + offsetX * (width / 4) * drift, height / 2 + offsetY * (height / 4) * drift, -50F);
            float scale = 50 + 175 * MathHelper.sin(phase);
            GlStateManager.scale(scale, -scale, scale);
            GlStateManager.rotate(900 * MathHelper.abs(MathHelper.sin(phase)), 0, 1, 0);
            GlStateManager.rotate(6 * MathHelper.cos(progress * 8), 1, 0, 0);
            GlStateManager.rotate(6 * MathHelper.cos(progress * 8), 0, 0, 1);
            mc.getRenderItem().renderItem(item, ItemCameraTransforms.TransformType.FIXED);
        } finally {
            GlStateManager.popAttrib(); GlStateManager.popMatrix(); RenderHelper.disableStandardItemLighting();
            GlStateManager.enableCull(); GlStateManager.disableDepth();
        }
    }
    private static void particle(WorldClient world, boolean longDistance, double x, double y, double z, double vx, double vy, double vz) {
        Minecraft mc = Minecraft.getMinecraft(); Entity camera = mc.getRenderViewEntity();
        if (camera == null || longDistance && camera.getDistanceSq(x, y, z) > 65536) return;
        ServerVisualParticles.spawn(world, 47, longDistance, x, y, z, vx, vy, vz);
    }
    private static final class Emitter {
        final Entity entity; int age;
        Emitter(Entity entity) { this.entity = entity; }
        void tick() {
            for (int i = 0; i < 16; i++) {
                double x = RANDOM.nextFloat() * 2 - 1, y = RANDOM.nextFloat() * 2 - 1, z = RANDOM.nextFloat() * 2 - 1;
                if (x * x + y * y + z * z > 1) continue;
                particle(world, false, entity.posX + x * entity.width / 4,
                        entity.getEntityBoundingBox().minY + entity.height / 2 + y * entity.height / 4,
                        entity.posZ + z * entity.width / 4, x, y + .2, z);
            }
            age++;
        }
    }
    private ServerTotemAnimation() { }
}
