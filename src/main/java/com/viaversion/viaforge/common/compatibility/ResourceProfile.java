package com.viaversion.viaforge.common.compatibility;

import java.util.*;
import java.util.function.UnaryOperator;

/** Exact asset release and conversion to the renderer's resource layout. */
public final class ResourceProfile {
    private final String version;
    private final UnaryOperator<Map<String,byte[]>> converter;
    public ResourceProfile(String version,UnaryOperator<Map<String,byte[]>> converter) {
        this.version=Objects.requireNonNull(version);this.converter=Objects.requireNonNull(converter);
    }
    public static ResourceProfile legacy(String version) { return new ResourceProfile(version,assets->assets); }
    public String version() { return version; }
    public Map<String,byte[]> normalize(Map<String,byte[]> assets) {
        // Must precede converters that decode and compose chest, GUI or particle textures.
        return Collections.unmodifiableMap(new HashMap<>(converter.apply(
                com.viaversion.viaforge.blocks.resources.PngTextureConverter.normalize(assets))));
    }
}
