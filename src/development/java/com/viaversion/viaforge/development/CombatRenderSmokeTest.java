package com.viaversion.viaforge.development;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.items.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.model.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.*;
import org.lwjgl.util.glu.GLU;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

/** Native renderers and Forge HUD integration, with diagnostic cape patterns. */
final class CombatRenderSmokeTest {
    static void verify(BlockVersionProfile profile, WorldClient world, Path directory) throws Exception {
        Minecraft mc = Minecraft.getMinecraft(); ItemStack elytra = stack(443, 0); ServerElytra item = (ServerElytra)elytra.getItem();
        ResourceLocation cape = mc.getTextureManager().getDynamicTextureLocation("cape-fixture", new DynamicTexture(pattern()));
        CapePlayer player = new CapePlayer(world, cape); player.setCurrentItemOrArmor(3, elytra);
        require(item.getArmorTexture(elytra, player, 3, null).equals(cape.toString()), "Visible cape becomes elytra texture " + profile);
        player.capeVisible = false; require(item.getArmorTexture(elytra, player, 3, null).equals("viaforge:textures/entity/elytra.png"), "Hidden cape uses default wings");
        player.capeVisible = true;
        TrackingRenderer renderer = new TrackingRenderer(mc.getRenderManager()); LayerCape layer = new LayerCape(renderer);
        GlStateManager.pushMatrix();
        try {
            renderer.binds = 0; layer.doRenderLayer(player, 0, 0, .5F, 0, 0, 0, .0625F); require(renderer.binds == 0, "Cape layer suppressed with elytra");
            player.setCurrentItemOrArmor(3, null); layer.doRenderLayer(player, 0, 0, .5F, 0, 0, 0, .0625F); require(renderer.binds == 1, "Cape visible again without elytra");
            player.setCurrentItemOrArmor(3, elytra);
            ModelBiped model = item.getArmorModel(player, elytra, 3);
            Field left = model.getClass().getDeclaredField("left"); left.setAccessible(true); ModelRenderer wing = (ModelRenderer)left.get(model);
            float angle = wing.rotateAngleX;
            for (int i = 0; i < 3; i++) model.render(player, 0, 0, 0, 0, 0, .0625F);
            require(wing.rotateAngleX == angle && wing.rotationPointZ == 0, "Glint passes retain same wing pose and translated pivot");
        } finally { GlStateManager.popMatrix(); }
        if (profile == BlockVersionProfile.V1_9 || profile == BlockVersionProfile.V1_12_2) preview(profile, directory, player);
        mc.getTextureManager().deleteTexture(cape);
    }
    private static void preview(BlockVersionProfile profile, Path directory, CapePlayer player) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        int oldWidth = mc.displayWidth, oldHeight = mc.displayHeight, oldScale = mc.gameSettings.guiScale, oldThird = mc.gameSettings.thirdPersonView;
        Entity oldView = mc.getRenderViewEntity(), oldPointed = mc.pointedEntity; ItemStack oldHeld = mc.thePlayer.getHeldItem(); PlayerControllerMP oldController = mc.playerController;
        EffectRenderer oldEffects = mc.effectRenderer;
        mc.displayWidth = 1000; mc.displayHeight = 640; mc.gameSettings.guiScale = 2; mc.gameSettings.thirdPersonView = 0;
        mc.playerController = new PlayerControllerMP(mc, mc.thePlayer.sendQueue); mc.playerController.setGameType(net.minecraft.world.WorldSettings.GameType.CREATIVE);
        mc.setRenderViewEntity(mc.thePlayer); Framebuffer frame = new Framebuffer(1000, 640, true); frame.setFramebufferColor(.09F, .1F, .13F, 1);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.matrixMode(5888); GlStateManager.pushMatrix();
        try {
            frame.framebufferClear(); frame.bindFramebuffer(true); overlay();
            for (int i = 0; i < 3; i++) {
                player.capeVisible = i != 2; player.setCurrentItemOrArmor(3, i == 0 ? null : stack(443, 0));
                if (i != 0) for (int step = 0; step < 60; step++) ((ServerElytra)player.getCurrentArmor(2).getItem()).getArmorModel(player, player.getCurrentArmor(2), 3);
                renderBack(175 + i * 325, 530, player);
                mc.fontRendererObj.drawString(i == 0 ? "Cape" : i == 1 ? "Cape on elytra" : "Hidden cape: default elytra", 70 + i * 325, 575, 0xffffff);
            }
            mc.fontRendererObj.drawString("Forge " + profile.resourceVersion() + " | native cape/wing rendering | diagnostic cape pattern", 20, 16, 0xffffff);
            ScreenShotHelper.saveScreenshot(directory.toFile(), "elytra-cape-" + profile.resourceVersion() + ".png", 1000, 640, frame);
            mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = new ItemStack(Items.diamond_sword); ServerCombatState.clear(); ServerCombatState.tick(mc.thePlayer);
            ItemRenderer hand = new ItemRenderer(mc); ServerCombatSmokeTest.set(hand, "itemToRender", mc.thePlayer.getHeldItem());
            for (int tick : new int[]{0, 4, 9, 14}) {
                ServerCombatState.attack(); for (int i = 0; i < tick; i++) ServerCombatState.tick(mc.thePlayer);
                float charge = ServerCombatState.strength(1), progress = charge * charge * charge;
                ServerCombatSmokeTest.set(hand, "equippedProgress", progress); ServerCombatSmokeTest.set(hand, "prevEquippedProgress", progress);
                mc.thePlayer.swingProgress = mc.thePlayer.prevSwingProgress = tick < 6 ? tick / 6F : 0;
                frame.framebufferClear(); frame.bindFramebuffer(true);
                GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GLU.gluPerspective(70, 1000F / 640, .05F, 100);
                GlStateManager.matrixMode(5888); GlStateManager.loadIdentity(); GlStateManager.enableDepth();
                hand.renderItemInFirstPerson(.5F);
                mc.entityRenderer.setupOverlayRendering();
                player.setHealth(20); mc.pointedEntity = player;
                if (tick == 14) {
                    require(player.isEntityAlive() && ServerCombatState.strength(0) >= 1 && ServerCombatState.period() > 5, "Charged HUD target is alive and sword ready");
                    require(com.viaversion.viaforge.blocks.ServerBlockSession.supportsProtocol(316) == (profile.protocol() >= 316), "HUD uses current target version");
                    ResourceLocation iconLocation = new ResourceLocation("viaforge:textures/gui/icons.png");
                    BufferedImage icons;
                    try (java.io.InputStream input = mc.getResourceManager().getResource(iconLocation).getInputStream()) { icons = javax.imageio.ImageIO.read(input); }
                    int visible = 0; for (int yy = 94; yy < 110; yy++) for (int xx = 68; xx < 84; xx++) if ((icons.getRGB(xx, yy) >>> 24) != 0) visible++;
                    require((visible > 0) == (profile.protocol() >= 316), "HUD loads ready icon from current target texture");
                    mc.getTextureManager().bindTexture(iconLocation);
                    java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(icons.getWidth() * icons.getHeight() * 4);
                    org.lwjgl.opengl.GL11.glGetTexImage(3553, 0, 6408, 5121, pixels);
                    int uploaded = 0; for (int yy = 94; yy < 110; yy++) for (int xx = 68; xx < 84; xx++) if (pixels.get((yy * icons.getWidth() + xx) * 4 + 3) != 0) uploaded++;
                    require(uploaded == visible, "GPU HUD texture follows server changes instead of retaining earlier versions");
                }
                // Exercise the actual Forge HUD, which overrides vanilla's HUD method.
                mc.ingameGUI.renderGameOverlay(.5F);
                overlay(); mc.fontRendererObj.drawString("Target sword hand and Forge attack indicator | tick " + tick + " | " + profile.resourceVersion(), 20, 16, 0xffffff);
                ScreenShotHelper.saveScreenshot(directory.toFile(), "sword-swing-" + profile.resourceVersion() + "-tick-" + tick + ".png", 1000, 640, frame);
            }
            EntityOtherPlayerMP camera = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(new UUID(0, 990), "SweepCamera"));
            camera.setPosition(0, 76, 5); camera.lastTickPosY = camera.prevPosY = 76; camera.lastTickPosZ = camera.prevPosZ = 5; camera.rotationYaw = 180;
            camera.lastTickPosX = camera.prevPosX = 0;
            mc.setRenderViewEntity(camera); mc.effectRenderer = new EffectRenderer(mc.theWorld, mc.getTextureManager());
            for (int i = 0; i < 3; i++) {
                ServerSweepParticle fx = new ServerSweepParticle(mc.theWorld, (i - 1) * 2.5, 76, 0, 0);
                for (int tick = 0; tick < i + 1; tick++) fx.onUpdate(); mc.effectRenderer.addEffect(fx);
            }
            frame.framebufferClear(); frame.bindFramebuffer(true);
            GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GLU.gluPerspective(65, 1000F / 640, .05F, 100);
            GlStateManager.matrixMode(5888); GlStateManager.loadIdentity(); GlStateManager.enableAlpha(); GlStateManager.enableTexture2D(); GlStateManager.disableLighting(); GlStateManager.disableCull();
            ActiveRenderInfo.updateRenderInfo(camera, false); mc.effectRenderer.renderParticles(camera, .1F); mc.effectRenderer.renderLitParticles(camera, .1F);
            overlay(); mc.fontRendererObj.drawString("Original sweep sheet | animation ticks 1 / 2 / 3 | " + profile.resourceVersion(), 20, 16, 0xffffff);
            ScreenShotHelper.saveScreenshot(directory.toFile(), "sweep-" + profile.resourceVersion() + ".png", 1000, 640, frame);
        } finally {
            mc.thePlayer.swingProgress = mc.thePlayer.prevSwingProgress = 0; mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = oldHeld;
            mc.displayWidth = oldWidth; mc.displayHeight = oldHeight; mc.gameSettings.guiScale = oldScale; mc.gameSettings.thirdPersonView = oldThird;
            mc.setRenderViewEntity(oldView); mc.pointedEntity = oldPointed; mc.playerController = oldController; mc.effectRenderer = oldEffects; ServerCombatState.clear();
            GlStateManager.matrixMode(5888); GlStateManager.popMatrix(); GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            frame.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); GlStateManager.color(1, 1, 1, 1);
        }
    }
    private static void renderBack(int x, int y, CapePlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        player.renderYawOffset = player.prevRenderYawOffset = 180; player.rotationYaw = player.prevRotationYaw = 180; player.rotationYawHead = player.prevRotationYawHead = 180;
        GlStateManager.pushMatrix(); GlStateManager.enableColorMaterial(); GlStateManager.translate(x, y, 50); GlStateManager.scale(-140, 140, 140); GlStateManager.rotate(180, 0, 0, 1);
        RenderHelper.enableStandardItemLighting(); GlStateManager.enableDepth();
        RenderManager manager = mc.getRenderManager(); manager.setRenderShadow(false);
        manager.renderEntityWithPosYaw(player, 0, 0, 0, 0, 1);
        manager.setRenderShadow(true); RenderHelper.disableStandardItemLighting(); GlStateManager.disableRescaleNormal(); GlStateManager.popMatrix(); GlStateManager.disableDepth();
    }
    private static void overlay() {
        GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GlStateManager.ortho(0, 1000, 640, 0, -1000, 1000);
        GlStateManager.matrixMode(5888); GlStateManager.loadIdentity(); GlStateManager.disableDepth(); GlStateManager.disableLighting(); GlStateManager.color(1, 1, 1, 1);
    }
    private static BufferedImage pattern() {
        BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) image.setRGB(x, y, ((x / 3 + y / 3) % 2 == 0) ? 0xffffbb24 : 0xffbd263d);
        return image;
    }
    private static final class CapePlayer extends EntityOtherPlayerMP {
        final ResourceLocation cape; final NetworkPlayerInfo info; boolean capeVisible = true;
        CapePlayer(WorldClient world, ResourceLocation cape) {
            super(world, new GameProfile(new UUID(0, 998), "CapePreview")); this.cape = cape; info = new NetworkPlayerInfo(getGameProfile());
            setPosition(0, 75, 0); prevPosY = prevChasingPosY = chasingPosY = 75; prevPosX = prevChasingPosX = chasingPosX = 0; prevPosZ = prevChasingPosZ = chasingPosZ = 0;
        }
        @Override public boolean hasPlayerInfo() { return true; }
        @Override protected NetworkPlayerInfo getPlayerInfo() { return info; }
        @Override public ResourceLocation getLocationCape() { return cape; }
        @Override public ResourceLocation getLocationSkin() { return DefaultPlayerSkin.getDefaultSkin(getUniqueID()); }
        @Override public boolean isWearing(EnumPlayerModelParts part) { return part == EnumPlayerModelParts.CAPE ? capeVisible : true; }
    }
    private static final class TrackingRenderer extends RenderPlayer {
        int binds;
        TrackingRenderer(RenderManager manager) { super(manager); }
        @Override public void bindTexture(ResourceLocation texture) { binds++; super.bindTexture(texture); }
    }
}
