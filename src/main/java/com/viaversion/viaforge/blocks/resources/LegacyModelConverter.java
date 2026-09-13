package com.viaversion.viaforge.blocks.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 1.8 rejects models containing both elements and parent, unlike 1.9+. */
public final class LegacyModelConverter {
    private LegacyModelConverter() { }

    public static JsonObject flatten(String path, Map<String, byte[]> assets) throws IOException {
        JsonObject model = flatten(path, assets, new HashSet<String>());
        // 1.8's texture resolver also rejects aliases that loop back into the same
        // flattened model. Resolve the aliases after all child overrides are merged.
        if (model.has("textures")) {
            JsonObject textures = model.getAsJsonObject("textures");
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                String value = entry.getValue().getAsString();
                Set<String> visited = new HashSet<>();
                while (value.startsWith("#")) {
                    String name = value.substring(1);
                    if (!visited.add(name) || !textures.has(name)) throw new IOException("Unresolved model texture " + value + " in " + path);
                    value = textures.get(name).getAsString();
                }
                entry.setValue(new com.google.gson.JsonPrimitive(value));
            }
        }
        adaptDisplayTransforms(model);
        return model;
    }

    /** Newer models rotate X/Y/Z; the 1.8 camera applies Y/X/Z instead. */
    public static void adaptDisplayTransforms(JsonObject model) {
        if (!model.has("display")) return;
        JsonObject display = model.getAsJsonObject("display");
        for (Map.Entry<String, JsonElement> entry : display.entrySet()) {
            JsonObject transform = entry.getValue().getAsJsonObject();
            if (!transform.has("rotation")) continue;
            JsonArray rotation = transform.getAsJsonArray("rotation");
            double x = Math.toRadians(rotation.get(0).getAsDouble());
            double y = Math.toRadians(rotation.get(1).getAsDouble());
            double z = Math.toRadians(rotation.get(2).getAsDouble());
            double legacyX = Math.asin(Math.max(-1, Math.min(1, Math.sin(x) * Math.cos(y))));
            double legacyY = Math.atan2(Math.sin(y), Math.cos(x) * Math.cos(y));
            double legacyZ = Math.atan2(Math.cos(x) * Math.sin(z) + Math.sin(x) * Math.sin(y) * Math.cos(z),
                    Math.cos(x) * Math.cos(z) - Math.sin(x) * Math.sin(y) * Math.sin(z));
            if (Math.abs(Math.cos(legacyX)) < 1E-7) {
                legacyY = 0;
                legacyZ = Math.atan2(Math.cos(y) * Math.sin(z), Math.cos(y) * Math.cos(z));
            }
            JsonArray converted = new JsonArray();
            for (double angle : new double[]{legacyX, legacyY, legacyZ}) converted.add(new JsonPrimitive(Math.toDegrees(angle)));
            transform.add("rotation", converted);
        }
        if (display.has("firstperson_righthand")) display.add("firstperson", display.get("firstperson_righthand"));
        if (display.has("thirdperson_righthand")) display.add("thirdperson", display.get("thirdperson_righthand"));
    }

    private static JsonObject flatten(String path, Map<String, byte[]> assets, Set<String> chain) throws IOException {
        if (!chain.add(path) || chain.size() > 32) throw new IOException("Cyclic or excessive model inheritance: " + path);
        byte[] bytes = assets.get(path);
        if (bytes == null) throw new IOException("Missing parent model: " + path);
        JsonObject model = new JsonParser().parse(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject result = new JsonObject();
        if (model.has("parent")) {
            String parent = model.get("parent").getAsString();
            if (parent.startsWith("minecraft:")) parent = parent.substring(10);
            if (parent.equals("builtin/generated")) result.addProperty("parent", parent);
            else {
                if (parent.contains(":") || parent.startsWith("builtin/")) throw new IOException("Unsupported block model parent: " + parent);
                result = flatten("models/" + parent + ".json", assets, chain);
            }
        }
        for (Map.Entry<String, JsonElement> entry : model.entrySet()) {
            String key = entry.getKey();
            if (key.equals("parent")) continue;
            if ((key.equals("textures") || key.equals("display")) && result.has(key)) {
                JsonObject inherited = result.getAsJsonObject(key);
                for (Map.Entry<String, JsonElement> value : entry.getValue().getAsJsonObject().entrySet()) {
                    inherited.add(value.getKey(), value.getValue());
                }
            } else result.add(key, entry.getValue());
        }
        chain.remove(path);
        return result;
    }
}
