package com.viaversion.viaforge.blocks;

import com.google.gson.*;
import com.viaversion.viaforge.blocks.resources.LegacyBlockModels;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyBlockModelsTest {
    @Test public void expandsChorusConnectionsWithResolvableTextureSlots() throws Exception {
        Map<String, byte[]> assets = new HashMap<>();
        put(assets, "blockstates/chorus_plant.json", "{\"multipart\":[{\"when\":{\"north\":true},\"apply\":{\"model\":\"side\"}},{\"when\":{\"east\":true},\"apply\":{\"model\":\"side\",\"y\":90}}]}");
        put(assets, "models/block/side.json", "{\"textures\":{\"side\":\"blocks/chorus_plant\"},\"elements\":[{\"from\":[4,4,0],\"to\":[12,12,8],\"faces\":{\"north\":{\"texture\":\"#side\",\"uv\":[0,0,8,8]}}}]}");
        Map<String, byte[]> models = LegacyBlockModels.generate(assets);
        JsonObject variants = parse(models.get("blockstates/chorus_plant.json")).getAsJsonObject("variants");
        assertEquals(64, variants.entrySet().size());
        String both = "down=false,east=true,north=true,south=false,up=false,west=false";
        String modelName = variants.getAsJsonObject(both).get("model").getAsString().substring("viaforge:".length());
        JsonObject combined = parse(models.get("models/block/" + modelName + ".json"));
        assertEquals(2, combined.getAsJsonArray("elements").size());
        JsonObject north = combined.getAsJsonArray("elements").get(0).getAsJsonObject();
        JsonObject east = combined.getAsJsonArray("elements").get(1).getAsJsonObject();
        assertTrue(north.getAsJsonObject("faces").has("north"));
        assertTrue(east.getAsJsonObject("faces").has("east"));
        assertEquals(8, east.getAsJsonArray("from").get(0).getAsInt());
        assertEquals(16, east.getAsJsonArray("to").get(0).getAsInt());
        for (JsonElement element : combined.getAsJsonArray("elements")) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                String texture = entry.getValue().getAsJsonObject().get("texture").getAsString();
                assertTrue(texture.startsWith("#"));
                assertEquals("viaforge:blocks/chorus_plant", combined.getAsJsonObject("textures").get(texture.substring(1)).getAsString());
            }
        }
    }
    private static JsonObject parse(byte[] data) { return new JsonParser().parse(new String(data, StandardCharsets.UTF_8)).getAsJsonObject(); }
    private static void put(Map<String, byte[]> target, String key, String value) { target.put(key, value.getBytes(StandardCharsets.UTF_8)); }
}
