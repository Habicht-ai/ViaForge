package com.viaversion.viaforge.blocks.resources;

import com.google.gson.*;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Kind;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Expands newer blockstates into definitions understood by the 1.8 model loader. */
public final class LegacyBlockModels {
    private static final String[] DIRECTIONS = {"down", "up", "north", "south", "west", "east"};
    private static final String[] HORIZONTAL = {"south", "west", "north", "east"};

    public static Map<String, byte[]> generate(Map<String, byte[]> assets) throws IOException {
        Map<String, byte[]> generated = new HashMap<>();
        for (Definition block : LegacyBlockCatalog.BLOCKS) {
            JsonObject source = read(assets, "blockstates/" + block.name + ".json");
            JsonObject variants = new JsonObject();
            for (String key : keys(block)) {
                JsonElement variant = source != null && source.has("variants") ? source.getAsJsonObject("variants").get(key) : null;
                if (variant == null && source != null && source.has("variants")) variant = source.getAsJsonObject("variants").get("normal");
                if (block.kind == Kind.BED) {
                    boolean head = key.contains("part=head");
                    String name = block.name + (head ? "_head" : "_foot");
                    put(generated, "models/block/" + name + ".json", bed(block, head, false, assets));
                    variant = model("viaforge:" + name);
                    String facing = key.substring(7, key.indexOf(','));
                    int rotation = facing.equals("east") ? 90 : facing.equals("south") ? 180 : facing.equals("west") ? 270 : 0;
                    variant.getAsJsonObject().addProperty("y", rotation);
                } else if (block.kind == Kind.CHORUS && source != null && source.has("multipart")) {
                    String name = "chorus_" + variants.entrySet().size();
                    put(generated, "models/block/" + name + ".json", multipart(source.getAsJsonArray("multipart"), key, assets));
                    variant = model("viaforge:" + name);
                } else if (block.kind == Kind.SHULKER) {
                    String name = block.name + "_closed";
                    put(generated, "models/block/" + name + ".json", shulker(block, assets));
                    variant = oriented("viaforge:" + name, key.substring(7));
                } else if (block.kind == Kind.GATEWAY || block.kind == Kind.VOID) {
                    String name = block.name + "_display";
                    JsonObject visual = cube(assets.containsKey("textures/entity/end_portal.png") ? "viaforge:entity/end_portal" : "minecraft:blocks/obsidian");
                    if (block.kind == Kind.VOID) {
                        JsonObject empty = visual.getAsJsonArray("elements").get(0).getAsJsonObject();
                        empty.add("to", numbers(0, 0, 0));
                        JsonObject faces = new JsonObject(); faces.add("north", face(0, 0, 0, 0)); empty.add("faces", faces);
                    }
                    put(generated, "models/block/" + name + ".json", visual);
                    variant = model("viaforge:" + name);
                } else if (variant != null) variant = qualifyModels(variant);
                else {
                    String name = defaultModel(block);
                    if (assets.containsKey("models/block/" + name + ".json")) variant = model("viaforge:" + name);
                    else {
                        String fallback = block.name + "_fallback_" + variants.entrySet().size();
                        put(generated, "models/block/" + fallback + ".json", fallback(block, key));
                        variant = model("viaforge:" + fallback);
                    }
                }
                variants.add(key, variant);
            }
            JsonObject definition = new JsonObject(); definition.add("variants", variants);
            put(generated, "blockstates/" + block.name + ".json", definition);
            if (block.itemId() >= 0) {
                String itemName = block.kind == Kind.CROP ? "beetroot_seeds" : block.name;
                JsonObject item;
                if (block.kind == Kind.BED) item = bed(block, true, true, assets);
                else if (block.kind == Kind.SHULKER) {
                    item = shulker(block, assets);
                    String path = "models/item/" + itemName + ".json";
                    if (assets.containsKey(path)) {
                        // Keep our baked geometry, but inherit every display context from
                        // the target's builtin/entity item model (including its parents).
                        // flatten already converts rotations into the native 1.8 order.
                        JsonObject sourceItem = LegacyModelConverter.flatten(path, assets);
                        if (sourceItem.has("display")) item.add("display", sourceItem.get("display"));
                    }
                }
                else if (block.kind == Kind.VOID && !assets.containsKey("models/item/structure_void.json")) item = cube("minecraft:items/barrier");
                else if (assets.containsKey("models/item/" + itemName + ".json")) item = qualifyModel(LegacyModelConverter.flatten("models/item/" + itemName + ".json", assets));
                else if (assets.containsKey("models/block/" + defaultModel(block) + ".json")) item = qualifyModel(LegacyModelConverter.flatten("models/block/" + defaultModel(block) + ".json", assets));
                else item = fallback(block, "normal");
                put(generated, "models/item/" + itemName + ".json", item);
            }
        }
        return Collections.unmodifiableMap(generated);
    }

