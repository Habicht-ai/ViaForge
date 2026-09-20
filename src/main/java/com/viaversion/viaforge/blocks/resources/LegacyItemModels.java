package com.viaversion.viaforge.blocks.resources;

import com.google.gson.*;
import com.viaversion.viaforge.common.blocks.LegacyItemCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Resolves target item inheritance, including the display transforms moved in 1.9. */
public final class LegacyItemModels {
    public static Map<String, byte[]> generate(Map<String, byte[]> assets) throws IOException {
        Map<String, byte[]> models = new HashMap<>();
        Set<String> names = new HashSet<>(Arrays.asList("shield_blocking", "broken_elytra", "fireworks"));
        for(String egg:com.viaversion.viaforge.common.blocks.SpawnEggNames.MODERN.keySet())names.add("egg/"+egg);
        for (LegacyItemCatalog.Definition item : LegacyItemCatalog.ITEMS) names.add(item.model);
        for (String material : new String[]{"wooden", "stone", "iron", "diamond", "golden"}) {
            for (String tool : new String[]{"sword", "axe", "pickaxe", "shovel", "hoe"}) names.add(material + "_" + tool);
        }
        for (String name : names) {
            String path = "models/item/" + name + ".json";
            JsonObject model;
            if (assets.containsKey(path)) model = LegacyBlockModels.qualifyModel(LegacyModelConverter.flatten(path, assets));
            else {
                model = new JsonObject(); model.addProperty("parent", "builtin/generated");
                JsonObject textures = new JsonObject(); textures.addProperty("layer0", "minecraft:items/paper"); model.add("textures", textures);
            }
            models.put(path, model.toString().getBytes(StandardCharsets.UTF_8));
            if (name.equals("fireworks") || name.endsWith("_sword") || name.endsWith("_axe") || name.endsWith("_pickaxe") || name.endsWith("_shovel") || name.endsWith("_hoe")) {
                models.put("models/item/combat/" + name + ".json", model.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (name.equals("shield") || name.equals("shield_blocking")) {
                JsonObject mirrored = new JsonParser().parse(model.toString()).getAsJsonObject();
                JsonObject display = mirrored.has("display") ? mirrored.getAsJsonObject("display") : new JsonObject();
                if (display.has("thirdperson_lefthand")) {
                    JsonObject transform = display.getAsJsonObject("thirdperson_lefthand");
                    JsonArray rotation = transform.getAsJsonArray("rotation");
                    if (rotation != null) transform.add("rotation", reflected(rotation, 1, -1, -1));
                    JsonArray translation = transform.getAsJsonArray("translation");
                    if (translation != null) transform.add("translation", reflected(translation, -1, 1, 1));
                    display.add("thirdperson", transform);
                }
                models.put("models/item/" + name + "_left.json", mirrored.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        return models;
    }
    private static JsonArray reflected(JsonArray vector, int x, int y, int z) {
        JsonArray result = new JsonArray();
        int[] signs = {x, y, z};
        for (int i = 0; i < 3; i++) result.add(new JsonPrimitive(vector.get(i).getAsFloat() * signs[i]));
        return result;
    }
    private LegacyItemModels() { }
}
