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

/** All session/resource changes happen on Minecraft's main thread. */
public final class ServerSession {
    private static final Logger LOGGER = Logger.getLogger("ViaForge/Blocks");
    private static final VersionBlockPack PACK = new VersionBlockPack();
    private static final ExecutorService DOWNLOADS = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "ViaForge block resources");
        thread.setDaemon(true);
        return thread;
    });
    private static final SessionEpoch SESSION = new SessionEpoch();
    private static boolean resourcesLoaded;
    private static boolean resourcesInstalled;
    private static String loadedResourceVersion;
    private static Throwable resourceFailure;
    private static volatile WaterColors waterColors;
    private static long resourceGeneration;
    private static final Map<String, java.util.concurrent.CompletableFuture<VersionBlockPack.Prepared>> PREPARED = new java.util.LinkedHashMap<>();

    private ServerSession() { }

    public static String getLoadedResourceVersion() { return loadedResourceVersion; }
    public static Throwable resourceFailure() { return resourceFailure; }
    public static WaterColors waterColors() { return waterColors; }
    public static long resourceGeneration() { return resourceGeneration; }
    public static boolean awaitingResources() { return profile().extended() && !resourcesLoaded; }
    public static boolean awaitingWorld(net.minecraft.client.network.NetHandlerPlayClient handler) {
        return profile().extended() && (awaitingResources()
            || !((com.viaversion.viaforge.mixin.impl.connect.ClientTerrainReady)handler).viaForge$terrainReady());
    }
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
            waterColors=null;
            if(connection instanceof io.netty.channel.Channel) {
                com.viaversion.viaversion.api.connection.UserConnection user=((io.netty.channel.Channel)connection).attr(com.viaversion.viaforge.common.ViaForgeCommon.VF_VIA_USER).get();
                FlattenedProtocolAdapter adapter=user==null?null:user.get(FlattenedProtocolAdapter.class);
                if(adapter!=null)waterColors=adapter.waterColors;
            }
            resourceFailure=null;
            if (resourcesInstalled) resetResources(mc);
            if (!profile.extended()) return;
            prepare(profile).whenComplete((prepared,error) -> mc.addScheduledTask(() -> {
                if (!SESSION.current(ticket) || mc.isSingleplayer()) return;
                if (error != null) {
                    resourceFailure=error;
                    LOGGER.log(Level.WARNING,"Could not load resources for "+profile.resources().version(),error);
                    return;
                }
                try {
                    PACK.install(prepared);
                    resourcesInstalled=true;
                    reloadBlockModels(mc);
                    // Resource baking blocks the game thread. Do not replay its elapsed
                    // loading time as a burst of player movement in the newly loaded world.
                    net.minecraft.util.Timer timer=((com.viaversion.viaforge.mixin.impl.items.MinecraftItemTimer)mc).viaForge$itemTimer();
                    timer.updateTimer();timer.elapsedTicks=0;timer.elapsedPartialTicks=0;
                    resourcesLoaded=true;
                    loadedResourceVersion=profile.resources().version();
                    LOGGER.info("Loaded block resources for Minecraft "+loadedResourceVersion);
                } catch (Exception failure) { resourceFailure=failure;LOGGER.log(Level.WARNING,"Could not apply target resources",failure); }
            }));
        });
    }

    /** Start during server address resolution. A bounded cache reuses conversion
     * work on reconnect; only the current session ticket may install the result. */
    public static void prefetch(CompatibilityProfile profile) { if(profile.extended())prepare(profile); }
    private static synchronized java.util.concurrent.CompletableFuture<VersionBlockPack.Prepared> prepare(CompatibilityProfile profile) {
        String version=profile.resources().version();
        java.util.concurrent.CompletableFuture<VersionBlockPack.Prepared> pending=PREPARED.get(version);
        if(pending!=null&&!pending.isCompletedExceptionally())return pending;
        java.nio.file.Path cachePath=Minecraft.getMinecraft().mcDataDir.toPath().resolve("ViaForge/block-assets");
        pending=java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {return VersionBlockPack.prepare(profile.resources().normalize(new BlockAssetCache(cachePath).load(version)));}
            catch(Exception error){throw new java.util.concurrent.CompletionException(error);}
        },DOWNLOADS);
        PREPARED.put(version,pending);
        while(PREPARED.size()>2)PREPARED.remove(PREPARED.keySet().iterator().next());
        return pending;
    }

    public static void leave(Object connection) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (!SESSION.leave(connection)) return;
            waterColors=null;
            resourceFailure=null;
            com.viaversion.viaforge.items.ServerEntityViews.clear();
            if (resourcesInstalled) resetResources(mc);
        });
    }
    /** Invalidates pending downloads as well as current client state. */
    public static void unload() {
        com.viaversion.viaforge.items.ServerEntityViews.clear();
        SESSION.clear();
        waterColors=null;
        resourceFailure=null;
        if (resourcesInstalled) resetResources(Minecraft.getMinecraft());
    }

    private static void resetResources(Minecraft mc) {
        PACK.setAssets(Collections.emptyMap());
        resourcesInstalled = false;
        resourcesLoaded = false;
        loadedResourceVersion = null;
        reloadBlockModels(mc);
    }

    private static void reloadBlockModels(Minecraft mc) {
        resourceGeneration++;
        com.viaversion.viaforge.items.ServerItemRenderer.clearPatterns();
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
        Map<net.minecraft.util.ResourceLocation,net.minecraft.client.renderer.texture.ITextureObject> objects=
                ((com.viaversion.viaforge.mixin.impl.blocks.VersionTextureCache)mc.getTextureManager()).viaForge$textures();
        java.util.Iterator<net.minecraft.util.ResourceLocation> textures = objects.keySet().iterator();
        while (textures.hasNext()) {
            net.minecraft.util.ResourceLocation texture = textures.next();
            if (objects.get(texture) instanceof net.minecraft.client.renderer.texture.LayeredColorMaskTexture
                    || texture.getResourceDomain().equals("viaforge") && (texture.getResourcePath().startsWith("textures/") || texture.getResourcePath().startsWith("shield_patterns/"))
                    || texture.getResourceDomain().equals("minecraft") && (texture.getResourcePath().startsWith("textures/entity/") || texture.getResourcePath().startsWith("textures/models/armor/"))) {
                net.minecraft.client.renderer.texture.ITextureObject object=objects.get(texture);
                if(object!=net.minecraft.client.renderer.texture.TextureUtil.missingTexture && object instanceof net.minecraft.client.renderer.texture.AbstractTexture)
                    ((net.minecraft.client.renderer.texture.AbstractTexture)object).deleteGlTexture();
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
