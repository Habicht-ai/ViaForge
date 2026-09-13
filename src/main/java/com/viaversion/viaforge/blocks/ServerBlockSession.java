package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.blocks.resources.BlockAssetCache;
import com.viaversion.viaforge.blocks.resources.VersionBlockPack;
import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
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
public final class ServerBlockSession {
    private static final Logger LOGGER = Logger.getLogger("ViaForge/Blocks");
    private static final VersionBlockPack PACK = new VersionBlockPack();
    private static final ExecutorService DOWNLOADS = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ViaForge block resources");
        thread.setDaemon(true);
        return thread;
    });
    private static Object activeConnection;
    private static boolean resourcesLoaded;
    private static String loadedResourceVersion;
    private static BlockVersionProfile activeProfile;

    private ServerBlockSession() { }

    public static String getLoadedResourceVersion() { return loadedResourceVersion; }
    public static boolean supportsItem(com.viaversion.viaforge.common.blocks.LegacyItemDefinition item) {
        return item != null && supportsProtocol(item.itemProtocol());
    }
    public static boolean supportsProtocol(int protocol) {
        return activeProfile != null && activeProfile.protocol() >= protocol && !Minecraft.getMinecraft().isSingleplayer();
    }
    public static boolean supports(com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition block) {
        return block != null && activeProfile != null && activeProfile.protocol() >= block.protocol
                && !Minecraft.getMinecraft().isSingleplayer();
    }
    public static boolean supportsItem(com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition block) {
        return supports(block) && activeProfile.protocol() >= block.itemProtocol();
    }

    public static void initialize() {
        Minecraft mc = Minecraft.getMinecraft();
        ((MinecraftResourcePacks) mc).viaForge$defaultResourcePacks().add(PACK);
        // Forge first builds block models before the next full resource reload.
        // Expose our fallback definitions to that initial bake as well.
        ((net.minecraft.client.resources.SimpleReloadableResourceManager) mc.getResourceManager()).reloadResourcePack(PACK);
    }

    public static void join(Object connection, BlockVersionProfile profile) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.isSingleplayer()) return;
            activeConnection = connection;
            activeProfile = profile;
            if (resourcesLoaded) resetResources(mc);
            DOWNLOADS.execute(() -> {
                try {
                    BlockAssetCache cache = new BlockAssetCache(mc.mcDataDir.toPath().resolve("ViaForge/block-assets"));
                    Map<String, byte[]> assets = cache.load(profile.resourceVersion());
                    mc.addScheduledTask(() -> {
                        if (activeConnection != connection || mc.isSingleplayer()) return;
                        PACK.setAssets(assets);
                        resourcesLoaded = true;
                        reloadBlockModels(mc);
                        loadedResourceVersion = profile.resourceVersion();
                        LOGGER.info("Loaded block resources for Minecraft " + profile.resourceVersion());
                    });
                } catch (Exception error) {
                    LOGGER.log(Level.WARNING, "Could not load block resources for " + profile.resourceVersion(), error);
                    mc.addScheduledTask(() -> {
                        if (activeConnection == connection && mc.thePlayer != null) {
                            mc.thePlayer.addChatMessage(new ChatComponentText("[ViaForge] Block textures for " + profile.resourceVersion()
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
            if (activeConnection != connection) return;
            activeConnection = null;
            activeProfile = null;
            if (resourcesLoaded) resetResources(mc);
        });
    }

    /** Called before loading any disconnected or integrated world, including fast server switches. */
    public static void unload() {
        com.viaversion.viaforge.items.ServerEntityViews.clear();
        activeConnection = null;
        activeProfile = null;
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
        net.minecraft.util.ResourceLocation particles = new net.minecraft.util.ResourceLocation("textures/particle/particles.png");
        mc.getTextureManager().deleteTexture(particles);
        mc.getTextureManager().loadTexture(particles, new net.minecraft.client.renderer.texture.SimpleTexture(particles));
        com.viaversion.viaforge.items.ServerTotemSound.reload();
    }
}
