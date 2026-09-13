package com.viaversion.viaforge.blocks;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.viaversion.viaforge.blocks.resources.LegacyModelConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyModelConverterTest {
    @Test
    public void flattensGeometryAndTextureInheritanceForThe18Loader() throws Exception {
        Map<String, byte[]> assets = new HashMap<>();
        put(assets, "block", "{\"ambientocclusion\":false,\"textures\":{\"particle\":\"#side\"}}");
        put(assets, "stairs", "{\"parent\":\"block/block\",\"elements\":[{\"from\":[0,0,0],\"to\":[16,8,16]}],\"textures\":{\"side\":\"blocks/stone\"}}");
        put(assets, "purpur_stairs", "{\"parent\":\"minecraft:block/stairs\",\"textures\":{\"side\":\"blocks/purpur_block\"}}");
        JsonObject model = LegacyModelConverter.flatten("models/block/purpur_stairs.json", assets);
        assertFalse(model.has("parent"));
        assertEquals(1, model.getAsJsonArray("elements").size());
        assertFalse(model.get("ambientocclusion").getAsBoolean());
        assertEquals("blocks/purpur_block", model.getAsJsonObject("textures").get("particle").getAsString());
        assertEquals("blocks/purpur_block", model.getAsJsonObject("textures").get("side").getAsString());
    }

    @Test(expected = IOException.class)
    public void rejectsInheritanceCycles() throws Exception {
        Map<String, byte[]> assets = new HashMap<>();
        put(assets, "a", "{\"parent\":\"block/b\"}");
        put(assets, "b", "{\"parent\":\"block/a\"}");
        LegacyModelConverter.flatten("models/block/a.json", assets);
    }

    @Test
    public void preservesCameraOrientationAcrossLegacyRotationOrderAndInheritedHandAliases() throws Exception {
        for (double[] angles : new double[][]{{30, 225, 0}, {0, 0, -45}, {75, 45, 0},
                {35, -65, 17}, {90, 0, 25}, {-90, 180, -40}}) {
            String rotation = java.util.Arrays.toString(angles);
            Map<String, byte[]> assets = new HashMap<>();
            put(assets, "base", "{\"display\":{\"gui\":{\"rotation\":" + rotation
                    + "},\"firstperson_righthand\":{\"rotation\":" + rotation + "}}}");
            put(assets, "child", "{\"parent\":\"block/base\"}");
            JsonObject display = LegacyModelConverter.flatten("models/block/child.json", assets).getAsJsonObject("display");
            JsonArray converted = display.getAsJsonObject("gui").getAsJsonArray("rotation");
            double[] legacy = {converted.get(0).getAsDouble(), converted.get(1).getAsDouble(), converted.get(2).getAsDouble()};
            for (double[] basis : new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}) {
                double[] expected = rotate(rotate(rotate(basis, 2, angles[2]), 1, angles[1]), 0, angles[0]);
                double[] actual = rotate(rotate(rotate(basis, 2, legacy[2]), 0, legacy[0]), 1, legacy[1]);
                assertArrayEquals("Camera orientation " + rotation, expected, actual, 1E-7);
            }
            assertEquals(converted, display.getAsJsonObject("firstperson").getAsJsonArray("rotation"));
            assertEquals(display.get("firstperson"), display.get("firstperson_righthand"));
        }
    }

    private static double[] rotate(double[] vector, int axis, double degrees) {
        double[] result = vector.clone();
        int a = (axis + 1) % 3, b = (axis + 2) % 3;
        double radians = Math.toRadians(degrees);
        result[a] = Math.cos(radians) * vector[a] - Math.sin(radians) * vector[b];
        result[b] = Math.sin(radians) * vector[a] + Math.cos(radians) * vector[b];
        return result;
    }

    private static void put(Map<String, byte[]> assets, String name, String json) {
        assets.put("models/block/" + name + ".json", json.getBytes(StandardCharsets.UTF_8));
    }
}
