package com.viaversion.viaforge.blocks.resources;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Resolves 1.21.4 item definitions to their original default geometry. The
 * existing shield, bow, rod and Elytra renderers select their action variants. */
public final class ItemDefinitionResourceConverter {
    public static Map<String,byte[]> convert(Map<String,byte[]> source) {
        Map<String,byte[]> assets=new HashMap<>(source);
        for(Map.Entry<String,byte[]> entry:source.entrySet()) {
            String path=entry.getKey();if(!path.startsWith("items/")||!path.endsWith(".json"))continue;
            JsonObject definition=new JsonParser().parse(new String(entry.getValue(),StandardCharsets.UTF_8)).getAsJsonObject();
            restoreSpecialModels(definition.get("model"),assets);
            String target="models/item/"+path.substring(6);if(assets.containsKey(target))continue;
            String model=defaultModel(definition.getAsJsonObject("model"));if(model==null)continue;
            if(model.startsWith("minecraft:"))model=model.substring(10);else if(model.contains(":"))continue;
            if(!source.containsKey("models/"+model+".json"))throw new IllegalArgumentException("Missing original item model "+model);
            JsonObject alias=new JsonObject();alias.addProperty("parent","minecraft:"+model);
            assets.put(target,alias.toString().getBytes(StandardCharsets.UTF_8));
        }
        return assets;
    }
    private static void restoreSpecialModels(JsonElement node,Map<String,byte[]> assets) {
        if(node==null)return;
        if(node.isJsonArray()){for(JsonElement child:node.getAsJsonArray())restoreSpecialModels(child,assets);return;}
        if(!node.isJsonObject())return;
        JsonObject object=node.getAsJsonObject();
        if(object.has("type")&&object.get("type").getAsString().equals("minecraft:special")&&object.has("base")) {
            String parent=object.get("base").getAsString();
            Set<String> visited=new HashSet<>();
            while(visited.add(parent)&&visited.size()<=32) {
                parent=parent.replace("minecraft:","");String path="models/"+parent+".json";
                byte[] bytes=assets.get(path);if(bytes==null)break;
                JsonObject model=new JsonParser().parse(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
                if(model.has("parent")){parent=model.get("parent").getAsString();continue;}
                // Rendering moved from builtin/entity to the item's special definition.
                if(!model.has("elements")){model.addProperty("parent","builtin/entity");assets.put(path,model.toString().getBytes(StandardCharsets.UTF_8));}
                break;
            }
        }
        for(Map.Entry<String,JsonElement> child:object.entrySet())restoreSpecialModels(child.getValue(),assets);
    }
    private static String defaultModel(JsonObject node) {
        if(node==null||!node.has("type"))return null;
        String type=node.get("type").getAsString().replace("minecraft:","");
        if(type.equals("model"))return node.get("model").getAsString();
        if(type.equals("special"))return node.get("base").getAsString();
        if(type.equals("condition"))return defaultModel(node.getAsJsonObject("on_false"));
        if(type.equals("range_dispatch")||type.equals("select"))return defaultModel(node.getAsJsonObject("fallback"));
        return null;
    }
    private ItemDefinitionResourceConverter() { }
}
