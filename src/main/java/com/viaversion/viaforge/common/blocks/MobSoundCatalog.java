package com.viaversion.viaforge.common.blocks;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Mojang's numeric sound registry order, including the extra event added in 1.9.1. */
public final class MobSoundCatalog {
    private static final Map<String,List<String>> REGISTRIES = new HashMap<>();
    public static synchronized String name(String version, int id) {
        List<String> names = REGISTRIES.get(version);
        if (names == null) {
            names = new ArrayList<>();
            try (InputStream stream = MobSoundCatalog.class.getResourceAsStream("/assets/viaforge/mob-sounds/"+version+".txt")) {
                if (stream == null) return null;
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
                String line; while ((line = reader.readLine()) != null) if (!line.isEmpty()) names.add(line);
            } catch (IOException error) { throw new IllegalStateException("Cannot read sound registry",error); }
            REGISTRIES.put(version,names);
        }
        return id >= 0 && id < names.size() ? names.get(id) : null;
    }
    private MobSoundCatalog() { }
}
