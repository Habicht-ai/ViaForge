package com.viaversion.viaforge.blocks.resources;

import com.google.gson.*;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.common.compatibility.FlattenedBlockData;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Imports original 1.13 resources into the established client model/texture layout. */
public final class FlattenedResourceConverter {
    public static Map<String,byte[]> convert(Map<String,byte[]> source) {
        Map<String,byte[]> result=new HashMap<>();
        for(Map.Entry<String,byte[]> entry:source.entrySet()) {
            String path=entry.getKey();byte[] data=entry.getValue();
            if(path.startsWith("models/") || path.startsWith("blockstates/")) {
                JsonElement json=parse(data);rewrite(json,path.startsWith("blockstates/"));data=bytes(json);
            }
            result.put(path,data);
            if(path.startsWith("textures/block/"))result.put(path.replace("textures/block/","textures/blocks/"),data);
            if(path.startsWith("textures/item/"))result.put(path.replace("textures/item/","textures/items/"),data);
        }
        try(InputStream stream=FlattenedResourceConverter.class.getResourceAsStream("/assets/viaforge/flattened-resource-aliases.json")) {
            if(stream==null)throw new IllegalStateException("Missing flattened resource aliases");
            JsonObject aliases=new JsonParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            for(Map.Entry<String,JsonElement> alias:aliases.entrySet()) {
                String target=alias.getValue().getAsString();
                if(!source.containsKey(target))throw new IllegalArgumentException("Missing target texture "+target);
                result.put(alias.getKey(),source.get(target));
                if(source.containsKey(target+".mcmeta"))result.put(alias.getKey()+".mcmeta",source.get(target+".mcmeta"));
            }
        }catch(IOException error){throw new IllegalStateException("Cannot read flattened resource aliases",error);}
        for(LegacyBlockCatalog.Definition block:LegacyBlockCatalog.BLOCKS) {
            String modern=FlattenedBlockData.modernName(block.name);
            alias(result,"models/item/"+block.name+".json","models/item/"+modern+".json");
            String path="blockstates/"+modern+".json";
            if(!result.containsKey(path))continue;
            JsonObject states=parse(result.get(path)).getAsJsonObject();
            if(states.has("variants")) {
                JsonObject variants=new JsonObject();
                for(Map.Entry<String,JsonElement> entry:states.getAsJsonObject("variants").entrySet()) {
                    String key=entry.getKey();
                    if(key.isEmpty())key="normal";
                    if(block.kind==LegacyBlockCatalog.Kind.SLAB) {
                        if(key.equals("type=double"))continue;
                        key=key.replace("type=","half=")+",variant=default";
                    }else if(block.kind==LegacyBlockCatalog.Kind.DOUBLE_SLAB) {
                        if(!key.equals("type=double"))continue;key="variant=default";
                    }
                    variants.add(key,entry.getValue());
                }
                states.add("variants",variants);
            }
            result.put("blockstates/"+block.name+".json",bytes(states));
        }
        String[][] models={{"bottle_drinkable","potion"},{"bottle_splash","splash_potion"},{"bottle_lingering","lingering_potion"},
                {"skull_dragon","dragon_head"},{"chorus_fruit_popped","popped_chorus_fruit"},{"totem","totem_of_undying"},{"spawn_egg","template_spawn_egg"}};
        for(String[] pair:models)alias(result,"models/item/"+pair[0]+".json","models/item/"+pair[1]+".json");
        alias(result,"models/block/purpur_pillar_top.json","models/block/purpur_pillar.json");
        alias(result,"models/block/half_slab_purpur.json","models/block/purpur_slab.json");
        alias(result,"textures/entity/bed/silver.png","textures/entity/bed/light_gray.png");
        if(source.containsKey("textures/particle/particles.png"))result.put("textures/particle/particles.png",ParticleAtlasConverter.from1_13(source.get("textures/particle/particles.png")));
        if(result.containsKey("mob_sounds.json")) {
            JsonObject sounds=parse(result.get("mob_sounds.json")).getAsJsonObject();
            Map<String,JsonElement> renamed=new HashMap<>();
            for(Map.Entry<String,JsonElement> entry:sounds.entrySet()) {
                String old=com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.NamedSoundMappings1_12_2.getOldId(entry.getKey());
                if(old!=null)renamed.put(old,entry.getValue());
            }
            for(Map.Entry<String,JsonElement> entry:renamed.entrySet())sounds.add(entry.getKey(),entry.getValue());
            result.put("mob_sounds.json",bytes(sounds));
        }
        return result;
    }
    private static void alias(Map<String,byte[]> assets,String old,String modern){if(assets.containsKey(modern))assets.put(old,assets.get(modern));}
    private static void rewrite(JsonElement value,boolean state) {
        if(value.isJsonArray()){for(JsonElement child:value.getAsJsonArray())rewrite(child,state);}
        else if(value.isJsonObject())for(Map.Entry<String,JsonElement> entry:value.getAsJsonObject().entrySet()) {
            JsonElement child=entry.getValue();
            if(child.isJsonPrimitive()&&child.getAsJsonPrimitive().isString()) {
                String text=child.getAsString().replace("minecraft:","");
                if(state&&entry.getKey().equals("model")&&text.startsWith("block/"))text=text.substring(6);
                // Parents stay in models/block and models/item; only texture values move.
                if(!entry.getKey().equals("parent")&&!entry.getKey().equals("model")) {
                    if(text.startsWith("block/"))text="blocks/"+text.substring(6);
                    if(text.startsWith("item/"))text="items/"+text.substring(5);
                }
                entry.setValue(new JsonPrimitive(text));
            }else rewrite(child,state);
        }
    }
    private static JsonElement parse(byte[] data){return new JsonParser().parse(new String(data,StandardCharsets.UTF_8));}
    private static byte[] bytes(JsonElement json){return json.toString().getBytes(StandardCharsets.UTF_8);}
    private FlattenedResourceConverter(){}
}
