package com.viaversion.viaforge.development;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import com.viaversion.viaforge.common.compatibility.CompatibilityProfile;
import com.viaversion.viaforge.hands.HandModels;
import com.viaversion.viaforge.items.ServerEntityViews;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Real renderer matrices compared with unconverted, inherited target item JSON. */
final class ShulkerItemRenderSmokeTest {
    static void verify(CompatibilityProfile target, Path directory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient previousWorld = mc.theWorld;
        EntityPlayerSP previousPlayer = mc.thePlayer;
        ItemStack previousModel = HandModels.current;
        boolean previousLeft = HandModels.left;
        WorldClient world = new WorldClient(null, new WorldSettings(0, WorldSettings.GameType.CREATIVE,
                false, false, WorldType.DEFAULT), 0, EnumDifficulty.PEACEFUL, new Profiler());
        world.doPreChunk(0, 0, true);
        EntityOtherPlayerMP remote = new EntityOtherPlayerMP(world, new GameProfile(new UUID(0, 1710), "ShulkerPreview"));
        Field field = ItemRenderer.class.getDeclaredField("itemRenderer"); field.setAccessible(true);
        Object original = field.get(mc.getItemRenderer());
        Recorder recorder = new Recorder(mc);
        mc.theWorld = world;
        mc.thePlayer = new EntityPlayerSP(mc, world, new NetHandlerPlayClient(mc, null,
                new NetworkManager(EnumPacketDirection.CLIENTBOUND), new GameProfile(new UUID(0, 1711), "ShulkerTest")), new StatFileWriter());
        world.addEntityToWorld(1710, remote);
        net.minecraft.scoreboard.ScorePlayerTeam team = world.getScoreboard().createTeam("shulker_preview");
        team.setNameTagVisibility(net.minecraft.scoreboard.Team.EnumVisible.NEVER);
        world.getScoreboard().addPlayerToTeam(remote.getName(), team.getRegisteredName());
        field.set(mc.getItemRenderer(), recorder);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.pushMatrix();
        try (ZipFile zip = new ZipFile(new File(mc.mcDataDir, "ViaForge/block-assets/" + target.resources().version() + "-client.jar"))) {
            // Create only the internal equipment view; packet translation is tested separately.
            ByteBuf event = Unpooled.buffer();
            try {
                event.writeShort(340).writeByte(3); Types.VAR_INT.writePrimitive(event, 1710);
                Types.VAR_INT.writePrimitive(event, 1); Types.ITEM1_8.write(event, null); ServerEntityViews.accept(event);
            } finally { event.release(); }
            ServerEntityViews.View view = ServerEntityViews.get(1710);
            require(view != null, "Shulker renderer equipment fixture");
            nativeGround(world, recorder);
            String[] models = {"purpur_block", "beetroot_seeds", "shield", "diamond_sword"};
            int[] ids = {201, 435, 442};
            for (int i = 0; i < models.length; i++) {
                ItemStack item = i < ids.length ? ServerEntitySmokeTest.stack(ids[i], 0)
                        : new ItemStack(net.minecraft.init.Items.diamond_sword);
                dropped(world, recorder, item, originalDisplay(zip, "item/" + models[i], 0),
                        target.resources().version() + " " + models[i]);
            }
            for (int color = 0; color < (target.serverProtocol() >= 315 ? 16 : 0); color++) {
                String name = LegacyBlockCatalog.COLORS[color];
                if (target.serverProtocol() >= 393 && color == 8) name = "light_gray";
                JsonObject display = originalDisplay(zip, "item/" + name + "_shulker_box", 0);
                String label = target.resources().version() + " " + name + " shulker";
                ItemStack stack = stack(color);
                for (boolean slim : new boolean[]{false, true}) {
                    RenderPlayer renderer = new RenderPlayer(mc.getRenderManager(), slim);
                    ModelBiped model = renderer.getMainModel(); model.isChild = false;
                    LayerHeldItem layer = new LayerHeldItem(renderer);
                    for (boolean left : new boolean[]{false, true}) for (boolean sneak : new boolean[]{false, true}) {
                        remote.setCurrentItemOrArmor(0, left ? null : stack); view.offhand = left ? stack : null;
                        remote.setSneaking(sneak); model.isSneak = sneak;
                        model.heldItemRight = left ? 0 : 1; model.heldItemLeft = left ? 1 : 0; model.swingProgress = 0;
                        model.setRotationAngles(0, 0, 0, 0, 0, 1F / 16, remote);
                        GlStateManager.loadIdentity(); recorder.matrix = null;
                        layer.doRenderLayer(remote, 0, 0, 0, 0, 0, 0, 1F / 16);
                        float[] actual = recorder.matrix;
                        GlStateManager.loadIdentity();
                        ModelRenderer arm = left ? model.bipedLeftArm : model.bipedRightArm;
                        float offset = slim ? (left ? -.5F : .5F) : 0;
                        arm.rotationPointX += offset;
                        try { arm.postRender(1F / 16); } finally { arm.rotationPointX -= offset; }
                        if (sneak) GlStateManager.translate(0, .2F, 0);
                        GlStateManager.rotate(-90, 1, 0, 0); GlStateManager.rotate(180, 0, 1, 0);
                        GlStateManager.translate((left ? -1 : 1) / 16F, .125F, -.625F);
                        originalTransform(display, "thirdperson", left); GlStateManager.scale(2, 2, 2);
                        compare(actual, label + " third person left=" + left + " slim=" + slim + " sneak=" + sneak);
                    }
                }
                for (boolean left : new boolean[]{false, true}) {
                    HandModels.current = stack; HandModels.left = left;
                    GlStateManager.loadIdentity(); recorder.matrix = null;
                    recorder.renderItemModelForEntity(stack, remote, TransformType.FIRST_PERSON);
                    HandModels.current = null;
                    GlStateManager.loadIdentity(); originalTransform(display, "firstperson", left); GlStateManager.scale(2, 2, 2);
                    compare(recorder.matrix, label + " first person left=" + left);
                }
                GlStateManager.loadIdentity(); recorder.matrix = null;
                recorder.renderItemIntoGUI(stack, 0, 0);
                GlStateManager.loadIdentity(); GlStateManager.translate(8, 8, 100); GlStateManager.scale(16, -16, 16);
                originalTransform(display, "gui", false); GlStateManager.scale(2, 2, 2);
                compare(recorder.matrix, label + " inventory");
                GlStateManager.loadIdentity(); recorder.matrix = null;
                recorder.renderItemModelForEntity(stack, remote, TransformType.FIXED);
                GlStateManager.loadIdentity(); originalTransform(display, "fixed", false); GlStateManager.scale(2, 2, 2);
                compare(recorder.matrix, label + " fixed model");
                dropped(world, recorder, stack, display, label);
            }
            remote.setSneaking(false); view.offhand = null;
            field.set(mc.getItemRenderer(), original);
            String version = target.resources().version();
            if (version.equals("1.11") || version.equals("1.12.2") || version.equals("1.13.2") || version.equals("1.14.4"))
                preview(version, directory, remote, view);
        } finally {
            field.set(mc.getItemRenderer(), original); GlStateManager.popMatrix();
            ServerEntityViews.clear(); mc.theWorld = previousWorld; mc.thePlayer = previousPlayer;
            HandModels.current = previousModel; HandModels.left = previousLeft;
        }
    }

