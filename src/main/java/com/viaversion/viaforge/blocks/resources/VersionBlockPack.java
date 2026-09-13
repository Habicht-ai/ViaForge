package com.viaversion.viaforge.blocks.resources;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

/** Target textures overlay vanilla; converted block and item models use a private namespace. */
public final class VersionBlockPack implements IResourcePack {
    private static final Set<String> DOMAINS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("minecraft", "viaforge")));
    private Map<String, byte[]> assets = Collections.emptyMap();
    private Map<String, byte[]> generated;

    public VersionBlockPack() {
        try { generated = generate(assets); }
        catch (IOException impossible) { throw new IllegalStateException("Invalid fallback models", impossible); }
    }

    public void setAssets(Map<String, byte[]> assets) {
        try {
            Map<String, byte[]> models = generate(assets);
            this.assets = assets;
            this.generated = models;
        } catch (IOException error) { throw new IllegalArgumentException("Could not convert server block models", error); }
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        String path = location.getResourcePath();
        if (location.getResourceDomain().equals("minecraft")) {
            return (path.startsWith("textures/blocks/") || path.startsWith("textures/items/") || path.startsWith("textures/entity/")
                    || path.startsWith("textures/models/armor/") || path.equals("textures/particle/particles.png")) && assets.containsKey(path);
        }
        if (!location.getResourceDomain().equals("viaforge")) return false;
        return generated.containsKey(path) || (path.startsWith("models/") || path.startsWith("textures/") || path.startsWith("sounds/") || path.equals("mob_sounds.json")) && assets.containsKey(path);
    }

    private static Map<String, byte[]> generate(Map<String, byte[]> assets) throws IOException {
        Map<String, byte[]> models = new java.util.HashMap<>(LegacyBlockModels.generate(assets));
        models.putAll(LegacyItemModels.generate(assets));
        return models;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (!resourceExists(location)) throw new FileNotFoundException(location.toString());
        String path = location.getResourcePath();
        if (location.getResourceDomain().equals("minecraft") || path.startsWith("textures/") || path.startsWith("sounds/") || path.equals("mob_sounds.json")) return new ByteArrayInputStream(assets.get(path));
        byte[] converted = generated.get(path);
        if (converted != null) return new ByteArrayInputStream(converted);
        return new ByteArrayInputStream(LegacyBlockModels.qualifyModel(LegacyModelConverter.flatten(path, assets)).toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override public Set<String> getResourceDomains() { return DOMAINS; }
    @Override public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer serializer, String section) { return null; }
    @Override public BufferedImage getPackImage() { return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB); }
    @Override public String getPackName() { return "ViaForge versioned blocks"; }
}