    private static String defaultModel(Definition d) {
        switch (d.kind) {
            case PILLAR: return d.id == 202 ? "purpur_pillar_top" : d.name;
            case SLAB: return "half_slab_purpur";
            case DOUBLE_SLAB: return "purpur_block";
            case CROP: return "beetroots_stage0";
            case ICE: return "frosted_ice_0";
            case STRUCTURE: return "structure_block_data";
            default: return d.name;
        }
    }

    public static List<String> keys(Definition d) {
        List<String> keys = new ArrayList<>();
        if (d.kind == Kind.BED) {
            for (String face : HORIZONTAL) for (boolean occupied : new boolean[]{false, true}) for (String part : new String[]{"head", "foot"}) {
                keys.add("facing=" + face + ",occupied=" + occupied + ",part=" + part);
            }
        } else if (d.kind == Kind.STAIRS) {
            for (String face : HORIZONTAL) for (String half : new String[]{"bottom", "top"})
                for (String shape : new String[]{"straight", "inner_left", "inner_right", "outer_left", "outer_right"})
                    keys.add("facing=" + face + ",half=" + half + ",shape=" + shape);
        } else if (d.kind == Kind.CHORUS) {
            for (int mask = 0; mask < 64; mask++) {
                TreeMap<String, String> values = new TreeMap<>();
                for (int i = 0; i < 6; i++) values.put(DIRECTIONS[i], Boolean.toString((mask & 1 << i) != 0));
                keys.add(key(values));
            }
        } else for (int meta = 0; meta < 16; meta++) {
            if (!d.acceptsMetadata(meta)) continue;
            switch (d.kind) {
                case ROD: case SHULKER: keys.add("facing=" + DIRECTIONS[meta]); break;
                case FLOWER: case CROP: case ICE: keys.add("age=" + meta); break;
                case PILLAR: keys.add("axis=" + (meta == 4 ? "x" : meta == 8 ? "z" : "y")); break;
                case SLAB: keys.add("half=" + (meta == 8 ? "top" : "bottom") + ",variant=default"); break;
                case DOUBLE_SLAB: keys.add("variant=default"); break;
                case COMMAND: keys.add("conditional=" + ((meta & 8) != 0) + ",facing=" + DIRECTIONS[meta & 7]); break;
                case OBSERVER: keys.add("facing=" + DIRECTIONS[meta & 7] + ",powered=" + ((meta & 8) != 0)); break;
                case STRUCTURE: keys.add("mode=" + new String[]{"save", "load", "corner", "data"}[meta]); break;
                case GLAZED: keys.add("facing=" + HORIZONTAL[meta]); break;
                default: keys.add("normal");
            }
        }
        return keys;
    }

