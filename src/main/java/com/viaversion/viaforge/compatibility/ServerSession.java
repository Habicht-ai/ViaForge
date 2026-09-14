package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;

import com.viaversion.viaforge.blocks.resources.BlockAssetCache;
import com.viaversion.viaforge.blocks.resources.VersionBlockPack;
import com.viaversion.viaforge.mixin.impl.blocks.MinecraftResourcePacks;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/** All session/resource changes happen on Minecraft's main thread. */
public final class ServerSession {
    private static final Logger LOGGER = Logger.getLogger("ViaForge/Blocks");
    private static final VersionBlockPack PACK = new VersionBlockPack();
    private static final ExecutorService DOWNLOADS = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ViaForge block resources");
        thread.setDaemon(true);
        return thread;
    });
    private static final SessionEpoch SESSION = new SessionEpoch();
    private static boolean resourcesLoaded;
    private static String loadedResourceVersion;

    private ServerSession() { }

    public static String getLoadedResourceVersion() { return loadedResourceVersion; }
    public static CompatibilityProfile profile() { return SESSION.profile(); }
    public static VersionRules rules() { return Minecraft.getMinecraft().isSingleplayer()?VersionRules.NATIVE:profile().rules(); }
    public static boolean has(ClientFeature feature) { return rules().has(feature); }
    public static boolean rule(ClientRule rule) { return rules().enabled(rule); }
    public static boolean contentSince(int introduction) { return rules().contentSince(introduction); }
    public static boolean supportsItem(com.viaversion.viaforge.common.blocks.LegacyItemDefinition item) {
        return has(ClientFeature.ITEMS)&&item!=null&&contentSince(item.itemProtocol());
    }
    public static boolean supports(com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition block) {
        return has(ClientFeature.BLOCKS)&&block!=null&&contentSince(block.protocol);
    }
    public static boolean supportsItem(com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition block) {
        return has(ClientFeature.ITEMS)&&supports(block)&&contentSince(block.itemProtocol());
    }

    public static void initialize() {
        Minecraft mc = Minecraft.getMinecraft();
        ((MinecraftResourcePacks) mc).viaForge$defaultResourcePacks().add(PACK);
        // Forge first builds block models before the next full resource reload.
        // Expose our fallback definitions to that initial bake as well.
        ((net.minecraft.client.resources.SimpleReloadableResourceManager) mc.getResourceManager()).reloadResourcePack(PACK);
    }

    public static void join(Object connection, CompatibilityProfile profile) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.isSingleplayer()) return;
            SessionEpoch.Ticket ticket = SESSION.begin(connection,profile);
            if (resourcesLoaded) resetResources(mc);
            if (!profile.extended()) return;
            DOWNLOADS.execute(() -> {
                try {
                    BlockAssetCache cache = new BlockAssetCache(mc.mcDataDir.toPath().resolve("ViaForge/block-assets"));
                    Map<String, byte[]> assets = profile.resources().normalize(cache.load(profile.resources().version()));
                    mc.addScheduledTask(() -> {
                        if (!SESSION.current(ticket) || mc.isSingleplayer()) return;
                        PACK.setAssets(assets);
                        resourcesLoaded = true;
                        loadedResourceVersion = profile.resources().version();
                        reloadBlockModels(mc);
                        LOGGER.info("Loaded block resources for Minecraft " + profile.resources().version());
                    });
                } catch (Exception error) {
                    LOGGER.log(Level.WARNING, "Could not load block resources for " + profile.resources().version(), error);
                    mc.addScheduledTask(() -> {
                        if (SESSION.current(ticket) && mc.thePlayer != null) {
                            mc.thePlayer.addChatMessage(new ChatComponentText("[ViaForge] Block textures for " + profile.resources().version()
                                    + " could not be loaded. Temporary replacement textures remain active; reconnect to retry."));
                        }
                    });
                }
            });
        });
    }

    public static void leave(Object connection) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (!SESSION.leave(connection)) return;
            com.viaversion.viaforge.items.ServerEntityViews.clear();
            if (resourcesLoaded) resetResources(mc);
        });
    }
    /** Invalidates pending downloads as well as current client state. */
    public static void unload() {
        com.viaversion.viaforge.items.ServerEntityViews.clear();
        SESSION.clear();
        if (resourcesLoaded) resetResources(Minecraft.getMinecraft());
    }

    private static void resetResources(Minecraft mc) {
        PACK.setAssets(Collections.emptyMap());
        resourcesLoaded = false;
        loadedResourceVersion = null;
        reloadBlockModels(mc);
    }

    private static void reloadBlockModels(Minecraft mc) {
        // The pack object is already in the resource manager; only its contents
        // changed. Rebuild atlas consumers without restarting 1.8's asynchronous
        // sound engine on every join/leave (rapid reloads can break OpenAL).
        net.minecraft.client.resources.IResourceManager resources = mc.getResourceManager();
        if (mc.theWorld != null) {
            // Drain chunk compilation workers before replacing their models and
            // atlas. New chunks are submitted only after this main-thread task.
            mc.renderGlobal.loadRenderers();
            mc.effectRenderer.clearEffects(mc.theWorld);
        }
        mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().onResourceManagerReload(resources);
        mc.getBlockRendererDispatcher().onResourceManagerReload(resources);
        mc.getRenderItem().onResourceManagerReload(resources);
        mc.renderGlobal.onResourceManagerReload(resources);
        // Standalone entity/HUD textures are not part of the rebuilt item atlas.
        // Evict them so the next bind uploads this connection's version. Merely
        // deleting the GL texture leaves TextureManager holding the old object.
        java.util.Iterator<net.minecraft.util.ResourceLocation> textures =
                ((com.viaversion.viaforge.mixin.impl.blocks.VersionTextureCache)mc.getTextureManager()).viaForge$textures().keySet().iterator();
        while (textures.hasNext()) {
            net.minecraft.util.ResourceLocation texture = textures.next();
            if (texture.getResourceDomain().equals("viaforge") && texture.getResourcePath().startsWith("textures/")
                    || texture.getResourceDomain().equals("minecraft") && (texture.getResourcePath().startsWith("textures/entity/") || texture.getResourcePath().startsWith("textures/models/armor/"))) {
                mc.getTextureManager().deleteTexture(texture);
                textures.remove();
            }
        }
        net.minecraft.util.ResourceLocation particles = new net.minecraft.util.ResourceLocation("textures/particle/particles.png");
        mc.getTextureManager().deleteTexture(particles);
        mc.getTextureManager().loadTexture(particles, new net.minecraft.client.renderer.texture.SimpleTexture(particles));
        com.viaversion.viaforge.items.ServerTotemSound.reload();
        com.viaversion.viaforge.mobs.ServerMobSounds.reload();
    }
}
