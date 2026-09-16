package com.viaversion.viaforge.common.blocks;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Inherited egg identities and their resource names after entity renames. */
public final class SpawnEggNames {
    /** Local appearance retained before flattening; the item snapshot removes it on return. */
    public static final String CLIENT_ID = "ViaForge|egg";
    public static final Map<String,String> MODERN;
    static {
        Map<String,String> names=new LinkedHashMap<>();
        try(Reader reader=new InputStreamReader(SpawnEggNames.class.getResourceAsStream("/assets/viaforge/item-variants.json"),StandardCharsets.UTF_8)) {
            for(JsonElement entry:new JsonParser().parse(reader).getAsJsonObject().getAsJsonArray("eggs")) {
                String name=entry.getAsJsonObject().get("name").getAsString();
                String modern=name.equals("evocation_illager")?"evoker":name.equals("vindication_illager")?"vindicator":name.equals("zombie_pigman")?"zombified_piglin":name;
                names.put(name,modern);
            }
        }catch(IOException error){throw new ExceptionInInitializerError(error);}
        MODERN=Collections.unmodifiableMap(names);
    }
    private SpawnEggNames() { }
}