    private static String key(Map<String, String> values) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (result.length() > 0) result.append(',');
            result.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return result.toString();
    }
    private static JsonObject multipart(JsonArray parts, String key, Map<String, byte[]> assets) throws IOException {
        Map<String, String> values = new HashMap<>();
        for (String entry : key.split(",")) { String[] pair = entry.split("="); values.put(pair[0], pair[1]); }
        JsonObject result = new JsonObject();
        JsonObject textures = new JsonObject(); textures.addProperty("particle", "viaforge:blocks/chorus_plant"); result.add("textures", textures);
        JsonArray elements = new JsonArray(); result.add("elements", elements);
        for (JsonElement raw : parts) {
            JsonObject part = raw.getAsJsonObject();
            boolean matches = true;
            if (part.has("when")) for (Map.Entry<String, JsonElement> entry : part.getAsJsonObject("when").entrySet()) {
                if (!entry.getValue().getAsString().equals(values.get(entry.getKey()))) matches = false;
            }
            if (!matches) continue;
            JsonElement apply = part.get("apply");
            if (apply.isJsonArray()) apply = apply.getAsJsonArray().get(0);
            JsonObject transform = apply.getAsJsonObject();
            String name = transform.get("model").getAsString().replace("minecraft:", "");
            JsonObject model = qualifyModel(LegacyModelConverter.flatten("models/block/" + name + ".json", assets));
            int x = transform.has("x") ? transform.get("x").getAsInt() : 0;
            int y = transform.has("y") ? transform.get("y").getAsInt() : 0;
            for (JsonElement element : model.getAsJsonArray("elements")) {
                JsonObject box = element.getAsJsonObject();
                // Resolve texture slots before combining independent models.
                for (Map.Entry<String, JsonElement> face : box.getAsJsonObject("faces").entrySet()) {
                    JsonObject f = face.getValue().getAsJsonObject();
                    String texture = f.get("texture").getAsString();
                    if (texture.startsWith("#")) texture = model.getAsJsonObject("textures").get(texture.substring(1)).getAsString();
                    // 1.8 face textures must reference a model texture slot. A direct
                    // resource location here is interpreted as a missing slot name.
                    String slot = "part_" + textures.entrySet().size();
                    textures.addProperty(slot, texture);
                    f.addProperty("texture", "#" + slot);
                }
                rotateBox(box, x, y);
                elements.add(box);
            }
        }
        return result;
    }

    private static void rotateBox(JsonObject box, int x, int y) {
        double[] from = vector(box.getAsJsonArray("from")), to = vector(box.getAsJsonArray("to"));
        from = rotate(from, x, y, 8); to = rotate(to, x, y, 8);
        box.add("from", numbers(Math.min(from[0], to[0]), Math.min(from[1], to[1]), Math.min(from[2], to[2])));
        box.add("to", numbers(Math.max(from[0], to[0]), Math.max(from[1], to[1]), Math.max(from[2], to[2])));
        JsonObject faces = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : box.getAsJsonObject("faces").entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            if (face.has("cullface")) face.addProperty("cullface", rotateFace(face.get("cullface").getAsString(), x, y));
            faces.add(rotateFace(entry.getKey(), x, y), face);
        }
        box.add("faces", faces);
    }
    private static String rotateFace(String face, int x, int y) {
        double[] v = new double[]{face.equals("east") ? 1 : face.equals("west") ? -1 : 0,
                face.equals("up") ? 1 : face.equals("down") ? -1 : 0, face.equals("south") ? 1 : face.equals("north") ? -1 : 0};
        v = rotate(v, x, y, 0);
        return v[0] > .5 ? "east" : v[0] < -.5 ? "west" : v[1] > .5 ? "up" : v[1] < -.5 ? "down" : v[2] > .5 ? "south" : "north";
    }
    private static double[] rotate(double[] v, int x, int y, double center) {
        double a = v[0] - center, b = v[1] - center, c = v[2] - center;
        for (int i = 0; i < x / 90; i++) { double t = b; b = c; c = -t; }
        for (int i = 0; i < y / 90; i++) { double t = a; a = -c; c = t; }
        return new double[]{a + center, b + center, c + center};
    }
    private static double[] vector(JsonArray array) { return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()}; }

    private static JsonObject fallback(Definition block, String key) {
        JsonObject model = cube("minecraft:blocks/quartz_block_side");
        if (block.kind == Kind.SLAB) {
            JsonObject box = model.getAsJsonArray("elements").get(0).getAsJsonObject();
            box.add("from", numbers(0, key.contains("top") ? 8 : 0, 0)); box.add("to", numbers(16, key.contains("top") ? 16 : 8, 16));
        } else if (block.kind == Kind.ROD) {
            JsonObject box = model.getAsJsonArray("elements").get(0).getAsJsonObject(); box.add("from", numbers(6, 0, 6)); box.add("to", numbers(10, 16, 10));
        }
        return model;
    }
    private static JsonObject shulker(Definition block, Map<String, byte[]> assets) {
        String texture = "textures/entity/shulker/shulker_" + LegacyBlockCatalog.COLORS[block.id - 219] + ".png";
        if (!assets.containsKey(texture)) return cube("minecraft:blocks/quartz_block_side");
        JsonObject model = cube("viaforge:" + texture.substring(9, texture.length() - 4));
        JsonArray elements = new JsonArray();
        // Vanilla's bottom is 16x8x16 at UV (0,28), its lid 16x12x16 at (0,0).
        elements.add(shulkerPart(0, 0, 16, 8, 28, 8));
        elements.add(shulkerPart(0, 4, 16, 16, 0, 12));
        model.add("elements", elements);
        return model;
    }

    private static JsonObject bed(Definition block, boolean head, boolean item, Map<String, byte[]> assets) throws IOException {
        String texture = "textures/entity/bed/" + LegacyBlockCatalog.COLORS[block.color] + ".png";
        JsonObject model = cube(assets.containsKey(texture) ? "viaforge:" + texture.substring(9, texture.length() - 4) : "minecraft:blocks/planks_oak");
        JsonArray elements = new JsonArray();
        // The native item renderer draws a south-facing head at z=0 and foot at z=-16.
        // Keep that origin: bed.json's display translations already account for it.
        addBedHalf(elements, head, item, 0);
        if (item) addBedHalf(elements, false, true, -16);
        model.add("elements", elements);
        if (item) {
            JsonObject source = read(assets, "models/item/bed.json");
            if (source != null && source.has("display")) {
                LegacyModelConverter.adaptDisplayTransforms(source);
                model.add("display", source.get("display"));
            }
        }
        return model;
    }

    /** Vanilla's 64x64 bed texture: two 16x16x6 mattresses and four 3x3x3 legs. */
    private static void addBedHalf(JsonArray elements, boolean head, boolean item, int z) {
        int v = head ? 0 : 22;
        JsonArray parts = new JsonArray();
        JsonObject body = new JsonObject(); body.add("from", numbers(0, 3, 0)); body.add("to", numbers(16, 9, 16));
        JsonObject faces = new JsonObject();
        // ModelBed uses a 16x16x6 ModelBox, rotated +90 degrees around X.
        // Carry the UV corners through that rotation, including reversed endpoints.
        faces.add("up", bedFace(6, v + 6, 22, v + 22, 0));
        faces.add("down", bedFace(44, v + 22, 28, v + 6, 0));
        faces.add("north", bedFace(22, v + 6, 6, v, 0));
        faces.add("south", bedFace(22, v + 6, 38, v, 0));
        faces.add("west", bedFace(6, v + 22, 0, v + 6, 90));
        faces.add("east", bedFace(22, v + 6, 28, v + 22, 90));
        body.add("faces", faces); parts.add(body);
        for (int side = 0; side < 2; side++) {
            int uvY = (head ? 6 : 0) + side * 12;
            JsonObject leg = new JsonObject(); leg.add("from", numbers(0, 0, 13)); leg.add("to", numbers(3, 3, 16));
            JsonObject legFaces = new JsonObject();
            legFaces.add("up", bedFace(53, uvY, 56, uvY + 3, 0));
            legFaces.add("down", bedFace(56, uvY + 3, 59, uvY, 0));
            legFaces.add("west", bedFace(50, uvY + 3, 53, uvY + 6, 0));
            legFaces.add("south", bedFace(53, uvY + 3, 56, uvY + 6, 0));
            legFaces.add("east", bedFace(56, uvY + 3, 59, uvY + 6, 0));
            legFaces.add("north", bedFace(59, uvY + 3, 62, uvY + 6, 0));
            leg.add("faces", legFaces);
            rotateBedPart(leg, head ? (side == 0 ? 90 : 180) : (side == 0 ? 0 : 270));
            parts.add(leg);
        }
        for (JsonElement element : parts) {
            JsonObject part = element.getAsJsonObject();
            if (item) rotateBedPart(part, 180);
            for (String bound : new String[]{"from", "to"}) {
                double[] point = vector(part.getAsJsonArray(bound));
                part.add(bound, numbers(point[0], point[1], point[2] + z));
            }
            elements.add(part);
        }
    }
    private static void rotateBedPart(JsonObject part, int y) {
        rotateBox(part, 0, y);
        // Horizontal faces move with the box; top/bottom also turn within their planes.
        for (String direction : new String[]{"up", "down"}) {
            JsonObject face = part.getAsJsonObject("faces").getAsJsonObject(direction);
            int rotation = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
            face.addProperty("rotation", (rotation + (direction.equals("up") ? y : -y) + 360) % 360);
        }
    }
    private static JsonObject bedFace(double x, double y, double xx, double yy, int rotation) {
        JsonObject result = face(x / 4, y / 4, xx / 4, yy / 4);
        if (rotation != 0) result.addProperty("rotation", rotation);
        return result;
    }
    private static JsonObject shulkerPart(int x0, int y0, int x1, int y1, int v, int h) {
        JsonObject box = new JsonObject(); box.add("from", numbers(x0, y0, 0)); box.add("to", numbers(x1, y1, 16));
        JsonObject faces = new JsonObject();
        faces.add("down", face(8, (v + 16) / 4.0, 12, v / 4.0));
        faces.add("up", face(4, v / 4.0, 8, (v + 16) / 4.0));
        faces.add("west", face(0, (v + 16) / 4.0, 4, (v + 16 + h) / 4.0));
        faces.add("north", face(4, (v + 16) / 4.0, 8, (v + 16 + h) / 4.0));
        faces.add("east", face(8, (v + 16) / 4.0, 12, (v + 16 + h) / 4.0));
        faces.add("south", face(12, (v + 16) / 4.0, 16, (v + 16 + h) / 4.0));
        box.add("faces", faces); return box;
    }
    private static JsonObject cube(String texture) {
        JsonObject result = new JsonObject(); JsonObject textures = new JsonObject(); textures.addProperty("all", texture); textures.addProperty("particle", texture); result.add("textures", textures);
        JsonObject box = new JsonObject(); box.add("from", numbers(0, 0, 0)); box.add("to", numbers(16, 16, 16));
        JsonObject faces = new JsonObject(); for (String direction : DIRECTIONS) faces.add(direction, face(0, 0, 16, 16)); box.add("faces", faces);
        JsonArray elements = new JsonArray(); elements.add(box); result.add("elements", elements);
        JsonObject display = new JsonObject(); JsonObject gui = new JsonObject(); gui.add("rotation", numbers(30, 225, 0)); gui.add("scale", numbers(.625, .625, .625)); display.add("gui", gui); result.add("display", display);
        // Generated block geometry (including shulker items) needs a hand transform
        // too; native block models inherit this from block/block.json.
        JsonObject hand = new JsonObject(); hand.add("rotation", numbers(0, 45, 0)); hand.add("scale", numbers(.4, .4, .4));
        display.add("firstperson_righthand", hand);
        LegacyModelConverter.adaptDisplayTransforms(result);
        return result;
    }
    private static JsonObject face(double a, double b, double c, double d) { JsonObject face = new JsonObject(); face.add("uv", numbers(a, b, c, d)); face.addProperty("texture", "#all"); return face; }
    private static JsonArray numbers(double... values) { JsonArray array = new JsonArray(); for (double value : values) array.add(new JsonPrimitive(value)); return array; }
    private static JsonObject oriented(String name, String facing) {
        JsonObject model = model(name);
        if (facing.equals("down")) model.addProperty("x", 180);
        else if (!facing.equals("up")) { model.addProperty("x", 90); model.addProperty("y", facing.equals("east") ? 90 : facing.equals("south") ? 180 : facing.equals("west") ? 270 : 0); }
        return model;
    }
    private static JsonObject model(String name) { JsonObject result = new JsonObject(); result.addProperty("model", name); return result; }
    private static JsonElement qualifyModels(JsonElement source) {
        JsonElement copy = new JsonParser().parse(source.toString());
        if (copy.isJsonArray()) for (JsonElement child : copy.getAsJsonArray()) qualifyVariant(child.getAsJsonObject());
        else qualifyVariant(copy.getAsJsonObject());
        return copy;
    }
    private static void qualifyVariant(JsonObject model) { model.addProperty("model", "viaforge:" + model.get("model").getAsString().replace("minecraft:", "")); }
    public static JsonObject qualifyModel(JsonObject model) {
        if (model.has("textures")) for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
            String value = entry.getValue().getAsString();
            if (!value.startsWith("#")) entry.setValue(new JsonPrimitive("viaforge:" + value.replace("minecraft:", "")));
        }
        return model;
    }
    private static JsonObject read(Map<String, byte[]> assets, String path) { byte[] data = assets.get(path); return data == null ? null : new JsonParser().parse(new String(data, StandardCharsets.UTF_8)).getAsJsonObject(); }
    private static void put(Map<String, byte[]> target, String path, JsonObject data) { target.put(path, data.toString().getBytes(StandardCharsets.UTF_8)); }
    private LegacyBlockModels() { }
}
