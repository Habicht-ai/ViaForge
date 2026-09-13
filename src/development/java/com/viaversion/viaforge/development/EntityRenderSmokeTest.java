package com.viaversion.viaforge.development;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.items.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Tests actual GL matrices against original vanilla JSON, plus review images of real renderers. */
final class EntityRenderSmokeTest {
    static void verify(BlockVersionProfile profile, WorldClient world, Path directory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        EntityOtherPlayerMP remote = new EntityOtherPlayerMP(world, new GameProfile(new UUID(0, 710), "ShieldPreview")); world.addEntityToWorld(710, remote);
        ItemStack shield = ServerEntitySmokeTest.stack(442, 0);
        Field field = ItemRenderer.class.getDeclaredField("itemRenderer"); field.setAccessible(true);
        Object original = field.get(mc.getItemRenderer()); Recorder recorder = new Recorder(mc); field.set(mc.getItemRenderer(), recorder);
        GlStateManager.pushMatrix();
        try (ZipFile zip = new ZipFile(new File(mc.mcDataDir, "ViaForge/block-assets/" + profile.resourceVersion() + "-client.jar"))) {
            for (boolean slim : new boolean[]{false, true}) {
            RenderPlayer renderer = new RenderPlayer(mc.getRenderManager(), slim); ModelBiped model = renderer.getMainModel(); model.isChild = false;
            LayerHeldItem layer = new LayerHeldItem(renderer);
            for (boolean left : new boolean[]{false, true}) for (boolean block : new boolean[]{false, true}) for (boolean sneak : new boolean[]{false, true}) {
                remote.setCurrentItemOrArmor(0, left ? null : shield); remote.setSneaking(sneak);
                pose(profile, 710, left, block, left ? shield : null);
                model.heldItemRight = left ? 0 : 1; model.isSneak = sneak; model.swingProgress = 0;
                model.setRotationAngles(0, 0, 0, 0, 0, .0625F, remote);
                float angle = left ? model.bipedLeftArm.rotateAngleX : model.bipedRightArm.rotateAngleX;
                ServerEntitySmokeTest.require(Math.abs(angle - ((block ? -.9424779F : -.31415927F) + (sneak ? .4F : 0))) < .0001, "Native shield arm angle");
                GlStateManager.loadIdentity(); recorder.matrix = null;
                layer.doRenderLayer(remote, 0, 0, 0, 0, 0, 0, .0625F);
                float[] actual = recorder.matrix; ServerEntitySmokeTest.require(actual != null, "Shield rendered in either hand");
                JsonObject asset;
                try (Reader reader = new InputStreamReader(zip.getInputStream(zip.getEntry("assets/minecraft/models/item/shield" + (block ? "_blocking" : "") + ".json")), "UTF-8")) { asset = new JsonParser().parse(reader).getAsJsonObject(); }
                JsonObject display = asset.getAsJsonObject("display").getAsJsonObject("thirdperson_" + (left ? "lefthand" : "righthand"));
                GlStateManager.loadIdentity();
                net.minecraft.client.model.ModelRenderer arm = left ? model.bipedLeftArm : model.bipedRightArm;
                float offset = slim ? left ? -.5F : .5F : 0;
                arm.rotationPointX += offset; arm.postRender(1F / 16); arm.rotationPointX -= offset;
                if (sneak) GlStateManager.translate(0, .2F, 0);
                GlStateManager.rotate(-90, 1, 0, 0); GlStateManager.rotate(180, 0, 1, 0); GlStateManager.translate((left ? -1 : 1) / 16F, .125F, -.625F);
                JsonArray t = display.getAsJsonArray("translation"), r = display.getAsJsonArray("rotation"), s = display.getAsJsonArray("scale");
                GlStateManager.translate(t.get(0).getAsFloat() / 16 * (left ? -1 : 1), t.get(1).getAsFloat() / 16, t.get(2).getAsFloat() / 16);
                GlStateManager.rotate(r.get(0).getAsFloat(), 1, 0, 0); GlStateManager.rotate(r.get(1).getAsFloat() * (left ? -1 : 1), 0, 1, 0); GlStateManager.rotate(r.get(2).getAsFloat() * (left ? -1 : 1), 0, 0, 1);
                GlStateManager.scale(s.get(0).getAsFloat() * 2, s.get(1).getAsFloat() * 2, s.get(2).getAsFloat() * 2);
                float[] expected = matrix();
                for (int i = 0; i < 16; i++) ServerEntitySmokeTest.require(Math.abs(actual[i] - expected[i]) < .0001, "Target shield matrix " + profile + " left=" + left + " block=" + block + " sneak=" + sneak + " element=" + i + ": " + actual[i] + " != " + expected[i]);
            }
            }
        } finally { field.set(mc.getItemRenderer(), original); GlStateManager.popMatrix(); remote.setSneaking(false); }
        try {
            if (profile == BlockVersionProfile.V1_9 || profile == BlockVersionProfile.V1_12_2) preview(profile, directory, remote, shield);
            particleGeometry();
            if (profile == BlockVersionProfile.V1_12_2) CloudRenderPreview.capture(directory);
            if (profile == BlockVersionProfile.V1_11 || profile == BlockVersionProfile.V1_12_2) ServerEffectsPreview.capture(profile, directory);
        } finally { world.removeEntityFromWorld(710); ServerEntityViews.clear(); }
    }
    private static void pose(BlockVersionProfile profile, int id, boolean left, boolean blocking, ItemStack offhand) throws Exception {
        int first = profile.protocol() >= 210 ? 6 : 5;
        ByteBuf data = Unpooled.buffer();
        try { data.writeShort(profile.protocol()).writeByte(2); Types.VAR_INT.writePrimitive(data, id); data.writeByte(first).writeByte(0).writeByte(blocking ? left ? 3 : 1 : 0).writeByte(255); ServerEntityViews.accept(data); }
        finally { data.release(); }
        data = Unpooled.buffer();
        try {
            data.writeShort(profile.protocol()).writeByte(3); Types.VAR_INT.writePrimitive(data, id); Types.VAR_INT.writePrimitive(data, 1);
            com.viaversion.viaversion.api.minecraft.item.Item item = offhand == null ? null : ServerItemSmokeTest.wire(offhand); if (item != null) item.setIdentifier(442);
            Types.ITEM1_8.write(data, item); ServerEntityViews.accept(data);
        } finally { data.release(); }
    }
    private static void preview(BlockVersionProfile profile, Path directory, EntityOtherPlayerMP remote, ItemStack shield) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); int width = 1200, height = 650;
        Framebuffer target = new Framebuffer(width, height, true); target.setFramebufferColor(.08F, .09F, .12F, 1); target.framebufferClear(); target.bindFramebuffer(true);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.loadIdentity(); GlStateManager.ortho(0, width, height, 0, -3000, 3000);
        GlStateManager.matrixMode(5888); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        mc.getRenderManager().cacheActiveRenderInfo(mc.theWorld, mc.fontRendererObj, mc.thePlayer, null, mc.gameSettings, 0);
        try {
            GlStateManager.disableLighting(); mc.fontRendererObj.drawString("Forge 1.8.9 | target " + profile.resourceVersion() + " | actual player / inventory / projectile renderers", 16, 14, 0xffffff);
            for (int i = 0; i < 4; i++) {
                boolean left = i >= 2, block = (i & 1) != 0; int x = i * 300;
                GlStateManager.disableLighting(); GlStateManager.disableDepth(); Gui.drawRect(x + 8, 38, x + 292, 390, 0xff252b35);
                mc.fontRendererObj.drawString((left ? "Offhand" : "Main hand") + (block ? " blocking" : " idle"), x + 16, 48, 0xffffff);
                remote.setCurrentItemOrArmor(0, left ? null : shield); pose(profile, 710, left, block, left ? shield : null);
                GlStateManager.enableDepth(); GuiInventory.drawEntityOnScreen(x + 150, 365, 125, -30, -8, remote);
            }
            RenderHelper.enableGUIStandardItemLighting(); GlStateManager.enableAlpha();
            ItemStack head = ServerEntitySmokeTest.stack(397, 5);
            GlStateManager.pushMatrix(); GlStateManager.translate(70, 440, 0); GlStateManager.scale(7, 7, 7); mc.getRenderItem().renderItemAndEffectIntoGUI(head, 0, 0); GlStateManager.popMatrix();
            for (int i = 0; i < 2; i++) {
                int id = 730 + i; EntityPotion potion = new EntityPotion(mc.theWorld); mc.theWorld.addEntityToWorld(id, potion);
                ByteBuf data = Unpooled.buffer();
                try {
                    data.writeShort(profile.protocol()).writeByte(1); Types.VAR_INT.writePrimitive(data, id); data.writeLong(0).writeLong(id).writeByte(73).writeDouble(0).writeDouble(0).writeDouble(0); ServerEntityViews.accept(data);
                    ServerEntityViews.get(id).potion = ItemVariants.potion(ServerEntitySmokeTest.stack(i == 0 ? 438 : 441, 0), "healing");
                    GlStateManager.pushMatrix(); GlStateManager.translate(530 + i * 330, 515, 100); GlStateManager.scale(230, -230, 230);
                    mc.getRenderManager().playerViewY = 0; mc.getRenderManager().playerViewX = 0;
                    mc.getRenderManager().getEntityRenderObject(potion).doRender(potion, 0, 0, 0, 0, 0); GlStateManager.popMatrix();
                } finally { data.release(); mc.theWorld.removeEntityFromWorld(id); }
            }
            RenderHelper.disableStandardItemLighting(); GlStateManager.disableDepth();
            mc.fontRendererObj.drawString("Dragon head (inventory)", 50, 600, 0xffffff);
            mc.fontRendererObj.drawString("Thrown splash potion", 430, 600, 0xffffff);
            mc.fontRendererObj.drawString("Thrown lingering potion", 760, 600, 0xffffff);
            ScreenShotHelper.saveScreenshot(directory.toFile(), "entity-visuals-" + profile.resourceVersion() + ".png", width, height, target);
        } finally {
            GlStateManager.matrixMode(5888); GlStateManager.popMatrix(); GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); GlStateManager.color(1, 1, 1, 1);
        }
    }
    private static void particleGeometry() {
        final List<double[]> particles = new ArrayList<>();
        WorldClient world = new WorldClient(null, new net.minecraft.world.WorldSettings(0, net.minecraft.world.WorldSettings.GameType.CREATIVE, false, false, net.minecraft.world.WorldType.DEFAULT), 0, net.minecraft.world.EnumDifficulty.PEACEFUL, new net.minecraft.profiler.Profiler()) {
            @Override public void spawnParticle(EnumParticleTypes type, double x, double y, double z, double r, double g, double b, int... args) { particles.add(new double[]{x, y, z, r, g, b}); }
        };
        ServerAreaEffectCloud cloud = new ServerAreaEffectCloud(world); cloud.setPosition(4, 65, 4); cloud.metadata(0, 2F); cloud.metadata(1, 0x804020); cloud.onUpdate();
        ServerEntitySmokeTest.require(particles.size() == 13, "Native cloud particle density pi*r*r");
        for (double[] point : particles) {
            ServerEntitySmokeTest.require(Math.pow(point[0] - 4, 2) + Math.pow(point[2] - 4, 2) <= 4 && point[1] == 65, "Particles inside server cloud radius");
            ServerEntitySmokeTest.require(point[3] == 128 / 255D && point[4] == 64 / 255D && point[5] == 32 / 255D, "Cloud uses server RGB");
        }
    }
    private static float[] matrix() { FloatBuffer buffer = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer); float[] values = new float[16]; buffer.get(values); return values; }
    private static final class Recorder extends RenderItem {
        float[] matrix;
        Recorder(Minecraft mc) { super(mc.getTextureManager(), mc.getRenderItem().getItemModelMesher().getModelManager()); }
        @Override public void renderItem(ItemStack stack, IBakedModel model) { matrix = EntityRenderSmokeTest.matrix(); }
    }
}