    private static void dropped(WorldClient world, Recorder recorder, ItemStack stack, JsonObject display, String label) {
        EntityItem entity = new EntityItem(world, 0, 0, 0, stack); entity.hoverStart = 0;
        RenderEntityItem renderer = new RenderEntityItem(Minecraft.getMinecraft().getRenderManager(), recorder) {
            @Override public boolean shouldBob() { return false; }
        };
        GlStateManager.loadIdentity(); recorder.matrix = null;
        renderer.doRender(entity, 0, 0, 0, 0, 0);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0, .25F * display.getAsJsonObject("ground").getAsJsonArray("scale").get(1).getAsFloat(), 0);
        originalTransform(display, "ground", false); GlStateManager.scale(2, 2, 2);
        compare(recorder.matrix, label + " dropped item");
    }

    static void disconnected() {
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = new WorldClient(null, new WorldSettings(0, WorldSettings.GameType.CREATIVE,
                false, false, WorldType.DEFAULT), 0, EnumDifficulty.PEACEFUL, new Profiler());
        GlStateManager.pushMatrix();
        try { nativeGround(world, new Recorder(mc)); } finally { GlStateManager.popMatrix(); }
    }

    private static void nativeGround(WorldClient world, Recorder recorder) {
        ItemStack stone = new ItemStack(net.minecraft.init.Blocks.stone);
        EntityItem entity = new EntityItem(world, 0, 0, 0, stone); entity.hoverStart = 0;
        GlStateManager.loadIdentity(); recorder.matrix = null;
        new RenderEntityItem(Minecraft.getMinecraft().getRenderManager(), recorder).doRender(entity, 0, 0, 0, 0, 0);
        require(recorder.matrix != null, "Native dropped stone rendered");
        for (int axis = 0; axis < 3; axis++) {
            double length = 0;
            for (int row = 0; row < 3; row++) length += Math.pow(recorder.matrix[axis * 4 + row], 2);
            // Vanilla's .5 ground scale precedes RenderItem's final .5 geometry scale.
            require(Math.abs(Math.sqrt(length) - .5) < .0001, "Native dropped stone scale unchanged");
        }
    }

    private static JsonObject originalDisplay(ZipFile zip, String model, int depth) throws IOException {
        require(depth < 16, "Original shulker model parent depth");
        JsonObject json;
        try (Reader reader = new InputStreamReader(zip.getInputStream(zip.getEntry("assets/minecraft/models/" + model + ".json")), "UTF-8")) {
            json = new JsonParser().parse(reader).getAsJsonObject();
        }
        JsonObject display = new JsonObject();
        if (json.has("parent")) {
            String parent = json.get("parent").getAsString().replace("minecraft:", "");
            if (!parent.startsWith("builtin/")) display = originalDisplay(zip, parent, depth + 1);
        }
        if (json.has("display")) for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("display").entrySet())
            display.add(entry.getKey(), entry.getValue());
        return display;
    }

    private static void originalTransform(JsonObject display, String key, boolean left) {
        if (key.equals("firstperson") || key.equals("thirdperson")) {
            String side = key + (left ? "_lefthand" : "_righthand");
            key = display.has(side) ? side : key + "_righthand";
        }
        require(display.has(key), "Original target display " + key);
        JsonObject transform = display.getAsJsonObject(key);
        float[] t = vector(transform, "translation", 0), r = vector(transform, "rotation", 0), s = vector(transform, "scale", 1);
        float sign = left ? -1 : 1;
        GlStateManager.translate(sign * t[0] / 16, t[1] / 16, t[2] / 16);
        // Original modern order, independent of the conversion into 1.8's Y/X/Z order.
        GlStateManager.rotate(r[0], 1, 0, 0);
        GlStateManager.rotate(sign * r[1], 0, 1, 0);
        GlStateManager.rotate(sign * r[2], 0, 0, 1);
        GlStateManager.scale(s[0], s[1], s[2]);
    }

    private static float[] vector(JsonObject transform, String key, float fallback) {
        if (!transform.has(key)) return new float[]{fallback, fallback, fallback};
        JsonArray value = transform.getAsJsonArray(key);
        return new float[]{value.get(0).getAsFloat(), value.get(1).getAsFloat(), value.get(2).getAsFloat()};
    }

    private static ItemStack stack(int color) { return new ItemStack(Item.getItemById(ClientBlocks.localItem(219 + color, 0))); }
    private static float[] matrix() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
        float[] result = new float[16]; buffer.get(result); return result;
    }
    private static void compare(float[] actual, String label) {
        require(actual != null, label + " reached the real item renderer");
        float[] expected = matrix();
        for (int i = 0; i < 16; i++) require(Math.abs(actual[i] - expected[i]) < .0001F,
                label + " matrix element " + i + ": " + actual[i] + " != " + expected[i]);
    }
    private static final class Recorder extends RenderItem {
        float[] matrix;
        Recorder(Minecraft mc) { super(mc.getTextureManager(), mc.getRenderItem().getItemModelMesher().getModelManager()); }
        @Override public void renderItem(ItemStack stack, IBakedModel model) { matrix = ShulkerItemRenderSmokeTest.matrix(); }
    }

    private static void preview(String version, Path directory, EntityOtherPlayerMP remote, ServerEntityViews.View view) {
        Minecraft mc = Minecraft.getMinecraft(); int width = 1000, height = 540;
        Framebuffer target = new Framebuffer(width, height, true); target.setFramebufferColor(.08F, .09F, .12F, 1);
        target.framebufferClear(); target.bindFramebuffer(true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        GlStateManager.ortho(0, width, height, 0, -3000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        mc.getRenderManager().cacheActiveRenderInfo(mc.theWorld, mc.fontRendererObj, mc.thePlayer, null, mc.gameSettings, 0);
        try {
            GlStateManager.disableLighting(); mc.fontRendererObj.drawString("Target " + version + " | shulker items | original model transforms", 16, 14, 0xffffff);
            for (int i = 0; i < 4; i++) {
                int x = i * 250; boolean left = i >= 2; ItemStack stack = stack((i & 1) == 0 ? 1 : 10);
                GlStateManager.disableLighting(); GlStateManager.disableDepth();
                Gui.drawRect(x + 8, 38, x + 242, 426, 0xff252b35);
                mc.fontRendererObj.drawString(left ? "Offhand" : "Main hand", x + 16, 48, 0xffffff);
                remote.setCurrentItemOrArmor(0, left ? null : stack); view.offhand = left ? stack : null;
                GlStateManager.enableDepth(); GuiInventory.drawEntityOnScreen(x + 125, 400, 140, -30, -8, remote);
                RenderHelper.enableGUIStandardItemLighting(); GlStateManager.enableAlpha();
                GlStateManager.pushMatrix(); GlStateManager.translate(x + 95, 448, 0); GlStateManager.scale(4, 4, 4);
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0); GlStateManager.popMatrix();
            }
            ScreenShotHelper.saveScreenshot(directory.toFile(), "shulker-items-" + version + ".png", width, height, target);
        } finally {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.popMatrix(); GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true);
            RenderHelper.disableStandardItemLighting(); GlStateManager.color(1, 1, 1, 1);
        }
    }
    private ShulkerItemRenderSmokeTest() { }
}
